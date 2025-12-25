/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.confirmvote;

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
import ch.post.it.evoting.controlcomponent.process.IdentifierValidationService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote.CreateLVCCShareOutput;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.voting.confirmvote.ConfirmationKey;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.VotingServerConfirmPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

@Service
public class LongVoteCastReturnCodesShareHashProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(LongVoteCastReturnCodesShareHashProcessor.class);

	private final ObjectMapper objectMapper;
	private final ElectionEventService electionEventService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final IdentifierValidationService identifierValidationService;
	private final VerificationCardStateService verificationCardStateService;
	private final LVCCShareService lvccShareService;
	private final LongVoteCastReturnCodesShareHashService longVoteCastReturnCodesShareHashService;

	@Value("${nodeID}")
	private int nodeId;

	LongVoteCastReturnCodesShareHashProcessor(
			final ObjectMapper objectMapper,
			final ElectionEventService electionEventService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final IdentifierValidationService identifierValidationService,
			final VerificationCardStateService verificationCardStateService,
			final LVCCShareService lvccShareService,
			final LongVoteCastReturnCodesShareHashService longVoteCastReturnCodesShareHashService) {
		this.objectMapper = objectMapper;
		this.electionEventService = electionEventService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.identifierValidationService = identifierValidationService;
		this.verificationCardStateService = verificationCardStateService;
		this.lvccShareService = lvccShareService;
		this.longVoteCastReturnCodesShareHashService = longVoteCastReturnCodesShareHashService;
	}

	@Transactional
	public ControlComponenthlVCCSharePayload onRequest(final VotingServerConfirmPayload votingServerConfirmPayload) {
		checkNotNull(votingServerConfirmPayload);

		verifyPayloadSignature(votingServerConfirmPayload);

		final ContextIds contextIds = votingServerConfirmPayload.getConfirmationKey().contextIds();

		identifierValidationService.validateContextIds(contextIds);
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		lvccShareService.validateConfirmationIsAllowed(electionEventId, verificationCardId, LocalDateTime::now);

		// The next confirmation attempt id is initialized to 0 and incremented for each confirmation attempt.
		final int confirmationAttemptId = verificationCardStateService.getNextConfirmationAttemptId(verificationCardId);

		checkState(confirmationAttemptId == votingServerConfirmPayload.getConfirmationAttemptId(),
				"The confirmation attempt ids do not match between payload file and database. [payload: {}, database: {}]",
				votingServerConfirmPayload.getConfirmationAttemptId(), confirmationAttemptId);

		final String contextId = String.join("-", electionEventId, verificationCardSetId, verificationCardId,
				String.valueOf(confirmationAttemptId));
		LOGGER.info("Received create LVCC share hash request. [contextId: {}]", contextId);

		final ConfirmationKey confirmationKey = votingServerConfirmPayload.getConfirmationKey();
		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		final CreateLVCCShareOutput createLVCCShareOutput = longVoteCastReturnCodesShareHashService.performCreateLVCCShare(confirmationKey);

		final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload = new ControlComponenthlVCCSharePayload(encryptionGroup, nodeId,
				createLVCCShareOutput.hashedLongVoteCastReturnCodeShare(), confirmationKey, confirmationAttemptId);

		controlComponenthlVCCSharePayload.setSignature(generatePayloadSignature(controlComponenthlVCCSharePayload));

		LOGGER.info("Successfully signed Control Component hlVCC payload. [contextIds: {}]", contextIds);

		return controlComponenthlVCCSharePayload;
	}

	public ControlComponenthlVCCSharePayload onReplay(final VotingServerConfirmPayload votingServerConfirmPayload) {
		checkNotNull(votingServerConfirmPayload);

		final ConfirmationKey confirmationKey = votingServerConfirmPayload.getConfirmationKey();
		final int confirmationAttemptId = votingServerConfirmPayload.getConfirmationAttemptId();
		final ContextIds contextIds = confirmationKey.contextIds();
		final String electionEventId = contextIds.electionEventId();
		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		final String hashLongVoteCastReturnCodeShare = lvccShareService.getHashedLongVoteCastReturnCodeShare(confirmationKey,
				nodeId);

		final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload = new ControlComponenthlVCCSharePayload(encryptionGroup, nodeId,
				hashLongVoteCastReturnCodeShare, confirmationKey, confirmationAttemptId);

		controlComponenthlVCCSharePayload.setSignature(generatePayloadSignature(controlComponenthlVCCSharePayload));

		return controlComponenthlVCCSharePayload;
	}

	public boolean verifyPayloadSignature(final VotingServerConfirmPayload votingServerConfirmPayload) {
		final CryptoPrimitivesSignature signature = votingServerConfirmPayload.getSignature();
		final ContextIds contextIds = votingServerConfirmPayload.getConfirmationKey().contextIds();

		checkState(signature != null, "The signature of voting server confirm payload is null. [contextIds: %s]",
				contextIds);

		final Hashable additionalContextData = ChannelSecurityContextData.votingServerConfirm(contextIds.electionEventId(),
				contextIds.verificationCardSetId(), contextIds.verificationCardId());
		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.VOTING_SERVER, votingServerConfirmPayload,
					additionalContextData, signature.signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(String.format("Unable to verify signature [contextIds: %s]", contextIds), e);
		}

		return isSignatureValid;
	}

	private CryptoPrimitivesSignature generatePayloadSignature(final ControlComponenthlVCCSharePayload payload) {
		final ContextIds contextIds = payload.getConfirmationKey().contextIds();
		final Hashable additionalContextData = ChannelSecurityContextData.controlComponenthlVCCShare(nodeId, contextIds.electionEventId(),
				contextIds.verificationCardSetId(), contextIds.verificationCardId());

		final ImmutableByteArray signature;
		try {
			signature = signatureKeystoreService.generateSignature(payload, additionalContextData);
		} catch (final SignatureException e) {
			throw new IllegalStateException(String.format("Failed to generate payload signature [contextIds: %s, nodeId: %s]", contextIds, nodeId),
					e);
		}
		return new CryptoPrimitivesSignature(signature);
	}

	public VotingServerConfirmPayload deserializeRequest(final ImmutableByteArray bytes) {
		checkNotNull(bytes);
		try {
			return objectMapper.readValue(bytes.elements(), VotingServerConfirmPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public ImmutableByteArray serializeResponse(final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload) {
		checkNotNull(controlComponenthlVCCSharePayload);
		try {
			return new ImmutableByteArray(objectMapper.writeValueAsBytes(controlComponenthlVCCSharePayload));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
	}
}
