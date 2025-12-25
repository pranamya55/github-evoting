/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;

class VerificationCardStateEntityTest {

	@Test
	void updateStateDoesNotThrow() {
		final VerificationCardStateEntity verificationCardStateEntity = new VerificationCardStateEntity();
		final LocalDateTime initialStateDate = verificationCardStateEntity.getStateDate();
		await().pollDelay(Duration.ofMillis(1000)).until(() -> true);

		assertNotEquals(VerificationCardState.SENT, verificationCardStateEntity.getState());

		verificationCardStateEntity.updateState(VerificationCardState.SENT);

		assertEquals(VerificationCardState.SENT, verificationCardStateEntity.getState());
		assertTrue(initialStateDate.isBefore(verificationCardStateEntity.getStateDate()));
	}

	@Test
	void updateStateThrowsOnNullInput() {
		final VerificationCardStateEntity verificationCardStateEntity = new VerificationCardStateEntity();

		assertThrows(NullPointerException.class, () -> verificationCardStateEntity.updateState(null));
	}
}
