/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.configuration.upload;

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

import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventState;
import ch.post.it.evoting.controlcomponent.process.ElectionEventStateService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.configuration.setupvoting.LongVoteCastReturnCodesAllowListResponsePayload;
import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

@Service
public class UploadLongVoteCastReturnCodesAllowListProcessor {

	public static final Logger LOGGER = LoggerFactory.getLogger(UploadLongVoteCastReturnCodesAllowListProcessor.class);

	private final ObjectMapper objectMapper;
	private final ElectionEventContextService electionEventContextService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final ElectionEventStateService electionEventStateService;
	private final VerificationCardSetService verificationCardSetService;

	@Value("${nodeID}")
	private int nodeId;

	public UploadLongVoteCastReturnCodesAllowListProcessor(
			final ObjectMapper objectMapper,
			final ElectionEventContextService electionEventContextService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ElectionEventStateService electionEventStateService,
			final VerificationCardSetService verificationCardSetService) {
		this.objectMapper = objectMapper;
		this.electionEventContextService = electionEventContextService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.electionEventStateService = electionEventStateService;
		this.verificationCardSetService = verificationCardSetService;
	}

	@Transactional
	public LongVoteCastReturnCodesAllowListResponsePayload onRequest(final SetupComponentLVCCAllowListPayload longVoteCastCodeAllowListPayload) {
		checkNotNull(longVoteCastCodeAllowListPayload);
		final String electionEventId = longVoteCastCodeAllowListPayload.getElectionEventId();

		// Validate election event state. Implicitly checks election event existence.
		final ElectionEventState expectedState = ElectionEventState.INITIAL;
		final ElectionEventState electionEventState = electionEventStateService.getElectionEventState(electionEventId);
		checkState(expectedState.equals(electionEventState),
				"The election event is not in the expected state. [electionEventId: %s, nodeId: %s, expected: %s, actual: %s]", electionEventId,
				nodeId, expectedState, electionEventState);

		// Verify election event is not over.
		final LocalDateTime electionEventFinishTime = electionEventContextService.getElectionEventFinishTime(electionEventId);
		checkState(LocalDateTimeUtils.now().isBefore(electionEventFinishTime), "The election event is over. [electionEventId: %s, nodeId: %s]",
				electionEventId, nodeId);

		final String verificationCardSetId = longVoteCastCodeAllowListPayload.getVerificationCardSetId();
		final ImmutableList<String> longVoteCastReturnCodesAllowList = longVoteCastCodeAllowListPayload.getLongVoteCastReturnCodesAllowList();

		verificationCardSetService.setLongVoteCastReturnCodesAllowList(verificationCardSetId, longVoteCastReturnCodesAllowList);

		LOGGER.info("Saved long vote return codes code allow list. [electionEventId: {}, verificationCardSetId: {}, nodeId: {}]", electionEventId,
				verificationCardSetId, nodeId);

		return new LongVoteCastReturnCodesAllowListResponsePayload(nodeId, electionEventId, verificationCardSetId);
	}

	public LongVoteCastReturnCodesAllowListResponsePayload onReplay(final SetupComponentLVCCAllowListPayload longVoteCastCodeAllowListPayload) {
		checkNotNull(longVoteCastCodeAllowListPayload);

		final String electionEventId = longVoteCastCodeAllowListPayload.getElectionEventId();
		final String verificationCardSetId = longVoteCastCodeAllowListPayload.getVerificationCardSetId();

		return new LongVoteCastReturnCodesAllowListResponsePayload(nodeId, electionEventId, verificationCardSetId);
	}

	public boolean verifyPayloadSignature(final SetupComponentLVCCAllowListPayload setupComponentLVCCAllowListPayload) {
		checkNotNull(setupComponentLVCCAllowListPayload);
		final String electionEventId = setupComponentLVCCAllowListPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentLVCCAllowListPayload.getVerificationCardSetId();
		final CryptoPrimitivesSignature signature = setupComponentLVCCAllowListPayload.getSignature();

		checkState(signature != null,
				"The signature of setup component LVCC allow list payload is null. [electionEventId: %s, verificationCardSetId: %s]",
				electionEventId, verificationCardSetId);

		final Hashable additionalContextData = ChannelSecurityContextData.setupComponentLVCCAllowList(electionEventId, verificationCardSetId);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.SDM_CONFIG, setupComponentLVCCAllowListPayload,
					additionalContextData, signature.signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(String.format(
					"Cannot verify the signature of setup component LVCC allow list payload. [electionEventId: %s, verificationCardSetId: %s, nodeId: %s]",
					electionEventId, verificationCardSetId, nodeId), e);
		}
		return isSignatureValid;
	}

	public SetupComponentLVCCAllowListPayload deserializeRequest(final ImmutableByteArray bytes) {
		checkNotNull(bytes);
		try {
			return objectMapper.readValue(bytes.elements(), SetupComponentLVCCAllowListPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public ImmutableByteArray serializeResponse(
			final LongVoteCastReturnCodesAllowListResponsePayload longVoteCastReturnCodesAllowListResponsePayload) {
		checkNotNull(longVoteCastReturnCodesAllowListResponsePayload);
		try {
			return new ImmutableByteArray(objectMapper.writeValueAsBytes(longVoteCastReturnCodesAllowListResponsePayload));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
	}
}
