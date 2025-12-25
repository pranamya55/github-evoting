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

import ch.post.it.evoting.controlcomponent.process.ElectionEventState;
import ch.post.it.evoting.controlcomponent.process.ElectionEventStateService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.voting.sendvote.CombinedControlComponentPartialDecryptPayload;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentPartialDecryptPayload;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentlCCSharePayload;
import ch.post.it.evoting.domain.voting.sendvote.LongChoiceReturnCodeShare;
import ch.post.it.evoting.domain.voting.sendvote.PartiallyDecryptedEncryptedPCC;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

/**
 * Consumes the messages asking for the long Choice Return Code share.
 */
@Service
public class LongChoiceReturnCodeShareProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(LongChoiceReturnCodeShareProcessor.class);

	private final ObjectMapper objectMapper;
	private final LongChoiceReturnCodeShareService longChoiceReturnCodeShareService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final ElectionEventStateService electionEventStateService;
	private final PartiallyDecryptedPCCService partiallyDecryptedPCCService;
	private final LCCShareService lccShareService;

	@Value("${nodeID}")
	private int nodeId;

	public LongChoiceReturnCodeShareProcessor(
			final ObjectMapper objectMapper,
			final LongChoiceReturnCodeShareService longChoiceReturnCodeShareService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ElectionEventStateService electionEventStateService,
			final PartiallyDecryptedPCCService partiallyDecryptedPCCService,
			final LCCShareService lccShareService) {
		this.objectMapper = objectMapper;
		this.longChoiceReturnCodeShareService = longChoiceReturnCodeShareService;
		this.electionEventStateService = electionEventStateService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.partiallyDecryptedPCCService = partiallyDecryptedPCCService;
		this.lccShareService = lccShareService;
	}

	@Transactional
	public ControlComponentlCCSharePayload onRequest(
			final CombinedControlComponentPartialDecryptPayload combinedControlComponentPartialDecryptPayload) {
		checkNotNull(combinedControlComponentPartialDecryptPayload);

		final ImmutableList<ControlComponentPartialDecryptPayload> controlComponentPartialDecryptPayloads = combinedControlComponentPartialDecryptPayload.controlComponentPartialDecryptPayloads();
		final ContextIds contextIds = controlComponentPartialDecryptPayloads.get(0).getPartiallyDecryptedEncryptedPCC().contextIds();

		// Validate election event state.
		final ElectionEventState expectedState = ElectionEventState.CONFIGURED;
		final String electionEventId = contextIds.electionEventId();
		final ElectionEventState electionEventState = electionEventStateService.getElectionEventState(electionEventId);
		checkState(expectedState.equals(electionEventState),
				"The election event is not in the expected state. [electionEventId: %s, nodeId: %s, expected: %s, actual: %s]", electionEventId,
				nodeId, expectedState, electionEventState);

		// Perform LCC share computation.
		final LongChoiceReturnCodeShare longChoiceReturnCodeShare = longChoiceReturnCodeShareService.performCreateLCCShare(
				controlComponentPartialDecryptPayloads);
		LOGGER.info("Successfully generated the long Choice Return Code shares. [contextIds: {}]", contextIds);

		// Create and sign response payload.
		final GqGroup encryptionGroup = controlComponentPartialDecryptPayloads.get(0).getEncryptionGroup();
		final ControlComponentlCCSharePayload controlComponentLCCSharePayload = new ControlComponentlCCSharePayload(encryptionGroup,
				longChoiceReturnCodeShare);

		controlComponentLCCSharePayload.setSignature(generatePayloadSignature(controlComponentLCCSharePayload));
		LOGGER.info("Successfully signed Long Return Codes Share payload. [contextIds: {}]", contextIds);

		return controlComponentLCCSharePayload;
	}

	public ControlComponentlCCSharePayload onReplay(
			final CombinedControlComponentPartialDecryptPayload combinedControlComponentPartialDecryptPayload) {
		checkNotNull(combinedControlComponentPartialDecryptPayload);

		final GqGroup encryptionGroup = combinedControlComponentPartialDecryptPayload.controlComponentPartialDecryptPayloads()
				.get(0)
				.getEncryptionGroup();

		final ImmutableList<ControlComponentPartialDecryptPayload> controlComponentPartialDecryptPayloads = combinedControlComponentPartialDecryptPayload.controlComponentPartialDecryptPayloads();
		final ContextIds contextIds = controlComponentPartialDecryptPayloads
				.get(0)
				.getPartiallyDecryptedEncryptedPCC()
				.contextIds();

		final LongChoiceReturnCodeShare longChoiceReturnCodeShare = lccShareService.getLongChoiceReturnCodeShare(contextIds, nodeId);

		final ControlComponentlCCSharePayload controlComponentLCCSharePayload = new ControlComponentlCCSharePayload(encryptionGroup,
				longChoiceReturnCodeShare);

		controlComponentLCCSharePayload.setSignature(generatePayloadSignature(controlComponentLCCSharePayload));

		return controlComponentLCCSharePayload;
	}

	public boolean verifyPayload(final CombinedControlComponentPartialDecryptPayload combinedControlComponentPartialDecryptPayload) {
		final ContextIds contextIds = combinedControlComponentPartialDecryptPayload.controlComponentPartialDecryptPayloads().get(0)
				.getPartiallyDecryptedEncryptedPCC().contextIds();
		final String verificationCardId = contextIds.verificationCardId();

		// Verify signature of the received ControlComponentPartialDecryptPayloads.
		combinedControlComponentPartialDecryptPayload.controlComponentPartialDecryptPayloads().forEach(this::verifySignature);

		// Retrieve the partially decrypted encrypted PCC previously computed.
		final PartiallyDecryptedEncryptedPCC previouslyComputedPCCPayload = partiallyDecryptedPCCService.get(verificationCardId);

		// Get the partially decrypted encrypted PCC corresponding to this node id.
		final ControlComponentPartialDecryptPayload receivedPCCPayload = combinedControlComponentPartialDecryptPayload.controlComponentPartialDecryptPayloads()
				.stream().filter(payload -> payload.getPartiallyDecryptedEncryptedPCC().nodeId() == nodeId)
				.findAny() // Uniqueness ensured by the combined payload.
				.orElseThrow(() -> new IllegalStateException("The combined payload does not contain payload for this node id."));

		// Check that they are equal.
		if (!previouslyComputedPCCPayload.equals(receivedPCCPayload.getPartiallyDecryptedEncryptedPCC())) {
			throw new IllegalStateException("The received partially decrypted encrypted PCC is not equal to the previously computed one.");
		}

		return true;
	}

	private void verifySignature(final ControlComponentPartialDecryptPayload payload) {
		final PartiallyDecryptedEncryptedPCC partiallyDecryptedEncryptedPCC = payload.getPartiallyDecryptedEncryptedPCC();
		final ContextIds contextIds = partiallyDecryptedEncryptedPCC.contextIds();
		final int otherNodeId = partiallyDecryptedEncryptedPCC.nodeId();
		final CryptoPrimitivesSignature signature = payload.getSignature();

		checkState(signature != null, "The signature of the control component partial decrypt payload is null. [contextIds: %s]", contextIds);

		final Hashable additionalContextData = ChannelSecurityContextData.controlComponentPartialDecrypt(otherNodeId,
				contextIds.electionEventId(), contextIds.verificationCardSetId(), contextIds.verificationCardId());

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.getControlComponentByNodeId(otherNodeId), payload,
					additionalContextData, signature.signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Could not verify the signature of the control component partial decrypt payload. [contextIds: %s]", contextIds));
		}

		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(ControlComponentPartialDecryptPayload.class, String.format("[contextIds: %s]", contextIds));
		}
	}

	private CryptoPrimitivesSignature generatePayloadSignature(final ControlComponentlCCSharePayload payload) {
		final LongChoiceReturnCodeShare longChoiceReturnCodeShare = payload.getLongChoiceReturnCodeShare();
		final String electionEventId = longChoiceReturnCodeShare.electionEventId();
		final String verificationCardSetId = longChoiceReturnCodeShare.verificationCardSetId();
		final String verificationCardId = longChoiceReturnCodeShare.verificationCardId();

		final Hashable additionalContextData = ChannelSecurityContextData.controlComponentlCCShare(nodeId, electionEventId, verificationCardSetId,
				verificationCardId);

		try {
			final ImmutableByteArray signature = signatureKeystoreService.generateSignature(payload, additionalContextData);
			return new CryptoPrimitivesSignature(signature);
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Could not generate payload signature. [electionEventId: %s, verificationCardSetId: %s, verificationCardId: %s]",
							electionEventId,
							verificationCardSetId, verificationCardId));
		}
	}

	public CombinedControlComponentPartialDecryptPayload deserializeRequest(final ImmutableByteArray bytes) {
		checkNotNull(bytes);
		try {
			return objectMapper.readValue(bytes.elements(), CombinedControlComponentPartialDecryptPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public ImmutableByteArray serializeResponse(final ControlComponentlCCSharePayload controlComponentLCCSharePayload) {
		checkNotNull(controlComponentLCCSharePayload);
		try {
			return new ImmutableByteArray(objectMapper.writeValueAsBytes(controlComponentLCCSharePayload));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
	}
}
