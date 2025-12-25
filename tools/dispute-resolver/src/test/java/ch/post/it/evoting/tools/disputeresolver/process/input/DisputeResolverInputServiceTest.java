/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.input;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedElectionEventPayloadGenerator;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedVerificationCardsPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;

@DisplayName("DisputeResolverInputService")
class DisputeResolverInputServiceTest {

	private final ControlComponentExtractedElectionEventPayloadService controlComponentExtractedElectionEventPayloadService =
			mock(ControlComponentExtractedElectionEventPayloadService.class);
	private final ControlComponentExtractedVerificationCardsPayloadService controlComponentExtractedVerificationCardsPayloadService =
			mock(ControlComponentExtractedVerificationCardsPayloadService.class);

	private final ControlComponentExtractedElectionEventPayloadGenerator controlComponentExtractedElectionEventPayloadGenerator = new ControlComponentExtractedElectionEventPayloadGenerator();
	private final ControlComponentExtractedVerificationCardsPayloadGenerator controlComponentExtractedVerificationCardsPayloadGenerator = new ControlComponentExtractedVerificationCardsPayloadGenerator();

	private DisputeResolverInputService disputeResolverInputService;

	@BeforeEach
	void setUp() {
		disputeResolverInputService = new DisputeResolverInputService(
				controlComponentExtractedElectionEventPayloadService,
				controlComponentExtractedVerificationCardsPayloadService);
	}

	@Test
	@DisplayName("calling read behaves as expected.")
	void readHappyPath() {

		final ImmutableList<ControlComponentExtractedElectionEventPayload> controlComponentExtractedElectionEventPayloads = controlComponentExtractedElectionEventPayloadGenerator.generate();
		final ImmutableList<ControlComponentExtractedVerificationCardsPayload> controlComponentExtractedVerificationCardsPayloads =
				controlComponentExtractedVerificationCardsPayloadGenerator.generate(controlComponentExtractedElectionEventPayloads.getFirst());

		when(controlComponentExtractedElectionEventPayloadService.loadAll())
				.thenReturn(controlComponentExtractedElectionEventPayloads);
		when(controlComponentExtractedVerificationCardsPayloadService.loadAll())
				.thenReturn(controlComponentExtractedVerificationCardsPayloads);

		final DisputeResolverInput disputeResolverInput = assertDoesNotThrow(() -> disputeResolverInputService.read());

		assertEquals(controlComponentExtractedElectionEventPayloads, disputeResolverInput.controlComponentExtractedElectionEventPayloads());
		assertEquals(controlComponentExtractedVerificationCardsPayloads, disputeResolverInput.controlComponentExtractedVerificationCardsPayloads());
	}

}
