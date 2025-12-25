/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.confirmvote;

import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.validations.ControlComponentPayloadListValidation.validate;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SignatureException;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.HashedLVCCSharesService;
import ch.post.it.evoting.controlcomponent.process.IdentifierValidationService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.voting.confirmvote.ConfirmationKey;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCRequestPayload;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponentlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.LongVoteCastReturnCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

@Service
public class LongVoteCastReturnCodesShareVerifyProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(LongVoteCastReturnCodesShareVerifyProcessor.class);

	private final ObjectMapper objectMapper;
	private final ElectionEventService electionEventService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final IdentifierValidationService identifierValidationService;
	private final VerificationCardStateService verificationCardStateService;
	private final LVCCShareService lvccShareService;
	private final LongVoteCastReturnCodesShareVerifyService longVoteCastReturnCodesShareVerifyService;
	private final HashedLVCCSharesService hashedLVCCSharesService;

	@Value("${nodeID}")
	private int nodeId;

	LongVoteCastReturnCodesShareVerifyProcessor(
			final ObjectMapper objectMapper,
			final ElectionEventService electionEventService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final IdentifierValidationService identifierValidationService,
			final VerificationCardStateService verificationCardStateService,
			final LVCCShareService lvccShareService,
			final LongVoteCastReturnCodesShareVerifyService longVoteCastReturnCodesShareVerifyService,
			final HashedLVCCSharesService hashedLVCCSharesService) {
		this.objectMapper = objectMapper;
		this.electionEventService = electionEventService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.identifierValidationService = identifierValidationService;
		this.verificationCardStateService = verificationCardStateService;
		this.lvccShareService = lvccShareService;
		this.longVoteCastReturnCodesShareVerifyService = longVoteCastReturnCodesShareVerifyService;
		this.hashedLVCCSharesService = hashedLVCCSharesService;
	}

	@Transactional
	public ControlComponentlVCCSharePayload onRequest(final ControlComponenthlVCCRequestPayload controlComponenthlVCCRequestPayload) {
		checkNotNull(controlComponenthlVCCRequestPayload);

		final ImmutableList<ControlComponenthlVCCSharePayload> controlComponenthlVCCPayloads = controlComponenthlVCCRequestPayload.controlComponenthlVCCPayloads();

		verifyPayloadsConsistency(controlComponenthlVCCPayloads);

		final ContextIds contextIds = controlComponenthlVCCPayloads.get(0).getConfirmationKey().contextIds();

		identifierValidationService.validateContextIds(contextIds);
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardId = contextIds.verificationCardId();
		final String verificationCardSetId = contextIds.verificationCardSetId();

		lvccShareService.validateConfirmationIsAllowed(electionEventId, verificationCardId, LocalDateTime::now);

		final int confirmationAttemptId = verificationCardStateService.getNextConfirmationAttemptId(verificationCardId);
		final String contextId = String.join("-", electionEventId, verificationCardSetId, verificationCardId, String.valueOf(confirmationAttemptId));
		LOGGER.info("Received Long Vote Cast Return Codes Share verify request. [contextId: {}]", contextId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		final boolean isVerified = longVoteCastReturnCodesShareVerifyService.performVerifyLVCCHashService(encryptionGroup,
				controlComponenthlVCCPayloads);

		final ConfirmationKey confirmationKey = controlComponenthlVCCPayloads.get(0).getConfirmationKey();
		final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload;
		if (isVerified) {
			final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare = lvccShareService.getLongVoteCastReturnCodeShare(
					confirmationKey, nodeId);
			controlComponentlVCCSharePayload = new ControlComponentlVCCSharePayload(electionEventId, verificationCardSetId, verificationCardId,
					nodeId, encryptionGroup, longVoteCastReturnCodeShare, confirmationKey, true);
		} else {
			controlComponentlVCCSharePayload = new ControlComponentlVCCSharePayload(electionEventId, verificationCardSetId, verificationCardId,
					nodeId, encryptionGroup, confirmationKey, false);
		}

		controlComponentlVCCSharePayload.setSignature(generatePayloadSignature(controlComponentlVCCSharePayload));
		LOGGER.info("Successfully signed Control Component lVCC Share payload. [contextIds: {}]", contextIds);

		return controlComponentlVCCSharePayload;
	}

	public ControlComponentlVCCSharePayload onReplay(final ControlComponenthlVCCRequestPayload controlComponenthlVCCRequestPayload) {
		checkNotNull(controlComponenthlVCCRequestPayload);

		final ConfirmationKey confirmationKey = controlComponenthlVCCRequestPayload.controlComponenthlVCCPayloads()
				.get(0)
				.getConfirmationKey();
		final ContextIds contextIds = confirmationKey.contextIds();
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardId = contextIds.verificationCardId();
		final String verificationCardSetId = contextIds.verificationCardSetId();

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload;
		if (hashedLVCCSharesService.isLVCCHashVerified(verificationCardId)) {
			final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare = lvccShareService.getLongVoteCastReturnCodeShare(
					confirmationKey, nodeId);
			controlComponentlVCCSharePayload = new ControlComponentlVCCSharePayload(electionEventId, verificationCardSetId, verificationCardId,
					nodeId, encryptionGroup, longVoteCastReturnCodeShare, confirmationKey, true);
		} else {
			controlComponentlVCCSharePayload = new ControlComponentlVCCSharePayload(electionEventId, verificationCardSetId, verificationCardId,
					nodeId, encryptionGroup, confirmationKey, false);
		}

		controlComponentlVCCSharePayload.setSignature(generatePayloadSignature(controlComponentlVCCSharePayload));

		return controlComponentlVCCSharePayload;
	}

	public boolean verifyPayloadSignature(final ControlComponenthlVCCRequestPayload controlComponenthlVCCRequestPayload) {
		checkNotNull(controlComponenthlVCCRequestPayload);

		for (final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload : controlComponenthlVCCRequestPayload.controlComponenthlVCCPayloads()) {

			final CryptoPrimitivesSignature signature = controlComponenthlVCCSharePayload.getSignature();
			final ContextIds contextIds = controlComponenthlVCCRequestPayload.controlComponenthlVCCPayloads().get(0).getConfirmationKey()
					.contextIds();
			final int payloadNodeId = controlComponenthlVCCSharePayload.getNodeId();

			checkState(signature != null, "The signature of Control Component hlVCC Payload is null. [contextIds: %s]",
					contextIds);

			final Hashable additionalContextData = ChannelSecurityContextData.controlComponenthlVCCShare(payloadNodeId, contextIds.electionEventId(),
					contextIds.verificationCardSetId(), contextIds.verificationCardId());
			final boolean isSignatureValid;
			try {
				isSignatureValid = signatureKeystoreService.verifySignature(Alias.getControlComponentByNodeId(payloadNodeId),
						controlComponenthlVCCSharePayload, additionalContextData, signature.signatureContents());
			} catch (final SignatureException e) {
				throw new IllegalStateException(String.format(
						"Cannot verify the signature of Control Component hlVCC Payload. [contextIds: %s, payloadNodeId: %s]",
						contextIds, payloadNodeId), e);
			}

			if (!isSignatureValid) {
				return false;
			}
		}
		return true;
	}

	private void verifyPayloadsConsistency(final ImmutableList<ControlComponenthlVCCSharePayload> controlComponenthlVCCPayloads) {
		validate(controlComponenthlVCCPayloads);

		final ConfirmationKey confirmationKey = controlComponenthlVCCPayloads.get(0).getConfirmationKey();
		final ContextIds contextIds = confirmationKey.contextIds();

		checkArgument(allEqual(controlComponenthlVCCPayloads.stream(), ControlComponenthlVCCSharePayload::getConfirmationKey),
				"The Long Vote Cast Return Codes Share hash payloads do not contain all the same confirmation key. [contextIds: %s]", contextIds);

		checkArgument(allEqual(controlComponenthlVCCPayloads.stream(), payload -> payload.getConfirmationKey().contextIds()),
				"The Control Component hlVCC Payloads do not contain all the same context ids. [contextIds: %s]", contextIds);

		// Verify that the confirmation key exists in the internal view.
		checkArgument(lvccShareService.exists(contextIds.verificationCardId(), confirmationKey),
				"The received confirmation key must exist. [contextIds: %s]", contextIds);
	}

	private CryptoPrimitivesSignature generatePayloadSignature(final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload) {
		final String electionEventId = controlComponentlVCCSharePayload.getElectionEventId();
		final String verificationCardId = controlComponentlVCCSharePayload.getVerificationCardId();
		final String verificationCardSetId = controlComponentlVCCSharePayload.getVerificationCardSetId();
		final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);

		final Hashable additionalContextData = ChannelSecurityContextData.controlComponentlVCCShare(nodeId, electionEventId, verificationCardSetId,
				verificationCardId);
		try {
			final ImmutableByteArray signature = signatureKeystoreService.generateSignature(controlComponentlVCCSharePayload, additionalContextData);

			return new CryptoPrimitivesSignature(signature);
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Could not generate Control Component lVCC Share payload signature. [contextIds: %s]", contextIds));
		}

	}

	public ControlComponenthlVCCRequestPayload deserializeRequest(final ImmutableByteArray bytes) {
		checkNotNull(bytes);
		try {
			return objectMapper.readValue(bytes.elements(), ControlComponenthlVCCRequestPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public ImmutableByteArray serializeResponse(final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload) {
		checkNotNull(controlComponentlVCCSharePayload);
		try {
			return new ImmutableByteArray(objectMapper.writeValueAsBytes(controlComponentlVCCSharePayload));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
	}
}
