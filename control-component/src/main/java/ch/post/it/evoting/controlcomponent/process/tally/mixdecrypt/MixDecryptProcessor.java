/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt;

import static ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt.MixDecryptService.MixDecryptServiceOutput;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SignatureException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineRequestPayload;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineResponsePayload;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentVotesHashPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;

@Service
public class MixDecryptProcessor {

	private static final String PAYLOAD_NOT_INTENDED_FOR_CONTROL_COMPONENT_MESSAGE = "The payload is not intended for the current control component. [nodeId: {}, payloadNodeId: {}]";

	private final ObjectMapper objectMapper;
	private final MixDecryptService mixDecryptService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final int mixDecryptTransactionTimeout;

	@Value("${nodeID}")
	private int nodeId;

	MixDecryptProcessor(
			final ObjectMapper objectMapper,
			final MixDecryptService mixDecryptService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			@Value("${spring.transaction.mixDecrypt-timeout}")
			final int mixDecryptTransactionTimeout) {
		this.objectMapper = objectMapper;
		this.mixDecryptService = mixDecryptService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.mixDecryptTransactionTimeout = mixDecryptTransactionTimeout;
	}

	public void preValidateRequest(final MixDecryptOnlineRequestPayload mixDecryptOnlineRequestPayload) {
		// The first node omits the VerifyMixDecOnline algorithm, it has nothing to verify.
		if (nodeId == ControlComponentNode.first().id()) {
			return;
		}

		checkNotNull(mixDecryptOnlineRequestPayload);

		checkState(mixDecryptOnlineRequestPayload.nodeId() == nodeId, PAYLOAD_NOT_INTENDED_FOR_CONTROL_COMPONENT_MESSAGE, nodeId,
				mixDecryptOnlineRequestPayload.nodeId());

		final String electionEventId = mixDecryptOnlineRequestPayload.electionEventId();
		final String ballotBoxId = mixDecryptOnlineRequestPayload.ballotBoxId();

		final ImmutableList<ControlComponentVotesHashPayload> controlComponentVotesHashPayloads = mixDecryptOnlineRequestPayload.controlComponentVotesHashPayloads();
		final ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads = mixDecryptOnlineRequestPayload.controlComponentShufflePayloads();

		mixDecryptService.performVerifyMixDecOnline(electionEventId, ballotBoxId, controlComponentVotesHashPayloads, controlComponentShufflePayloads);
	}

	@Transactional
	public MixDecryptOnlineResponsePayload onRequest(final MixDecryptOnlineRequestPayload mixDecryptOnlineRequestPayload) {
		checkNotNull(mixDecryptOnlineRequestPayload);

		checkState(mixDecryptOnlineRequestPayload.nodeId() == nodeId, PAYLOAD_NOT_INTENDED_FOR_CONTROL_COMPONENT_MESSAGE, nodeId,
				mixDecryptOnlineRequestPayload.nodeId());

		final String electionEventId = mixDecryptOnlineRequestPayload.electionEventId();
		final String ballotBoxId = mixDecryptOnlineRequestPayload.ballotBoxId();

		final ImmutableList<ControlComponentVotesHashPayload> controlComponentVotesHashPayloads = mixDecryptOnlineRequestPayload.controlComponentVotesHashPayloads();
		final ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads = mixDecryptOnlineRequestPayload.controlComponentShufflePayloads();

		final MixDecryptServiceOutput mixDecryptServiceOutput = mixDecryptService.performMixDecOnline(electionEventId, ballotBoxId,
				controlComponentVotesHashPayloads, controlComponentShufflePayloads);

		return createMixDecryptOnlineResponsePayload(mixDecryptServiceOutput, electionEventId, ballotBoxId);
	}

	public MixDecryptOnlineResponsePayload onReplay(final MixDecryptOnlineRequestPayload mixDecryptOnlineRequestPayload) {
		checkNotNull(mixDecryptOnlineRequestPayload);

		checkState(mixDecryptOnlineRequestPayload.nodeId() == nodeId, PAYLOAD_NOT_INTENDED_FOR_CONTROL_COMPONENT_MESSAGE, nodeId,
				mixDecryptOnlineRequestPayload.nodeId());

		final String electionEventId = mixDecryptOnlineRequestPayload.electionEventId();
		final String ballotBoxId = mixDecryptOnlineRequestPayload.ballotBoxId();

		final MixDecryptServiceOutput mixDecryptServiceOutput = mixDecryptService.createMixDecryptServiceOutput(electionEventId, ballotBoxId);

		return createMixDecryptOnlineResponsePayload(mixDecryptServiceOutput, electionEventId, ballotBoxId);
	}

