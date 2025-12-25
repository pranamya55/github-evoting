/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.protocol;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.tools.disputeresolver.process.input.DisputeResolverInput;

@Service
public class CheckExtractedElectionEventConsistencyService {
	private static final Logger LOGGER = LoggerFactory.getLogger(CheckExtractedElectionEventConsistencyService.class);

	public final CheckExtractedElectionEventConsistencyAlgorithm checkExtractedElectionEventConsistencyAlgorithm;

	public CheckExtractedElectionEventConsistencyService(
			final CheckExtractedElectionEventConsistencyAlgorithm checkExtractedElectionEventConsistencyAlgorithm) {
		this.checkExtractedElectionEventConsistencyAlgorithm = checkExtractedElectionEventConsistencyAlgorithm;
	}

	/**
	 * Invokes the CheckExtractedElectionEventConsistency algorithm.
	 *
	 * @param disputeResolverInput the {@link DisputeResolverInput} containing the control component extracted election events payloads. Must be
	 *                             non-null.
	 * @return true if the extracted election events are consistent, false otherwise.
	 */
	public boolean checkExtractedElectionEventConsistency(final DisputeResolverInput disputeResolverInput) {
		checkNotNull(disputeResolverInput);

		// Prepare input.
		final ImmutableList<ExtractedElectionEvent> input = disputeResolverInput.controlComponentExtractedElectionEventPayloads()
				.stream()
				.map(ControlComponentExtractedElectionEventPayload::getExtractedElectionEvent)
				.collect(toImmutableList());

		LOGGER.debug("Performing CheckExtractedElectionEventConsistency algorithm...");

		// Call the algorithm.
		return checkExtractedElectionEventConsistencyAlgorithm.checkExtractedElectionEventConsistency(input);
	}
}
