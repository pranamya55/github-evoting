/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.input;

import static com.google.common.base.Preconditions.checkState;

import java.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

@Service
public class ControlComponentExtractedVerificationCardsPayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ControlComponentExtractedVerificationCardsPayloadService.class);

	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final ControlComponentExtractedVerificationCardsPayloadFileRepository controlComponentExtractedVerificationCardsPayloadFileRepository;

	public ControlComponentExtractedVerificationCardsPayloadService(
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ControlComponentExtractedVerificationCardsPayloadFileRepository controlComponentExtractedVerificationCardsPayloadFileRepository) {

		this.signatureKeystoreService = signatureKeystoreService;
		this.controlComponentExtractedVerificationCardsPayloadFileRepository = controlComponentExtractedVerificationCardsPayloadFileRepository;
	}

	/**
	 * Loads all control component extracted verification cards payloads in the file system and validates their signatures.
	 *
	 * @return an immutable list of control component extracted verification cards payloads.
	 * @throws InvalidPayloadSignatureException if the signature of any payload is invalid.
	 * @throws IllegalStateException            if
	 *                                          <ul>
	 *                                          	<li>any payload has a null signature.</li>
	 *                                           	<li>an exception occurs while verifying the signature of any payload.</li>
	 *                                          </ul>
	 */
	public ImmutableList<ControlComponentExtractedVerificationCardsPayload> loadAll() {
		LOGGER.debug("Loading all control component extracted verification cards payloads...");

		final ImmutableList<ControlComponentExtractedVerificationCardsPayload> controlComponentExtractedVerificationCardsPayloads = controlComponentExtractedVerificationCardsPayloadFileRepository.findAll();

		LOGGER.info("Loaded all control component extracted verification cards payloads. Validating signatures...");

		controlComponentExtractedVerificationCardsPayloads.forEach(this::validateSignature);

		LOGGER.info("Successfully validated the signatures of all control component extracted verification cards payloads.");

		return controlComponentExtractedVerificationCardsPayloads;
	}

	private void validateSignature(final ControlComponentExtractedVerificationCardsPayload controlComponentExtractedVerificationCardsPayload) {

		final CryptoPrimitivesSignature signature = controlComponentExtractedVerificationCardsPayload.getSignature();
		final int nodeId = controlComponentExtractedVerificationCardsPayload.getNodeId();
		final String electionEventId = controlComponentExtractedVerificationCardsPayload.getElectionEventId();

		LOGGER.debug("Validating signature of control component extracted verification cards payload... [electionEventId: {}, nodeId: {}]",
				electionEventId, nodeId);

		checkState(signature != null,
				"The signature of the control component extracted verification cards payload is null. [electionEventId: %s, nodeId: %s]",
				electionEventId, nodeId);

		final Hashable additionalContextData = ChannelSecurityContextData.controlComponentExtractedVerificationCards(nodeId, electionEventId);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.getControlComponentByNodeId(nodeId),
					controlComponentExtractedVerificationCardsPayload, additionalContextData, signature.signatureContents());

		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format(
							"Cannot verify the signature of control component extracted verification cards payload. [electionEventId: %s, nodeId: %s]",
							electionEventId, nodeId), e);
		}

		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(ControlComponentExtractedVerificationCardsPayload.class,
					String.format("[electionEventId: %s, nodeId: %s]", electionEventId, nodeId));
		}

		LOGGER.info("\tSignature of control component extracted extracted verification cards payload is valid for control component {}.", nodeId);
	}

}