	public MixDecryptOnlineRequestPayload deserializeRequest(final ImmutableByteArray messageBytes) {
		checkNotNull(messageBytes);
		try {
			return objectMapper.readValue(messageBytes.elements(), MixDecryptOnlineRequestPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException("Unable to deserialize Mix Decrypt Online Request Payload", e);
		}
	}

	public ImmutableByteArray serializeResponse(final MixDecryptOnlineResponsePayload mixDecryptOnlineResponsePayload) {
		checkNotNull(mixDecryptOnlineResponsePayload);
		try {
			return new ImmutableByteArray(objectMapper.writeValueAsBytes(mixDecryptOnlineResponsePayload));
		} catch (final IOException e) {
			throw new UncheckedIOException("Unable to serialize Mix Decrypt Online Response Payload", e);
		}

	}

	public boolean verifyPayloadSignature(final MixDecryptOnlineRequestPayload mixDecryptOnlineRequestPayload) {
		checkNotNull(mixDecryptOnlineRequestPayload);

		mixDecryptOnlineRequestPayload.controlComponentShufflePayloads().forEach(controlComponentShufflePayload -> {
			final int payloadNodeId = controlComponentShufflePayload.getNodeId();
			final String electionEventId = controlComponentShufflePayload.getElectionEventId();
			final String ballotBoxId = controlComponentShufflePayload.getBallotBoxId();
			final String contextMessage = String.format("[nodeId: %s, electionEventId: %s, ballotBoxId: %s]", payloadNodeId, electionEventId,
					ballotBoxId);

			final CryptoPrimitivesSignature signature = controlComponentShufflePayload.getSignature();

			checkState(signature != null, "The signature of the control component shuffle payload is null. %s", contextMessage);

			final Hashable additionalContextData = ChannelSecurityContextData.controlComponentShuffle(payloadNodeId, electionEventId, ballotBoxId);

			final boolean isSignatureValid;
			try {
				isSignatureValid = signatureKeystoreService.verifySignature(Alias.getControlComponentByNodeId(payloadNodeId),
						controlComponentShufflePayload, additionalContextData, signature.signatureContents());
			} catch (final SignatureException e) {
				throw new IllegalStateException(
						String.format("Could not verify the signature of %s. %s", controlComponentShufflePayload.getClass().getSimpleName(),
								contextMessage));
			}

			if (!isSignatureValid) {
				throw new InvalidPayloadSignatureException(ControlComponentShufflePayload.class, contextMessage);
			}
		});

		mixDecryptOnlineRequestPayload.controlComponentVotesHashPayloads().forEach(controlComponentVotesHashPayload -> {
			final String electionEventId = controlComponentVotesHashPayload.getElectionEventId();
			final String ballotBoxId = controlComponentVotesHashPayload.getBallotBoxId();
			final int payloadNodeId = controlComponentVotesHashPayload.getNodeId();
			final String contextMessage = String.format("[nodeId: %s, electionEventId: %s, ballotBoxId: %s]", payloadNodeId, electionEventId,
					ballotBoxId);

			final CryptoPrimitivesSignature signature = controlComponentVotesHashPayload.getSignature();

			checkState(signature != null, "The signature of the control component votes hash payload is null. %s", contextMessage);

			final Hashable additionalContextData = ChannelSecurityContextData.controlComponentVotesHash(payloadNodeId, electionEventId, ballotBoxId);

			final boolean isSignatureValid;
			try {
				isSignatureValid = signatureKeystoreService.verifySignature(Alias.getControlComponentByNodeId(payloadNodeId),
						controlComponentVotesHashPayload, additionalContextData, signature.signatureContents());
			} catch (final SignatureException e) {
				throw new IllegalStateException(
						String.format("Could not verify the signature of %s. %s", controlComponentVotesHashPayload.getClass().getSimpleName(),
								contextMessage));
			}

			if (!isSignatureValid) {
				throw new InvalidPayloadSignatureException(ControlComponentShufflePayload.class, contextMessage);
			}
		});

		return true;
	}

	/**
	 * @return the transaction timeout for the mix decrypt transaction.
	 */
	public Optional<Integer> getTransactionTimeout() {
		return Optional.of(mixDecryptTransactionTimeout);
	}

	private MixDecryptOnlineResponsePayload createMixDecryptOnlineResponsePayload(final MixDecryptServiceOutput mixDecryptServiceOutput,
			final String electionEventId, final String ballotBoxId) {
		final ControlComponentBallotBoxPayload controlComponentBallotBoxPayload = mixDecryptServiceOutput.controlComponentBallotBoxPayload();
		final ControlComponentShufflePayload controlComponentShufflePayload = mixDecryptServiceOutput.controlComponentShufflePayload();

		controlComponentBallotBoxPayload.setSignature(generatePayloadSignature(controlComponentBallotBoxPayload,
				ChannelSecurityContextData.controlComponentBallotBox(nodeId, electionEventId, ballotBoxId)));

		controlComponentShufflePayload.setSignature(
				generatePayloadSignature(controlComponentShufflePayload,
						ChannelSecurityContextData.controlComponentShuffle(nodeId, electionEventId, ballotBoxId)));

		return new MixDecryptOnlineResponsePayload(controlComponentBallotBoxPayload, controlComponentShufflePayload);
	}

	private CryptoPrimitivesSignature generatePayloadSignature(final SignedPayload payload, final Hashable additionalContextData) {

		try {
			final ImmutableByteArray signature = signatureKeystoreService.generateSignature(payload, additionalContextData);

			return new CryptoPrimitivesSignature(signature);

		} catch (final SignatureException se) {
			throw new IllegalStateException(
					String.format("Failed to generate payload signature [%s, %s]", payload.getClass().getSimpleName(), additionalContextData), se);
		}
	}
}
