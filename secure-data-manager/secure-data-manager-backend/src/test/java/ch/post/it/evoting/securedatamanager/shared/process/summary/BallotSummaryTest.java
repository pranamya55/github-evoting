/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

class BallotSummaryTest {

	private String ballotId;
	private int ballotPosition;
	private ImmutableList<DescriptionSummary> ballotDescription;
	private ImmutableList<QuestionSummary> questions;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final BallotSummary ballotSummary = summaryGenerator.generateBallotSummary();
		ballotId = ballotSummary.ballotId();
		ballotPosition = ballotSummary.ballotPosition();
		ballotDescription = ballotSummary.ballotDescription();
		questions = ballotSummary.questions();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new BallotSummary(ballotId, ballotPosition, ballotDescription, questions));
	}

	@Test
	@DisplayName("should throw exception when ballotId is null")
	void shouldThrowExceptionWhenBallotIdIsNull() {
		assertThrows(NullPointerException.class, () -> new BallotSummary(null, ballotPosition, ballotDescription, questions));
	}

	@Test
	@DisplayName("should throw exception when ballotPosition is negative")
	void shouldThrowExceptionWhenBallotPositionIsNegative() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new BallotSummary(ballotId, -1, ballotDescription, questions));

		final String expected = "The ballot position must be positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when ballotDescription is null")
	void shouldThrowExceptionWhenBallotDescriptionIsNull() {
		assertThrows(NullPointerException.class, () -> new BallotSummary(ballotId, ballotPosition, null, questions));
	}

	@Test
	@DisplayName("should throw exception when questions is null")
	void shouldThrowExceptionWhenQuestionsIsNull() {
		assertThrows(NullPointerException.class, () -> new BallotSummary(ballotId, ballotPosition, ballotDescription, null));
	}
}
