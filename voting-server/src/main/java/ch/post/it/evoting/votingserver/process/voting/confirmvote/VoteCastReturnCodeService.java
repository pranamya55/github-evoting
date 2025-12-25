/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.confirmvote;

import static ch.post.it.evoting.domain.Constants.MAX_CONFIRMATION_ATTEMPTS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.ControlComponentPayloadListValidation.validate;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.voting.confirmvote.ConfirmationKey;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCRequestPayload;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponentlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.VotingServerConfirmPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.votingserver.messaging.MessageHandler;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionCompletableFuture;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionService;
import ch.post.it.evoting.votingserver.messaging.Serializer;
import ch.post.it.evoting.votingserver.process.ElectionEventService;
import ch.post.it.evoting.votingserver.process.VerificationCardService;
import ch.post.it.evoting.votingserver.process.voting.ConfirmationKeyInvalidException;
import ch.post.it.evoting.votingserver.protocol.voting.confirmvote.ExtractVCCOutput;
import ch.post.it.evoting.votingserver.protocol.voting.confirmvote.ExtractVCCService;
import ch.post.it.evoting.votingserver.shelf.WorkflowShelfService;

/**
 * Generate the short Vote Cast Return Code based on the confirmation message - in interaction with the control components.
 */
@Service
public class VoteCastReturnCodeService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VoteCastReturnCodeService.class);

	private final Serializer serializer;
	private final MessageHandler messageHandler;
	private final ExtractVCCService extractVCCService;
	private final ElectionEventService electionEventService;
	private final WorkflowShelfService workflowShelfService;
	private final VerificationCardService verificationCardService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final ResponseCompletionService responseCompletionService;

	public VoteCastReturnCodeService(
			final Serializer serializer,
			final MessageHandler messageHandler,
			final ExtractVCCService extractVCCService,
			final ElectionEventService electionEventService,
			final WorkflowShelfService workflowShelfService,
			final VerificationCardService verificationCardService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ResponseCompletionService responseCompletionService) {
		this.serializer = serializer;
		this.messageHandler = messageHandler;
		this.extractVCCService = extractVCCService;
		this.electionEventService = electionEventService;
		this.workflowShelfService = workflowShelfService;
		this.verificationCardService = verificationCardService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.responseCompletionService = responseCompletionService;
	}

	/**
	 * Calculates in interaction with the control components the short Vote Cast Return Code based on the confirmation message received by the voting
	 * client.
	 *
	 * @param contextIds      the context ids.
	 * @param confirmationKey the confirmation key.
	 * @return The short Vote Cast Return Code.
	 * @throws NullPointerException if any parameter is null.
	 */
	@SuppressWarnings("java:S117")
	@Transactional
	public ResponseCompletionCompletableFuture<String> retrieveShortVoteCastCode(final ContextIds contextIds, final GqElement confirmationKey) {
		checkNotNull(contextIds);
		checkNotNull(confirmationKey);

		final String electionEventId = contextIds.electionEventId();
		final String verificationCardId = contextIds.verificationCardId();

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);
		checkArgument(encryptionGroup.equals(confirmationKey.getGroup()),
				"The encryption group does not match the confirmation key's group. [contextIds: %s]", contextIds);

		LOGGER.debug("Generating the short Vote Cast Return Code... [contextIds: {}]", contextIds);

		// Transition to CONFIRMING.
		verificationCardService.saveConfirmingState(verificationCardId);
		LOGGER.info("Successfully saved state. [contextIds: {}, state: {}]", contextIds, VerificationCardState.CONFIRMING);

		// Create VotingServerConfirmPayload to send.
		final int confirmationAttemptId = verificationCardService.getNextConfirmationAttemptId(verificationCardId);
		final VotingServerConfirmPayload votingServerConfirmPayload = createVotingServerConfirmPayload(contextIds, confirmationKey,
				confirmationAttemptId);

		final String correlationId = messageHandler.generateCorrelationId();

		final ShelfElement shelfElement = new ShelfElement(correlationId, contextIds, encryptionGroup);
		workflowShelfService.pushToShelf(correlationId, shelfElement);

		// Ask the control components to compute the long vote cast return code shares lCC_j_id.
		messageHandler.sendMessage(votingServerConfirmPayload, correlationId);

		return responseCompletionService.registerForResponseCompletion(correlationId, String.class);
	}

	@Transactional
	public void onResponseLongVoteCastReturnCodesShareHash(final String correlationId,
			final ImmutableList<ControlComponenthlVCCSharePayload> controlComponenthlVCCPayloads) {
		checkNotNull(correlationId);
		validate(controlComponenthlVCCPayloads);

		final ShelfElement shelfElement = workflowShelfService.pullFromShelf(correlationId, ShelfElement.class);
		final ContextIds contextIds = shelfElement.contextIds();

		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		LOGGER.info("Received Long Vote Cast Return Codes Share hashes [electionEventId : {}, verificationCardSetId : {}, verificationCardId : {}]",
				electionEventId, verificationCardSetId, verificationCardId);

		final String newCorrelationId = messageHandler.generateCorrelationId();
		workflowShelfService.pushToShelf(newCorrelationId, shelfElement);

		final ControlComponenthlVCCRequestPayload controlComponenthlVCCRequestPayload = new ControlComponenthlVCCRequestPayload(
				controlComponenthlVCCPayloads);

		messageHandler.sendMessage(controlComponenthlVCCRequestPayload, newCorrelationId);
	}

	@Transactional
	public void onResponseLongVoteCastReturnCodesShareVerify(final String correlationId,
			final ImmutableList<ControlComponentlVCCSharePayload> controlComponentlVCCSharePayloads) {

		checkNotNull(correlationId);
		validate(controlComponentlVCCSharePayloads);

		final ShelfElement shelfElement = workflowShelfService.pullFromShelf(correlationId, ShelfElement.class);
		final String rootCorrelationId = shelfElement.rootCorrelationId();
		final ContextIds contextIds = shelfElement.contextIds();
		final GqGroup encryptionGroup = shelfElement.encryptionGroup();

		final String verificationCardId = contextIds.verificationCardId();

		// Vote Cast Return Code computation response correctly received.
		LOGGER.info("Successfully retrieved the Long Vote Cast Return Code shares. [contextIds: {}]", contextIds);

		verifyPayload(encryptionGroup, contextIds, controlComponentlVCCSharePayloads);

		LOGGER.info("Successfully verified the Long Vote Cast Return Code shares. [contextIds: {}]", contextIds);

		if (!controlComponentlVCCSharePayloads.stream().allMatch(ControlComponentlVCCSharePayload::isVerified)) {
			final int incrementedConfirmationAttempts = verificationCardService.incrementConfirmationAttempts(verificationCardId);
			final int remainingAttempts = MAX_CONFIRMATION_ATTEMPTS - incrementedConfirmationAttempts;

			final String message = String.format("The ControlComponentlVCCSharePayload are not all verified. [contextIds: %s]", contextIds);
			final ConfirmationKeyInvalidException exception = new ConfirmationKeyInvalidException(message, remainingAttempts);
			LOGGER.info(message, exception);

			responseCompletionService.notifyException(rootCorrelationId, exception);
		} else {
			final ExtractVCCOutput voteCastReturnCodeOutput = extractVCCService.extractVCC(contextIds, controlComponentlVCCSharePayloads);
			LOGGER.info("Short Vote Cast Return Code successfully retrieved. [contextIds: {}]", contextIds);

			// Save the short Vote Cast Return Code for later use in case of a re-login and transition to CONFIRMED.
			final String shortVoteCastReturnCode = voteCastReturnCodeOutput.shortVoteCastReturnCode();
			verificationCardService.saveConfirmedState(verificationCardId, shortVoteCastReturnCode);
			LOGGER.info("Successfully saved state. [contextIds: {}, state: {}]", contextIds, VerificationCardState.CONFIRMED);

			responseCompletionService.notifyResponseCompleted(rootCorrelationId, shortVoteCastReturnCode);
		}
	}

	public ControlComponentlVCCSharePayload deserializeControlComponentLVCCSharePayload(final ImmutableByteArray bytes) {
		checkNotNull(bytes);
		return serializer.deserialize(bytes, ControlComponentlVCCSharePayload.class);
	}

	public ControlComponenthlVCCSharePayload deserializeControlComponenthlVCCPayload(final ImmutableByteArray bytes) {
		checkNotNull(bytes);
		return serializer.deserialize(bytes, ControlComponenthlVCCSharePayload.class);
	}

	public int extractNodeId(final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload) {
		checkNotNull(controlComponenthlVCCSharePayload);
		return controlComponenthlVCCSharePayload.getNodeId();
	}

	public int extractNodeId(final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload) {
		checkNotNull(controlComponentlVCCSharePayload);
		return controlComponentlVCCSharePayload.getNodeId();
	}

	private VotingServerConfirmPayload createVotingServerConfirmPayload(final ContextIds contextIds, final GqElement confirmationKeyElement,
			final int confirmationAttemptId) {

		final GqGroup encryptionGroup = confirmationKeyElement.getGroup();
		final ConfirmationKey confirmationKey = new ConfirmationKey(contextIds, confirmationKeyElement);

		// Create and sign payload.
		final VotingServerConfirmPayload votingServerConfirmPayload = new VotingServerConfirmPayload(encryptionGroup, confirmationKey,
				confirmationAttemptId);
		final CryptoPrimitivesSignature signature = getPayloadSignature(votingServerConfirmPayload, contextIds);
		votingServerConfirmPayload.setSignature(signature);
		LOGGER.info("Successfully signed the voting server confirm payload. [contextIds: {}]", contextIds);

		return votingServerConfirmPayload;
	}

	private CryptoPrimitivesSignature getPayloadSignature(final VotingServerConfirmPayload votingServerConfirmPayload, final ContextIds contextIds) {
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		final Hashable additionalContextData = ChannelSecurityContextData.votingServerConfirm(electionEventId, verificationCardSetId,
				verificationCardId);

		try {
			final ImmutableByteArray signature = signatureKeystoreService.generateSignature(votingServerConfirmPayload, additionalContextData);

			return new CryptoPrimitivesSignature(signature);

		} catch (final SignatureException se) {
			final String message = String.format("Failed to sign the voting server confirm payload. [contextIds: %s]", contextIds);
			throw new IllegalStateException(message, se);
		}
	}

	private void verifyPayload(final GqGroup encryptionGroup, final ContextIds contextIds,
			final ImmutableList<ControlComponentlVCCSharePayload> controlComponentlVCCSharePayloads) {

		controlComponentlVCCSharePayloads.forEach(payload -> {
			// Verify encryption group.
			checkArgument(payload.getEncryptionGroup().equals(encryptionGroup),
					"The group of the Control Component lVCC Share payload must be equal to the encryption group.");

			// Verify ids.
			checkArgument(payload.getElectionEventId().equals(contextIds.electionEventId()),
					"The Control Component lVCC Share payload does not contain the expected election event id. [expected: %s, actual: %s]",
					contextIds.electionEventId(), payload.getElectionEventId());
			checkArgument(payload.getVerificationCardSetId().equals(contextIds.verificationCardSetId()),
					"The Control Component lVCC Share payload does not contain the expected verification card set id. [expected: %s, actual: %s]",
					contextIds.verificationCardSetId(), payload.getVerificationCardSetId());
			checkArgument(payload.getVerificationCardId().equals(contextIds.verificationCardId()),
					"The Control Component lVCC Share payload does not contain the expected verification card id. [expected: %s, actual: %s]",
					contextIds.verificationCardId(), payload.getVerificationCardId());

			// Verify signature.
			final int nodeId = payload.getNodeId();
			final CryptoPrimitivesSignature signature = payload.getSignature();

			checkState(signature != null, "The signature of the Control Component lVCC Share payload is null. [nodeId: %s, contextIds: %s]",
					nodeId, contextIds);

			final Hashable additionalContextData = ChannelSecurityContextData.controlComponentlVCCShare(nodeId, contextIds.electionEventId(),
					contextIds.verificationCardSetId(), contextIds.verificationCardId());

			final boolean isSignatureValid;
			try {
				isSignatureValid = signatureKeystoreService.verifySignature(Alias.getControlComponentByNodeId(nodeId), payload, additionalContextData,
						signature.signatureContents());
			} catch (final SignatureException e) {
				throw new IllegalStateException(
						String.format("Could not verify the signature of the Control Component lVCC Share payload. [nodeId: %s, contextIds: %s]",
								nodeId, contextIds));
			}
			if (!isSignatureValid) {
				throw new InvalidPayloadSignatureException(ControlComponentlVCCSharePayload.class,
						String.format("[nodeId: %s, contextIds: %s]", nodeId, contextIds));
			}
		});
	}

	@VisibleForTesting
	record ShelfElement(String rootCorrelationId, ContextIds contextIds, GqGroup encryptionGroup) {
		ShelfElement {
			checkNotNull(rootCorrelationId);
			checkNotNull(contextIds);
			checkNotNull(encryptionGroup);
		}
	}

}
