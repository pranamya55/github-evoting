/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.ExtractedElectionEventHashService;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashExtractedElectionEventAlgorithm;

@Service
public class GetHashExtractedElectionEventService {

	private final ExtractedElectionEventHashService extractedElectionEventHashService;

	public GetHashExtractedElectionEventService(final ExtractedElectionEventHashService extractedElectionEventHashService) {
		this.extractedElectionEventHashService = extractedElectionEventHashService;
	}

	/**
	 * Gets the hash of the extracted election event.
	 * <p>
	 * For performance reasons, the control component precomputes the result of the algorithms {@link ExtractElectionEventAlgorithm} and
	 * {@link GetHashExtractedElectionEventAlgorithm} to avoid recalculating it for each vote.
	 * <p>
	 * The hash is computed and stored in {@link ExtractedElectionEventHashService#computeAndSave}.
	 *
	 * @param electionEventId ee, the election event id. Must be non-null and a valid UUID.
	 * @return the previously computed hash of the extracted election event for the given election event id.
	 */
	@Cacheable(value = "extractedElectionEventHashes", sync = true)
	public String getHashExtractedElectionEvent(final String electionEventId) {
		validateUUID(electionEventId);

		return extractedElectionEventHashService.getHashExtractedElectionEvent(electionEventId);
	}
}
