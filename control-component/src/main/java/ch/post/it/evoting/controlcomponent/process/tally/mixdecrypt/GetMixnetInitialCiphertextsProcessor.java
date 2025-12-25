/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.process.BallotBoxEntity;
import ch.post.it.evoting.controlcomponent.process.BallotBoxService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.EncryptedVerifiableVoteService;
import ch.post.it.evoting.controlcomponent.process.SetupComponentPublicKeysService;
import ch.post.it.evoting.controlcomponent.protocol.tally.mixonline.GetMixnetInitialCiphertextsService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.tally.GetMixnetInitialCiphertextsRequestPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentVotesHashPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsOutput;

@Service
public class GetMixnetInitialCiphertextsProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(GetMixnetInitialCiphertextsProcessor.class);

	private final ObjectMapper objectMapper;
	private final BallotBoxService ballotBoxService;
	private final ElectionEventService electionEventService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final EncryptedVerifiableVoteService encryptedVerifiableVoteService;
	private final SetupComponentPublicKeysService setupComponentPublicKeysService;
	private final MixnetInitialCiphertextsService mixnetInitialCiphertextsService;
	private final GetMixnetInitialCiphertextsService getMixnetInitialCiphertextsService;

	@Value("${nodeID}")
	private int nodeId;

	GetMixnetInitialCiphertextsProcessor(
			final ObjectMapper objectMapper,
			final BallotBoxService ballotBoxService,
			final ElectionEventService electionEventService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final EncryptedVerifiableVoteService encryptedVerifiableVoteService,
			final SetupComponentPublicKeysService setupComponentPublicKeysService,
			final MixnetInitialCiphertextsService mixnetInitialCiphertextsService,
			final GetMixnetInitialCiphertextsService getMixnetInitialCiphertextsService) {
		this.objectMapper = objectMapper;
		this.ballotBoxService = ballotBoxService;
		this.electionEventService = electionEventService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.encryptedVerifiableVoteService = encryptedVerifiableVoteService;
		this.setupComponentPublicKeysService = setupComponentPublicKeysService;
		this.mixnetInitialCiphertextsService = mixnetInitialCiphertextsService;
		this.getMixnetInitialCiphertextsService = getMixnetInitialCiphertextsService;
	}

	@Transactional
	public ControlComponentVotesHashPayload onRequest(final GetMixnetInitialCiphertextsRequestPayload getMixnetInitialCiphertextsRequestPayload) {
		checkNotNull(getMixnetInitialCiphertextsRequestPayload);

		final String electionEventId = getMixnetInitialCiphertextsRequestPayload.electionEventId();
		final String ballotBoxId = getMixnetInitialCiphertextsRequestPayload.ballotBoxId();

		checkArgument(ballotBoxService.existsForElectionEventId(ballotBoxId, electionEventId),
				"The given election event id and ballot box id are unrelated. [electionEventId: %s, ballotBoxId: %s]", electionEventId, ballotBoxId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);
		final BallotBoxEntity ballotBoxEntity = ballotBoxService.getBallotBoxByBallotBoxId(ballotBoxId);
		final String verificationCardSetId = ballotBoxEntity.getVerificationCardSetEntity().getVerificationCardSetId();
		final ImmutableList<EncryptedVerifiableVote> confirmedVotes = encryptedVerifiableVoteService.getConfirmedVotes(verificationCardSetId);
		final ElGamalMultiRecipientPublicKey electionPublicKey = setupComponentPublicKeysService.getElectionPublicKey(electionEventId);

		final GetMixnetInitialCiphertextsOutput getMixnetInitialCiphertextsOutput = getMixnetInitialCiphertextsService.getMixnetInitialCiphertexts(
				encryptionGroup, electionEventId, ballotBoxEntity, electionPublicKey, confirmedVotes);

		LOGGER.info("Mix net initial ciphertexts retrieved. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		mixnetInitialCiphertextsService.save(electionEventId, ballotBoxId, getMixnetInitialCiphertextsOutput);

		LOGGER.info("Mix net initial ciphertexts saved. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		final String encryptedConfirmedVotesHash = getMixnetInitialCiphertextsOutput.encryptedConfirmedVotesHash();
		final ControlComponentVotesHashPayload controlComponentVotesHashPayload = new ControlComponentVotesHashPayload(
				electionEventId, ballotBoxId, nodeId, encryptedConfirmedVotesHash);

		controlComponentVotesHashPayload.setSignature(generatePayloadSignature(controlComponentVotesHashPayload));

		return controlComponentVotesHashPayload;
	}

	public ControlComponentVotesHashPayload onReplay(final GetMixnetInitialCiphertextsRequestPayload getMixnetInitialCiphertextsRequestPayload) {
		checkNotNull(getMixnetInitialCiphertextsRequestPayload);

		final String electionEventId = getMixnetInitialCiphertextsRequestPayload.electionEventId();
		final String ballotBoxId = getMixnetInitialCiphertextsRequestPayload.ballotBoxId();

		final String encryptedConfirmedVotesHash = mixnetInitialCiphertextsService.getEncryptedConfirmedVotesHash(electionEventId, ballotBoxId);

		final ControlComponentVotesHashPayload controlComponentVotesHashPayload = new ControlComponentVotesHashPayload(
				electionEventId, ballotBoxId, nodeId, encryptedConfirmedVotesHash);

		controlComponentVotesHashPayload.setSignature(generatePayloadSignature(controlComponentVotesHashPayload));

		return controlComponentVotesHashPayload;
	}

	public GetMixnetInitialCiphertextsRequestPayload deserializeRequest(final ImmutableByteArray messageBytes) {
		checkNotNull(messageBytes);
		try {
			return objectMapper.readValue(messageBytes.elements(), GetMixnetInitialCiphertextsRequestPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException("Unable to deserialize GetMixnetInitialCiphertexts Request Payload", e);
		}
	}

	public ImmutableByteArray serializeResponse(final ControlComponentVotesHashPayload controlComponentVotesHashPayload) {
		checkNotNull(controlComponentVotesHashPayload);

		try {
			return new ImmutableByteArray(objectMapper.writeValueAsBytes(controlComponentVotesHashPayload));
		} catch (final IOException e) {
			throw new UncheckedIOException("Unable to serialize Control Component Votes Hash Payload", e);
		}

	}

	public boolean verifyPayloadSignature(final GetMixnetInitialCiphertextsRequestPayload getMixnetInitialCiphertextsRequestPayload) {
		checkNotNull(getMixnetInitialCiphertextsRequestPayload);

		// The GetMixnetInitialCiphertextsRequestPayload is not signed.
		return true;
	}

	private CryptoPrimitivesSignature generatePayloadSignature(final ControlComponentVotesHashPayload controlComponentVotesHashPayload) {

		final String electionEventId = controlComponentVotesHashPayload.getElectionEventId();
		final String ballotBoxId = controlComponentVotesHashPayload.getBallotBoxId();

		final Hashable additionalContextData = ChannelSecurityContextData.controlComponentVotesHash(nodeId, electionEventId, ballotBoxId);
		try {
			final ImmutableByteArray signature = signatureKeystoreService.generateSignature(controlComponentVotesHashPayload, additionalContextData);
			return new CryptoPrimitivesSignature(signature);
		} catch (final SignatureException e) {
			throw new IllegalStateException(String.format("Failed to generate Control Component Votes Hash Payload signature [%s, %s]",
					controlComponentVotesHashPayload.getClass().getSimpleName(), additionalContextData), e);
		}
	}
}
