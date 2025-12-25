/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.output;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.domain.generators.DisputeResolverResolvedConfirmedVotesPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.DisputeResolverResolvedConfirmedVotesPayload;

@DisplayName("DisputeResolverOutputService calling")
class DisputeResolverOutputServiceTest {
	private final DisputeResolverResolvedConfirmedVotesPayloadService disputeResolverResolvedConfirmedVotesPayloadService =
			mock(DisputeResolverResolvedConfirmedVotesPayloadService.class);
	private final DisputeResolverOutputService disputeResolverOutputService = new DisputeResolverOutputService(
			disputeResolverResolvedConfirmedVotesPayloadService);

	@Test
	@DisplayName("save with a valid input behaves as expected.")
	void saveHappyPath() {
		final DisputeResolverResolvedConfirmedVotesPayload disputeResolverResolvedConfirmedVotesPayload = new DisputeResolverResolvedConfirmedVotesPayloadGenerator().generate();
		final DisputeResolverOutput disputeResolverOutput = new DisputeResolverOutput(disputeResolverResolvedConfirmedVotesPayload);

		doNothing().when(disputeResolverResolvedConfirmedVotesPayloadService).save(disputeResolverResolvedConfirmedVotesPayload);

		assertDoesNotThrow(() -> disputeResolverOutputService.save(disputeResolverOutput));
	}

	@Test
	@DisplayName("save with a null input throws a NullPointerException.")
	void saveThrowsWhenGivenNullInput() {
		assertThrows(NullPointerException.class, () -> disputeResolverOutputService.save(null));
	}

}
