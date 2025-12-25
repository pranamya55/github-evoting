/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.sendvote;

import static ch.post.it.evoting.evotinglibraries.domain.validations.ControlComponentPayloadListValidation.validate;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.voting.sendvote.CombinedControlComponentPartialDecryptPayload;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentPartialDecryptPayload;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentlCCSharePayload;
import ch.post.it.evoting.domain.voting.sendvote.PartiallyDecryptedEncryptedPCC;
import ch.post.it.evoting.domain.voting.sendvote.VotingServerEncryptedVotePayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.votingserver.messaging.MessageHandler;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionCompletableFuture;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionService;
import ch.post.it.evoting.votingserver.messaging.Serializer;
import ch.post.it.evoting.votingserver.process.ElectionEventService;
import ch.post.it.evoting.votingserver.process.VerificationCardService;
import ch.post.it.evoting.votingserver.protocol.voting.sendvote.ExtractCRCOutput;
import ch.post.it.evoting.votingserver.protocol.voting.sendvote.ExtractCRCService;
import ch.post.it.evoting.votingserver.shelf.WorkflowShelfService;

/**
 * Generate the short Choice Return Codes based on the encrypted partial choice return codes - in interaction with the control components.
 */
@Service
public class ChoiceReturnCodesService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChoiceReturnCodesService.class);

	private final Serializer serializer;
	private final MessageHandler messageHandler;
	private final ExtractCRCService extractCRCService;
	private final ElectionEventService electionEventService;
	private final WorkflowShelfService workflowShelfService;
	private final VerificationCardService verificationCardService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final ResponseCompletionService responseCompletionService;

	ChoiceReturnCodesService(
			final Serializer serializer,
			final MessageHandler messageHandler,
			final ExtractCRCService extractCRCService,
			final ElectionEventService electionEventService,
			final WorkflowShelfService workflowShelfService,
			final VerificationCardService verificationCardService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ResponseCompletionService responseCompletionService) {
		this.serializer = serializer;
		this.messageHandler = messageHandler;
		this.extractCRCService = extractCRCService;
		this.electionEventService = electionEventService;
		this.workflowShelfService = workflowShelfService;
		this.verificationCardService = verificationCardService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.responseCompletionService = responseCompletionService;
	}

	@Transactional
	public ResponseCompletionCompletableFuture<ImmutableList<String>> retrieveShortChoiceReturnCodes(final ContextIds contextIds,
			final String credentialId,
			final EncryptedVerifiableVote encryptedVerifiableVote) {

		checkNotNull(contextIds);
		validateUUID(credentialId);
		checkNotNull(encryptedVerifiableVote);

		final String electionEventId = contextIds.electionEventId();

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		checkArgument(encryptionGroup.equals(encryptedVerifiableVote.encryptedVote().getGroup()),
				"The encryption group does not match the encrypted verifiable vote's group. [contextIds: %s]", contextIds);

		LOGGER.debug("Requesting Long Choice Return Codes to the control components... [contextIds: {}]", contextIds);

		// Ask the control components to partially decrypt the pCC.
		// Create and sign VotingServerEncryptedVotePayload with secret signing key.
		final VotingServerEncryptedVotePayload votingServerEncryptedVotePayload = new VotingServerEncryptedVotePayload(encryptionGroup,
				encryptedVerifiableVote);

		final CryptoPrimitivesSignature signature = getPayloadSignature(contextIds, votingServerEncryptedVotePayload);
		votingServerEncryptedVotePayload.setSignature(signature);

		final String correlationId = messageHandler.generateCorrelationId();
		final ContextShelf contextShelf = new ContextShelf(correlationId, contextIds, encryptionGroup);
		workflowShelfService.pushToShelf(correlationId, contextShelf);

		messageHandler.sendMessage(votingServerEncryptedVotePayload, correlationId);

		return responseCompletionService.registerForResponseCompletion(correlationId, new TypeReference<>() {});
	}

	@Transactional
	public void onResponsePartialDecrypt(final String correlationId,
			final ImmutableList<ControlComponentPartialDecryptPayload> controlComponentPartialDecryptPayloads) {

		checkNotNull(correlationId);
		validate(controlComponentPartialDecryptPayloads);

		final ContextShelf contextShelf = workflowShelfService.pullFromShelf(correlationId, ContextShelf.class);
		final ContextIds contextIds = contextShelf.contextIds();

		LOGGER.info("Partial decryptions received from the control-components. [contextIds: {}]", contextIds);

		verifyPartialDecryptPayloads(controlComponentPartialDecryptPayloads);

		// Combine response payloads.
		final CombinedControlComponentPartialDecryptPayload combinedPCCPayloads = new CombinedControlComponentPartialDecryptPayload(
				controlComponentPartialDecryptPayloads);

		final String newCorrelationId = messageHandler.generateCorrelationId();
		workflowShelfService.pushToShelf(newCorrelationId, contextShelf);

		// Ask the control components to compute the long Choice Return Code shares. The DecryptPCC will be done at same time by the CCs.
		messageHandler.sendMessage(combinedPCCPayloads, newCorrelationId);
	}

	@Transactional
	public void onResponseLongChoiceReturnCodesShare(final String correlationId,
			final ImmutableList<ControlComponentlCCSharePayload> controlComponentLCCSharePayloads) {

		checkNotNull(correlationId);
		validate(controlComponentLCCSharePayloads);

		final ContextShelf contextShelf = workflowShelfService.pullFromShelf(correlationId, ContextShelf.class);
		final String rootCorrelationId = contextShelf.rootCorrelationId();
		final ContextIds contextIds = contextShelf.contextIds();
		final GqGroup encryptionGroup = contextShelf.encryptionGroup();

		final String verificationCardId = contextIds.verificationCardId();

		LOGGER.info("Retrieved the long Choice Return Code shares payloads. [contextIds: {}]", contextIds);

		verifyLCCSharePayloads(encryptionGroup, contextIds, controlComponentLCCSharePayloads);

		final ExtractCRCOutput shortChoiceReturnCodesOutput = extractCRCService.extractCRC(contextIds, controlComponentLCCSharePayloads);
		LOGGER.info("Successfully extracted short Choice Return Codes. [contextIds: {}]", contextIds);

		// Save the short Choice Return Codes for later use in case of a re-login and transition to SENT.
		final ImmutableList<String> shortChoiceReturnCodes = shortChoiceReturnCodesOutput.shortChoiceReturnCodes();
		verificationCardService.saveSentState(verificationCardId, shortChoiceReturnCodes);
		LOGGER.info("Successfully saved state. [contextIds: {}, state: {}]", contextIds, VerificationCardState.SENT);

		responseCompletionService.notifyResponseCompleted(rootCorrelationId, shortChoiceReturnCodes);
	}

	public int extractNodeId(final ControlComponentPartialDecryptPayload controlComponentPartialDecryptPayload) {
		checkNotNull(controlComponentPartialDecryptPayload);

		return controlComponentPartialDecryptPayload.getPartiallyDecryptedEncryptedPCC().nodeId();
	}

	public ControlComponentPartialDecryptPayload deserializePartialDecryptPayload(final ImmutableByteArray bytes) {
		checkNotNull(bytes);
		return serializer.deserialize(bytes, ControlComponentPartialDecryptPayload.class);
	}

	public int extractNodeId(final ControlComponentlCCSharePayload controlComponentLCCSharePayload) {
		checkNotNull(controlComponentLCCSharePayload);
		return controlComponentLCCSharePayload.getLongChoiceReturnCodeShare().nodeId();
	}

	public ControlComponentlCCSharePayload deserializeLCCSharePayload(final ImmutableByteArray bytes) {
		checkNotNull(bytes);
		return serializer.deserialize(bytes, ControlComponentlCCSharePayload.class);
	}

	private CryptoPrimitivesSignature getPayloadSignature(final ContextIds contextIds, final VotingServerEncryptedVotePayload payload) {
		final Hashable additionalContextData = ChannelSecurityContextData.votingServerEncryptedVote(contextIds.electionEventId(),
				contextIds.verificationCardSetId(), contextIds.verificationCardId());
		try {
			final ImmutableByteArray signature = signatureKeystoreService.generateSignature(payload, additionalContextData);
			return new CryptoPrimitivesSignature(signature);
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Could not generate voting server encrypted vote payload signature. [contextIds: %s]", contextIds));
		}
	}

	private void verifyPartialDecryptPayloads(final ImmutableList<ControlComponentPartialDecryptPayload> controlComponentPartialDecryptPayloads) {
		final boolean isGqGroupEquals = controlComponentPartialDecryptPayloads.stream()
				.map(ControlComponentPartialDecryptPayload::getEncryptionGroup)
				.distinct()
				.count() == 1;

		if (!isGqGroupEquals) {
			throw new IllegalStateException("GqGroup is not identical for all the payloads.");
		}

		final boolean isContextIdsEquals = controlComponentPartialDecryptPayloads.stream()
				.map(ControlComponentPartialDecryptPayload::getPartiallyDecryptedEncryptedPCC)
				.map(PartiallyDecryptedEncryptedPCC::contextIds)
				.distinct()
				.count() == 1;

		if (!isContextIdsEquals) {
			throw new IllegalStateException("ContextIds are not identical for all the payloads.");
		}
	}

	/**
	 * Verifies the encryption group and signatures of the received {@link ControlComponentlCCSharePayload}s.
	 */
	private void verifyLCCSharePayloads(final GqGroup encryptionGroup, final ContextIds contextIds,
			final ImmutableList<ControlComponentlCCSharePayload> controlComponentLCCSharePayloads) {

		for (final ControlComponentlCCSharePayload payload : controlComponentLCCSharePayloads) {
			// Verify encryption group.
			checkArgument(payload.getEncryptionGroup().equals(encryptionGroup),
					"The group of the Control Component LCC Share payload must be equal to the encryption group.");

			// Verify signatures.
			final int nodeId = payload.getLongChoiceReturnCodeShare().nodeId();
			final Hashable additionalContextData = ChannelSecurityContextData.controlComponentlCCShare(nodeId, contextIds.electionEventId(),
					contextIds.verificationCardSetId(), contextIds.verificationCardId());

			final CryptoPrimitivesSignature signature = payload.getSignature();

			checkState(signature != null, "The signature of the long return codes share payload is null. [nodeId: %s, contextIds: %s]",
					nodeId, contextIds);

			final boolean isSignatureValid;
			try {
				isSignatureValid = signatureKeystoreService.verifySignature(Alias.getControlComponentByNodeId(nodeId), payload, additionalContextData,
						signature.signatureContents());
			} catch (final SignatureException e) {
				throw new IllegalStateException(
						String.format("Could not verify the signature of the long return codes share payload. [nodeId: %s, contextIds: %s]",
								nodeId, contextIds));
			}

			if (!isSignatureValid) {
				throw new InvalidPayloadSignatureException(ControlComponentlCCSharePayload.class,
						String.format("[nodeId: %s, contextIds: %s]", nodeId, contextIds));
			}
		}
	}

	private record ContextShelf(String rootCorrelationId, ContextIds contextIds, GqGroup encryptionGroup) {
		private ContextShelf {
			checkNotNull(rootCorrelationId);
			checkNotNull(contextIds);
			checkNotNull(encryptionGroup);
		}
	}

}
