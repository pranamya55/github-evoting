/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.protocol;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedElectionEventPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.generators.ExtractedElectionEventGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashExtractedElectionEventAlgorithm;

@DisplayName("CheckExtractedElectionEventConsistencyAlgorithm with")
class CheckExtractedElectionEventConsistencyAlgorithmTest {

	private static ImmutableList<ExtractedElectionEvent> input;
	private static CheckExtractedElectionEventConsistencyAlgorithm checkExtractedElectionEventConsistencyAlgorithm;

	@BeforeAll
	static void setUpAll() {
		final ControlComponentExtractedElectionEventPayloadGenerator generator = new ControlComponentExtractedElectionEventPayloadGenerator();

		input = generator.generate().stream()
				.map(ControlComponentExtractedElectionEventPayload::getExtractedElectionEvent)
				.collect(toImmutableList());

		final GetHashExtractedElectionEventAlgorithm getHashExtractedElectionEventAlgorithm = new GetHashExtractedElectionEventAlgorithm(
				BaseEncodingFactory.createBase64(), HashFactory.createHash());
		checkExtractedElectionEventConsistencyAlgorithm = new CheckExtractedElectionEventConsistencyAlgorithm(getHashExtractedElectionEventAlgorithm);
	}

	@Test
	@DisplayName("null input throws NullPointerException.")
	void nullInputThrows() {
		assertThrows(NullPointerException.class, () -> checkExtractedElectionEventConsistencyAlgorithm.checkExtractedElectionEventConsistency(null));
	}

	@Test
	@DisplayName("not enough extracted election events throws IllegalArgumentException.")
	void notEnoughExtractedElectionEventThrows() {
		final ImmutableList<ExtractedElectionEvent> notEnoughExtractedElectionEvent = input.subList(0, 3);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> checkExtractedElectionEventConsistencyAlgorithm.checkExtractedElectionEventConsistency(notEnoughExtractedElectionEvent));

		final String expected = "There must be as many CCR's extracted election events as node ids.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("too many extracted election events throws IllegalArgumentException.")
	void tooManyExtractedElectionEventThrows() {
		final ImmutableList<ExtractedElectionEvent> tooManyExtractedElectionEvent = input.append(input.getFirst());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> checkExtractedElectionEventConsistencyAlgorithm.checkExtractedElectionEventConsistency(tooManyExtractedElectionEvent));

		final String expected = "There must be as many CCR's extracted election events as node ids.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("inconsistent extracted election event returns false.")
	void invalidReturnsFalse() {
		final ExtractedElectionEvent otherExtractedElectionEvent = new ExtractedElectionEventGenerator().generate();
		final ImmutableList<ExtractedElectionEvent> inconsistentInput = input.subList(0, 3).append(otherExtractedElectionEvent);

		assertFalse(() -> checkExtractedElectionEventConsistencyAlgorithm.checkExtractedElectionEventConsistency(inconsistentInput));
	}

	@Test
	@DisplayName("consistent extracted election event returns true.")
	void validReturnsTrue() {
		assertTrue(() -> checkExtractedElectionEventConsistencyAlgorithm.checkExtractedElectionEventConsistency(input));
	}
}