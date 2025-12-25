/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.protocol;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashExtractedElectionEventAlgorithm;

@Service
public class CheckExtractedElectionEventConsistencyAlgorithm {

	private final GetHashExtractedElectionEventAlgorithm getHashExtractedElectionEventAlgorithm;

	public CheckExtractedElectionEventConsistencyAlgorithm(
			final GetHashExtractedElectionEventAlgorithm getHashExtractedElectionEventAlgorithm) {
		this.getHashExtractedElectionEventAlgorithm = getHashExtractedElectionEventAlgorithm;
	}

	/**
	 * Checks the consistency of the CCR's extracted election events.
	 *
	 * @param input (eee<sub>1</sub>, eee<sub>2</sub>, eee<sub>3</sub>, eee<sub>4</sub>), the CCR's {@link ExtractedElectionEvent}. Must be non-null.
	 * @return true if the extracted election events are consistent, false otherwise.
	 * @throws NullPointerException     if the input is null.
	 * @throws IllegalArgumentException if the number of extracted election events differs from the number of node ids.
	 */
	@SuppressWarnings("java:S117")
	public boolean checkExtractedElectionEventConsistency(final ImmutableList<ExtractedElectionEvent> input) {

		// Input.
		final ImmutableList<ExtractedElectionEvent> eee_vector = checkNotNull(input);

		checkArgument(eee_vector.size() == ControlComponentNode.ids().size(),
				"There must be as many CCR's extracted election events as node ids.");

		// Operation.
		final ImmutableList<String> h_eee_vector = eee_vector.stream()
				// for j in [1, 4]
				.map(getHashExtractedElectionEventAlgorithm::getHashExtractedElectionEvent)
				.collect(toImmutableList());

		final String h_eee_1 = h_eee_vector.get(0);
		final String h_eee_2 = h_eee_vector.get(1);
		final String h_eee_3 = h_eee_vector.get(2);
		final String h_eee_4 = h_eee_vector.get(3);

		// h_eee_1 = h_eee_2 = h_eee_3 = h_eee_4
		return h_eee_1.equals(h_eee_2) && h_eee_2.equals(h_eee_3) && h_eee_3.equals(h_eee_4);
	}
}
