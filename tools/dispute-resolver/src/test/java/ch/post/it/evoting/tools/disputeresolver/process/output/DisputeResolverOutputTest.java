/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.output;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.domain.generators.DisputeResolverResolvedConfirmedVotesPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.DisputeResolverResolvedConfirmedVotesPayload;

@DisplayName("DisputeResolverOutput with")
class DisputeResolverOutputTest {

	@Test
	@DisplayName("a valid payload instantiates successfully.")
	void instantiateHappyPath() {
		final DisputeResolverResolvedConfirmedVotesPayload disputeResolverResolvedConfirmedVotesPayload =
				new DisputeResolverResolvedConfirmedVotesPayloadGenerator().generate();

		final DisputeResolverOutput disputeResolverOutput = assertDoesNotThrow(
				() -> new DisputeResolverOutput(disputeResolverResolvedConfirmedVotesPayload));

		assertEquals(disputeResolverResolvedConfirmedVotesPayload, disputeResolverOutput.disputeResolverResolvedConfirmedVotesPayload());
	}

	@Test
	@DisplayName("a null payload throws a NullPointerException.")
	void instantiateThrowsWhenGivenNullInput() {
		assertThrows(NullPointerException.class, () -> new DisputeResolverOutput(null));
	}

}
