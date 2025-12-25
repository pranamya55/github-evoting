/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.protocol;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedElectionEventPayloadGenerator;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedVerificationCardsPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.tools.disputeresolver.process.input.DisputeResolverInput;

@DisplayName("CheckExtractedElectionEventConsistencyService with")
class CheckExtractedElectionEventConsistencyServiceTest {

	private final ControlComponentExtractedElectionEventPayloadGenerator controlComponentExtractedElectionEventPayloadGenerator = new ControlComponentExtractedElectionEventPayloadGenerator();
	private final ControlComponentExtractedVerificationCardsPayloadGenerator controlComponentExtractedVerificationCardsPayloadGenerator = new ControlComponentExtractedVerificationCardsPayloadGenerator();

	private final CheckExtractedElectionEventConsistencyAlgorithm checkExtractedElectionEventConsistencyAlgorithm =
			mock(CheckExtractedElectionEventConsistencyAlgorithm.class);
	private final CheckExtractedElectionEventConsistencyService checkExtractedElectionEventConsistencyService =
			new CheckExtractedElectionEventConsistencyService(checkExtractedElectionEventConsistencyAlgorithm);

	@Test
	@DisplayName("consistent dispute resolver input returns true.")
	void checkExtractedElectionEventConsistencyHappyPath() {
		final ImmutableList<ControlComponentExtractedElectionEventPayload> controlComponentExtractedElectionEventPayloads = controlComponentExtractedElectionEventPayloadGenerator.generate();
		final ImmutableList<ControlComponentExtractedVerificationCardsPayload> controlComponentExtractedVerificationCardsPayloads =
				controlComponentExtractedVerificationCardsPayloadGenerator.generate(controlComponentExtractedElectionEventPayloads.getFirst());

		final DisputeResolverInput disputeResolverInput = new DisputeResolverInput(
				controlComponentExtractedElectionEventPayloads,
				controlComponentExtractedVerificationCardsPayloads
		);

		when(checkExtractedElectionEventConsistencyAlgorithm.checkExtractedElectionEventConsistency(
				controlComponentExtractedElectionEventPayloads.stream()
						.map(ControlComponentExtractedElectionEventPayload::getExtractedElectionEvent)
						.collect(toImmutableList())))
				.thenReturn(true);

		final boolean result = assertDoesNotThrow(
				() -> checkExtractedElectionEventConsistencyService.checkExtractedElectionEventConsistency(disputeResolverInput));

		assertTrue(result);
	}

	@Test
	@DisplayName("null dispute resolver input throws NullPointerException.")
	void checkExtractedElectionEventConsistencyThrowsWhenGivenNullInput() {
		assertThrows(NullPointerException.class, () -> checkExtractedElectionEventConsistencyService.checkExtractedElectionEventConsistency(null));
	}
}
