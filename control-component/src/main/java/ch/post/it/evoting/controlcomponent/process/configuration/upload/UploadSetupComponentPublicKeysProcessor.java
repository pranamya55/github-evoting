/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.configuration.upload;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.process.BallotBoxEntity;
import ch.post.it.evoting.controlcomponent.process.BallotBoxService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventState;
import ch.post.it.evoting.controlcomponent.process.ElectionEventStateService;
import ch.post.it.evoting.controlcomponent.process.ExtractedElectionEventHashService;
import ch.post.it.evoting.controlcomponent.process.SetupComponentPublicKeysService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.configuration.SetupComponentPublicKeysResponsePayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * Consumes the messages for saving the Setup Component public keys.
 */
@Service
public class UploadSetupComponentPublicKeysProcessor {

	public static final Logger LOGGER = LoggerFactory.getLogger(UploadSetupComponentPublicKeysProcessor.class);

	private final BallotBoxService ballotBoxService;
	private final VerificationCardService verificationCardService;
	private final VerificationCardSetService verificationCardSetService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final ElectionEventStateService electionEventStateService;
	private final ExtractedElectionEventHashService extractedElectionEventHashService;
	private final SetupComponentPublicKeysService setupComponentPublicKeysService;
	private final VerifySetupComponentPublicKeysService verifySetupComponentPublicKeysService;
	private final ObjectMapper objectMapper;

	@Value("${nodeID}")
	private int nodeId;

	public UploadSetupComponentPublicKeysProcessor(
			final BallotBoxService ballotBoxService,
			final VerificationCardService verificationCardService,
			final VerificationCardSetService verificationCardSetService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ElectionEventStateService electionEventStateService,
			final ExtractedElectionEventHashService extractedElectionEventHashService,
			final SetupComponentPublicKeysService setupComponentPublicKeysService,
			final VerifySetupComponentPublicKeysService verifySetupComponentPublicKeysService,
			final ObjectMapper objectMapper) {
		this.ballotBoxService = ballotBoxService;
		this.verificationCardService = verificationCardService;
		this.verificationCardSetService = verificationCardSetService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.electionEventStateService = electionEventStateService;
		this.extractedElectionEventHashService = extractedElectionEventHashService;
		this.setupComponentPublicKeysService = setupComponentPublicKeysService;
		this.verifySetupComponentPublicKeysService = verifySetupComponentPublicKeysService;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public SetupComponentPublicKeysResponsePayload onRequest(final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload) {
		checkNotNull(setupComponentPublicKeysPayload);

		verifyPayloadSignature(setupComponentPublicKeysPayload);

		// Verify number of saved verification cards.
		final String electionEventId = setupComponentPublicKeysPayload.getElectionEventId();
		Flux.fromIterable(verificationCardSetService.findAllByElectionEventId(electionEventId))
				.publishOn(Schedulers.boundedElastic())
				.doOnNext(verificationCardSetEntity -> {
					final String verificationCardSetId = verificationCardSetEntity.getVerificationCardSetId();
					final BallotBoxEntity ballotBoxEntity = ballotBoxService.getBallotBoxByVerificationCardSetId(verificationCardSetId);
					final int numberOfEligibleVoters = ballotBoxEntity.getNumberOfEligibleVoters();
					final int savedNumberOfVerificationCards = verificationCardService.countNumberOfVerificationCards(electionEventId,
							verificationCardSetId);

					checkArgument(numberOfEligibleVoters == savedNumberOfVerificationCards,
							"The number of eligible voters should be equal to the number of verification cards saved for the given election event and verification card set id. "
									+ "[electionEventId: %s, nodeId: %s, verificationCardSetId: %s, numberOfEligibleVoters: %s, savedNumberOfVerificationCards: %s]",
							electionEventId, nodeId, verificationCardSetId, numberOfEligibleVoters, savedNumberOfVerificationCards);
				})
				.then()
				.block();

		final SetupComponentPublicKeys setupComponentPublicKeys = setupComponentPublicKeysPayload.getSetupComponentPublicKeys();

		return createSetupComponentPublicKeysResponse(electionEventId, setupComponentPublicKeys);
	}

	public SetupComponentPublicKeysResponsePayload onReplay(final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload) {
		checkNotNull(setupComponentPublicKeysPayload);

		final String electionEventId = setupComponentPublicKeysPayload.getElectionEventId();

		return new SetupComponentPublicKeysResponsePayload(nodeId, electionEventId);
	}

	public boolean verifyPayloadSignature(final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload) {
		final String electionEventId = setupComponentPublicKeysPayload.getElectionEventId();

		final CryptoPrimitivesSignature signature = setupComponentPublicKeysPayload.getSignature();

		checkState(signature != null, "The signature of the setup component public keys payload is null. [electionEventId: %s, nodeId: %s]",
				electionEventId, nodeId);

		final Hashable additionalContextData = ChannelSecurityContextData.setupComponentPublicKeys(electionEventId);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.SDM_CONFIG, setupComponentPublicKeysPayload,
					additionalContextData, signature.signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Could not verify the signature of the setup component public keys payload. [electionEventId: %s, nodeId: %s]",
							electionEventId, nodeId));
		}

		return isSignatureValid;
	}

	private SetupComponentPublicKeysResponsePayload createSetupComponentPublicKeysResponse(final String electionEventId,
			final SetupComponentPublicKeys setupComponentPublicKeys) {
		// Validate election event state. Implicitly checks election event existence.
		final ElectionEventState expectedState = ElectionEventState.INITIAL;
		final ElectionEventState electionEventState = electionEventStateService.getElectionEventState(electionEventId);
		checkState(expectedState.equals(electionEventState),
				"The election event is not in the expected state. [electionEventId: %s, nodeId: %s, expected: %s, actual: %s]", electionEventId,
				nodeId, expectedState, electionEventState);

		// Validate and save the Setup Component public keys.
		verifySetupComponentPublicKeysService.verifySetupComponentPublicKeys(electionEventId, setupComponentPublicKeys);
		setupComponentPublicKeysService.save(electionEventId, setupComponentPublicKeys);
		LOGGER.info("Saved setup component public keys. [electionEventId: {}, nodeId: {}]", electionEventId, nodeId);

		// For performance reasons, the control-component precomputes the result of the algorithms ExtractElectionEvent and
		// GetHashExtractedElectionEvent to avoid recalculating it for each vote.
		extractedElectionEventHashService.computeAndSave(electionEventId);
		LOGGER.info("Computed and saved the hash of the extracted election event. [electionEventId: {}, nodeId: {}]", electionEventId, nodeId);

		// Update election state to CONFIGURED.
		final ElectionEventState configuredState = ElectionEventState.CONFIGURED;
		electionEventStateService.updateElectionEventState(electionEventId, configuredState);
		LOGGER.info("Updated election event state. [electionEventId: {}, nodeId: {}, state: {}]", electionEventId, nodeId, configuredState);

		return new SetupComponentPublicKeysResponsePayload(nodeId, electionEventId);
	}

	public SetupComponentPublicKeysPayload deserializeRequest(final ImmutableByteArray bytes) {
		checkNotNull(bytes);
		try {
			return objectMapper.readValue(bytes.elements(), SetupComponentPublicKeysPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public ImmutableByteArray serializeResponse(final SetupComponentPublicKeysResponsePayload setupComponentPublicKeysResponsePayload) {
		checkNotNull(setupComponentPublicKeysResponsePayload);
		try {
			return new ImmutableByteArray(objectMapper.writeValueAsBytes(setupComponentPublicKeysResponsePayload));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
	}
}
