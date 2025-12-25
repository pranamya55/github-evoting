/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.compute;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

@RestController
@RequestMapping("/api/v1/configuration")
public class QueuedComputeChunkIdsController {

	private static final Logger LOGGER = LoggerFactory.getLogger(QueuedComputeChunkIdsController.class);

	private final QueuedComputeChunkIdsService queuedComputeChunkIdsService;

	public QueuedComputeChunkIdsController(final QueuedComputeChunkIdsService queuedComputeChunkIdsService) {
		this.queuedComputeChunkIdsService = queuedComputeChunkIdsService;
	}

	@GetMapping(value = "electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}/queuedcomputechunkids")
	public ImmutableList<Integer> getQueuedComputeChunkIds(
			@PathVariable
			final String electionEventId,
			@PathVariable
			final String verificationCardSetId) {

		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		LOGGER.debug("Received request for queued compute chunk ids. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);

		return queuedComputeChunkIdsService.getQueuedComputeChunkIds(electionEventId, verificationCardSetId);
	}

}
