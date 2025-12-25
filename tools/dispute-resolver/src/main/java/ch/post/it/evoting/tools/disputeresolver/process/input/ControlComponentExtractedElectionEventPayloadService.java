/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.input;

import static com.google.common.base.Preconditions.checkState;

import java.security.SignatureException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

@Service
public class ControlComponentExtractedElectionEventPayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ControlComponentExtractedElectionEventPayloadService.class);

	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final ControlComponentExtractedElectionEventPayloadFileRepository controlComponentExtractedElectionEventPayloadFileRepository;

	public ControlComponentExtractedElectionEventPayloadService(
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ControlComponentExtractedElectionEventPayloadFileRepository controlComponentExtractedElectionEventPayloadFileRepository) {

		this.signatureKeystoreService = signatureKeystoreService;
		this.controlComponentExtractedElectionEventPayloadFileRepository = controlComponentExtractedElectionEventPayloadFileRepository;
	}

	/**
	 * Loads all control component extracted election event payloads in the file system and validates their signatures.
	 *
	 * @return an immutable list of control component extracted election event payloads.
	 * @throws InvalidPayloadSignatureException if the signature of any payload is invalid.
	 * @throws IllegalStateException            if
	 *                                          <ul>
	 *                                              <li>any payload has a null signature.</li>
	 *                                              <li>an exception occurs while verifying the signature of any payload.</li>
	 *                                          </ul>
	 */
	public ImmutableList<ControlComponentExtractedElectionEventPayload> loadAll() {
		LOGGER.debug("Loading all control component extracted election event payloads...");

		final ImmutableList<ControlComponentExtractedElectionEventPayload> controlComponentExtractedElectionEventPayloads = controlComponentExtractedElectionEventPayloadFileRepository.findAll();

		LOGGER.info("Loaded all control component extracted election event payloads. Validating signatures...");

		controlComponentExtractedElectionEventPayloads.forEach(this::validateSignature);

		LOGGER.info("Successfully validated the signatures of all control component extracted election event payloads.");

		return controlComponentExtractedElectionEventPayloads;
	}

	private void validateSignature(final ControlComponentExtractedElectionEventPayload controlComponentExtractedElectionEventPayload) {

		final CryptoPrimitivesSignature signature = controlComponentExtractedElectionEventPayload.getSignature();
		final int nodeId = controlComponentExtractedElectionEventPayload.getNodeId();
		final String electionEventId = controlComponentExtractedElectionEventPayload.getExtractedElectionEvent().electionEventId();

		LOGGER.debug("Validating signature of control component extracted election event payload... [electionEventId: {}, nodeId: {}]",
				electionEventId, nodeId);

		checkState(Objects.nonNull(signature),
				"The signature of the control component extracted election event payload is null. [electionEventId: %s, nodeId: %s]", electionEventId,
				nodeId);

		final Hashable additionalContextData = ChannelSecurityContextData.controlComponentExtractedElectionEvent(nodeId, electionEventId);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.getControlComponentByNodeId(nodeId),
					controlComponentExtractedElectionEventPayload, additionalContextData, signature.signatureContents());

		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format(
							"Cannot verify the signature of control component extracted election event payload. [electionEventId: %s, nodeId: %s]",
							electionEventId, nodeId), e);
		}

		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(ControlComponentExtractedElectionEventPayload.class,
					String.format("[electionEventId: %s, nodeId: %s]", electionEventId, nodeId));
		}

		LOGGER.info("\tSignature of control component extracted election event payload is valid for control component {}.", nodeId);
	}

}
