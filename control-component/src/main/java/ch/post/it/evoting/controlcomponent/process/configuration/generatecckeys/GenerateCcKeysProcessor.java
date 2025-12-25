/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.configuration.generatecckeys;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

@Service
public class GenerateCcKeysProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateCcKeysProcessor.class);

	private final ObjectMapper objectMapper;
	private final GenerateCcKeysService generateCcKeysService;
	private final ElectionEventService electionEventService;
	private final ElectionEventContextService electionEventContextService;
	private final SignatureKeystore<Alias> signatureKeystoreService;

	@Value("${nodeID}")
	private int nodeId;

	GenerateCcKeysProcessor(final ObjectMapper objectMapper,
			final GenerateCcKeysService generateCcKeysService,
			final ElectionEventService electionEventService,
			final ElectionEventContextService electionEventContextService,
			final SignatureKeystore<Alias> signatureKeystoreService) {
		this.objectMapper = objectMapper;
		this.generateCcKeysService = generateCcKeysService;
		this.electionEventService = electionEventService;
		this.electionEventContextService = electionEventContextService;
		this.signatureKeystoreService = signatureKeystoreService;
	}

	@Transactional
	public ControlComponentPublicKeysPayload onRequest(final ElectionEventContextPayload electionEventContextPayload) {
		checkNotNull(electionEventContextPayload);
		final ElectionEventContext electionEventContext = electionEventContextPayload.getElectionEventContext();
		final String electionEventId = electionEventContext.electionEventId();

		checkArgument(electionEventContext.finishTime().isAfter(LocalDateTimeUtils.now()),
				"The election event period should not be finished yet. [electionEventId: %s, nodeId: %s]", electionEventId, nodeId);

		// Save the encryption group.
		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		electionEventService.save(electionEventId, encryptionGroup);
		LOGGER.info("Saved encryption parameters. [electionEventId: {}, nodeId: {}]", electionEventId, nodeId);

		// Save the election event context.
		electionEventContextService.save(electionEventContext);
		LOGGER.info("Saved election event context. [electionEventId: {}, nodeId: {}]", electionEventId, nodeId);

		// Generate ccrj and ccmj keys.
		final ControlComponentPublicKeys controlComponentPublicKeys = generateCcKeysService.performGenKeysCCR(encryptionGroup, electionEventId,
				electionEventContext);

		// Create and sign payload.
		final ControlComponentPublicKeysPayload controlComponentPublicKeysPayload = new ControlComponentPublicKeysPayload(encryptionGroup,
				electionEventId, controlComponentPublicKeys);
		controlComponentPublicKeysPayload.setSignature(generatePayloadSignature(controlComponentPublicKeysPayload));

		LOGGER.info("Successfully signed control component public keys payload. [electionEventId: {}, nodeId: {}]", electionEventId, nodeId);

		return controlComponentPublicKeysPayload;
	}

	public ControlComponentPublicKeysPayload onReplay(final ElectionEventContextPayload electionEventContextPayload) {
		checkNotNull(electionEventContextPayload);

		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		final String electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();

		final ControlComponentPublicKeys controlComponentPublicKeys = generateCcKeysService.getCcKeys(electionEventId);

		final ControlComponentPublicKeysPayload controlComponentPublicKeysPayload = new ControlComponentPublicKeysPayload(encryptionGroup,
				electionEventId, controlComponentPublicKeys);
		controlComponentPublicKeysPayload.setSignature(generatePayloadSignature(controlComponentPublicKeysPayload));

		return controlComponentPublicKeysPayload;
	}

	public ElectionEventContextPayload deserializeRequest(final ImmutableByteArray messageBytes) {
		checkNotNull(messageBytes);
		try {
			return objectMapper.readValue(messageBytes.elements(), ElectionEventContextPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException("Unable to deserialize Election Event Context payload", e);
		}
	}

	public ImmutableByteArray serializeResponse(final ControlComponentPublicKeysPayload controlComponentPublicKeysPayload) {
		checkNotNull(controlComponentPublicKeysPayload);
		try {
			return new ImmutableByteArray(objectMapper.writeValueAsBytes(controlComponentPublicKeysPayload));
		} catch (final IOException e) {
			throw new UncheckedIOException("Unable to serialize Control Component Public Keys Payload", e);
		}

	}

	public boolean verifyPayloadSignature(final ElectionEventContextPayload electionEventContextPayload) {
		checkNotNull(electionEventContextPayload);
		final String electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();

		final CryptoPrimitivesSignature signature = electionEventContextPayload.getSignature();

		checkState(signature != null, "The signature of the election event context payload is null. [electionEventId: %s, nodeId: %s]",
				electionEventId, nodeId);

		final Hashable additionalContextData = ChannelSecurityContextData.electionEventContext(electionEventId);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.SDM_CONFIG, electionEventContextPayload, additionalContextData,
					signature.signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Could not verify the signature of the election event context payload. [electionEventId: %s, nodeId: %s]",
							electionEventId, nodeId));
		}

		return isSignatureValid;
	}

	private CryptoPrimitivesSignature generatePayloadSignature(final ControlComponentPublicKeysPayload payload) {
		final String electionEventId = payload.getElectionEventId();

		final Hashable additionalContextData = ChannelSecurityContextData.controlComponentPublicKeys(nodeId, electionEventId);

		try {
			final ImmutableByteArray signature = signatureKeystoreService.generateSignature(payload, additionalContextData);

			return new CryptoPrimitivesSignature(signature);
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Could not generate control component public keys payload signature. [contextIds: %s, nodeId: %s]", electionEventId,
							nodeId));
		}
	}

}
