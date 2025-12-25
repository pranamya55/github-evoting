/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;

@Service
public class DisputeResolverInputService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DisputeResolverInputService.class);

	private final ControlComponentExtractedElectionEventPayloadService controlComponentExtractedElectionEventPayloadService;
	private final ControlComponentExtractedVerificationCardsPayloadService controlComponentExtractedVerificationCardsPayloadService;

	public DisputeResolverInputService(
			final ControlComponentExtractedElectionEventPayloadService controlComponentExtractedElectionEventPayloadService,
			final ControlComponentExtractedVerificationCardsPayloadService controlComponentExtractedVerificationCardsPayloadService) {

		this.controlComponentExtractedElectionEventPayloadService = controlComponentExtractedElectionEventPayloadService;
		this.controlComponentExtractedVerificationCardsPayloadService = controlComponentExtractedVerificationCardsPayloadService;
	}

	/**
	 * Reads the input control component payloads from the file system.
	 *
	 * @return the {@link DisputeResolverInput} containing the control component payloads.
	 */
	public DisputeResolverInput read() {
		LOGGER.debug("Reading files from file system...");

		final ImmutableList<ControlComponentExtractedElectionEventPayload> controlComponentExtractedElectionEventPayloads = controlComponentExtractedElectionEventPayloadService.loadAll();
		final ImmutableList<ControlComponentExtractedVerificationCardsPayload> controlComponentExtractedVerificationCardsPayloads = controlComponentExtractedVerificationCardsPayloadService.loadAll();

		final DisputeResolverInput disputeResolverInput = new DisputeResolverInput(
				controlComponentExtractedElectionEventPayloads,
				controlComponentExtractedVerificationCardsPayloads
		);

		LOGGER.debug("Files successfully read from file system.");

		return disputeResolverInput;
	}

}
