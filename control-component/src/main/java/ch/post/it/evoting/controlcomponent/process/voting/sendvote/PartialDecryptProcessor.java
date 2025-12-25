/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.sendvote;

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

import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventState;
import ch.post.it.evoting.controlcomponent.process.ElectionEventStateService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.ContextIdExtractor;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentPartialDecryptPayload;
import ch.post.it.evoting.domain.voting.sendvote.PartiallyDecryptedEncryptedPCC;
import ch.post.it.evoting.domain.voting.sendvote.VotingServerEncryptedVotePayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

/**
 * Consumes the messages asking for the partial decryption of the encrypted Partial Choice Return Codes.
 */
@Service
public class PartialDecryptProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(PartialDecryptProcessor.class);

	private final ObjectMapper objectMapper;
	private final ElectionEventService electionEventService;
	private final PartialDecryptService partialDecryptService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final ElectionEventStateService electionEventStateService;
	private final PartiallyDecryptedPCCService partiallyDecryptedPCCService;

	@Value("${nodeID}")
	private int nodeId;

	public PartialDecryptProcessor(
			final ObjectMapper objectMapper,
			final ElectionEventService electionEventService,
			final PartialDecryptService partialDecryptService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ElectionEventStateService electionEventStateService,
			final PartiallyDecryptedPCCService partiallyDecryptedPCCService) {
		this.objectMapper = objectMapper;
		this.electionEventService = electionEventService;
		this.partialDecryptService = partialDecryptService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.electionEventStateService = electionEventStateService;
		this.partiallyDecryptedPCCService = partiallyDecryptedPCCService;
	}

	/**
	 * Validates the authenticity of the given {@link VotingServerEncryptedVotePayload} and verifies that the payload's encryption group is consistent
	 * with the control component's one.
	 * <p>
	 * The following checks are done in {@link PartialDecryptService#performPartialDecrypt(EncryptedVerifiableVote)}:
	 *     <ul>
	 *         <li>context ids consistency</li>
	 *         <li>start and end time validity</li>
	 *         <li>mixing status</li>
	 *     </ul>
	 * </p>
	 *
	 * @param votingServerEncryptedVotePayload the payload to be verified.
	 */
	public boolean verifyPayload(final VotingServerEncryptedVotePayload votingServerEncryptedVotePayload) {
		final ContextIds contextIds = votingServerEncryptedVotePayload.getEncryptedVerifiableVote().contextIds();
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		final CryptoPrimitivesSignature signature = votingServerEncryptedVotePayload.getSignature();

		checkState(signature != null, "The signature of the voting server encrypted vote payload is null. [contextIds: %s]", contextIds);

		final Hashable additionalContextData = ChannelSecurityContextData.votingServerEncryptedVote(electionEventId, verificationCardSetId,
				verificationCardId);

		// Verify signature.
		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.VOTING_SERVER, votingServerEncryptedVotePayload,
					additionalContextData, signature.signatureContents());

		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Could not verify the signature of the voting server encrypted vote payload. [contextIds: %s]", contextIds));
		}

		// Verify consistency.
		final GqGroup ccGqGroup = electionEventService.getEncryptionGroup(electionEventId);

		if (!votingServerEncryptedVotePayload.getEncryptionGroup().equals(ccGqGroup)) {
			throw new IllegalArgumentException(
					String.format("The payload's group is different from the control-component's group. [contextIds: %s]", contextIds));
		}

		return isSignatureValid;
	}

	@Transactional
	public ControlComponentPartialDecryptPayload onRequest(final VotingServerEncryptedVotePayload votingServerEncryptedVotePayload) {
		checkNotNull(votingServerEncryptedVotePayload);

		final EncryptedVerifiableVote encryptedVerifiableVote = votingServerEncryptedVotePayload.getEncryptedVerifiableVote();
		final ContextIds contextIds = encryptedVerifiableVote.contextIds();

		final String contextId = ContextIdExtractor.extract(votingServerEncryptedVotePayload);

		LOGGER.info("Received partial decrypt request. [contextId: {}]", contextId);

		// Validate election event state.
		final ElectionEventState expectedState = ElectionEventState.CONFIGURED;
		final String electionEventId = contextIds.electionEventId();
		final ElectionEventState electionEventState = electionEventStateService.getElectionEventState(electionEventId);
		checkState(expectedState.equals(electionEventState),
				"The election event is not in the expected state. [electionEventId: %s, nodeId: %s, expected: %s, actual: %s]", electionEventId,
				nodeId, expectedState, electionEventState);

		// Perform partial decryption.
		final PartiallyDecryptedEncryptedPCC partiallyDecryptedEncryptedPCC = partialDecryptService.performPartialDecrypt(encryptedVerifiableVote);
		partiallyDecryptedPCCService.save(partiallyDecryptedEncryptedPCC);

		// Create and sign response payload.
		final GqGroup encryptionGroup = encryptedVerifiableVote.encryptedVote().getGroup();
		final ControlComponentPartialDecryptPayload controlComponentPartialDecryptPayload =
				new ControlComponentPartialDecryptPayload(encryptionGroup, partiallyDecryptedEncryptedPCC);

		controlComponentPartialDecryptPayload.setSignature(generatePayloadSignature(controlComponentPartialDecryptPayload));

		LOGGER.info("Successfully signed control component partial decrypt payload. [contextIds: {}, nodeId: {}]", contextIds, nodeId);

		return controlComponentPartialDecryptPayload;
	}

	public ControlComponentPartialDecryptPayload onReplay(final VotingServerEncryptedVotePayload votingServerEncryptedVotePayload) {
		checkNotNull(votingServerEncryptedVotePayload);

		final EncryptedVerifiableVote encryptedVerifiableVote = votingServerEncryptedVotePayload.getEncryptedVerifiableVote();
		final GqGroup encryptionGroup = encryptedVerifiableVote.encryptedVote().getGroup();

		final String verificationCardId = encryptedVerifiableVote.contextIds().verificationCardId();
		final PartiallyDecryptedEncryptedPCC partiallyDecryptedEncryptedPCC = partiallyDecryptedPCCService.get(verificationCardId);

		final ControlComponentPartialDecryptPayload controlComponentPartialDecryptPayload =
				new ControlComponentPartialDecryptPayload(encryptionGroup, partiallyDecryptedEncryptedPCC);

		controlComponentPartialDecryptPayload.setSignature(generatePayloadSignature(controlComponentPartialDecryptPayload));

		return controlComponentPartialDecryptPayload;
	}

	private CryptoPrimitivesSignature generatePayloadSignature(final ControlComponentPartialDecryptPayload payload) {
		final ContextIds contextIds = payload.getPartiallyDecryptedEncryptedPCC().contextIds();
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		final Hashable additionalContextData = ChannelSecurityContextData.controlComponentPartialDecrypt(nodeId, electionEventId,
				verificationCardSetId, verificationCardId);

		try {
			final ImmutableByteArray signature = signatureKeystoreService.generateSignature(payload, additionalContextData);

			return new CryptoPrimitivesSignature(signature);
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Could not generate control component partial decrypt payload signature. [contextIds: %s, nodeId: %s]", contextIds,
							nodeId));
		}
	}

	public VotingServerEncryptedVotePayload deserializeRequest(final ImmutableByteArray bytes) {
		checkNotNull(bytes);

		try {
			return objectMapper.readValue(bytes.elements(), VotingServerEncryptedVotePayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public ImmutableByteArray serializeResponse(final ControlComponentPartialDecryptPayload controlComponentPartialDecryptPayload) {
		checkNotNull(controlComponentPartialDecryptPayload);

		try {
			return new ImmutableByteArray(objectMapper.writeValueAsBytes(controlComponentPartialDecryptPayload));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
	}
}
