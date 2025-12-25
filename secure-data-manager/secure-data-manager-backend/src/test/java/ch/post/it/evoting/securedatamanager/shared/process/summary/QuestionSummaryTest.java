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

class QuestionSummaryTest {

	private int questionPosition;
	private String questionNumber;
	private ImmutableList<DescriptionSummary> questionInfo;
	private ImmutableList<AnswerSummary> answers;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final QuestionSummary questionSummary = summaryGenerator.generateQuestionSummary();
		questionPosition = questionSummary.questionPosition();
		questionNumber = questionSummary.questionNumber();
		questionInfo = questionSummary.questionInfo();
		answers = questionSummary.answers();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new QuestionSummary(questionPosition, questionNumber, questionInfo, answers));
	}

	@Test
	@DisplayName("should throw exception when questionPosition is negative")
	void shouldThrowExceptionWhenQuestionPositionIsNegative() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new QuestionSummary( -1, questionNumber, questionInfo, answers));

		final String expected = "The questionPosition must be positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when questionNumber is blank")
	void shouldThrowExceptionWhenQuestionNumberIsBlank() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new QuestionSummary( questionPosition, " ", questionInfo, answers));

		final String expected = "String to validate must not be blank.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when questionInfo is null")
	void shouldThrowExceptionWhenQuestionInfoIsNull() {
		assertThrows(NullPointerException.class, () -> new QuestionSummary(questionPosition, questionNumber, null, answers));
	}

	@Test
	@DisplayName("should throw exception when answers is null")
	void shouldThrowExceptionWhenAnswersIsNull() {
		assertThrows(NullPointerException.class, () -> new QuestionSummary(questionPosition, questionNumber, questionInfo, null));
	}
}
