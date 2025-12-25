/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@Service
public class ExtractElectionEventService {

	private final ExtractElectionEventAlgorithm extractElectionEventAlgorithm;

	public ExtractElectionEventService(final ExtractElectionEventAlgorithm extractElectionEventAlgorithm) {
		this.extractElectionEventAlgorithm = extractElectionEventAlgorithm;
	}

	/**
	 * Call the ExtractElectionEvent algorithm.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the output of the ExtractElectionEvent algorithm.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 * @throws NullPointerException      if {@code electionEventId} is null.
	 */
	public ExtractedElectionEvent extractElectionEvent(final String electionEventId) {
		validateUUID(electionEventId);

		return extractElectionEventAlgorithm.extractElectionEvent(electionEventId);
	}

}
