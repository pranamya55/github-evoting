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

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.Language;

class AnswerSummaryTest {

	private int answerPosition;
	private ImmutableMap<String, String> answerInfo;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final AnswerSummary answerSummary = summaryGenerator.generateAnswerSummary();
		answerPosition = answerSummary.answerPosition();
		answerInfo = answerSummary.answerInfo();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new AnswerSummary(answerPosition, answerInfo));
	}

	@Test
	@DisplayName("should throw exception when answerPosition is negative")
	void shouldThrowExceptionWhenAnswerPositionIsNegative() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new AnswerSummary(-1, answerInfo));

		final String expected = "The answer position must be positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when answerInfo is null")
	void shouldThrowExceptionWhenAnswerInfoIsNull() {
		assertThrows(NullPointerException.class, () -> new AnswerSummary(answerPosition, null));
	}

	@Test
	@DisplayName("should throw exception when answerInfo is empty")
	void shouldThrowExceptionWhenAnswerInfoIsEmpty() {
		final ImmutableMap<String, String> emptyAnswerInfo = ImmutableMap.of();
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new AnswerSummary(answerPosition, emptyAnswerInfo));

		final String expected = "The language map must contain all languages.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when answerInfo is too small")
	void shouldThrowExceptionWhenAnswerInfoIsTooSmall() {
		final ImmutableMap<String, String> tooSmallAnswerInfo = ImmutableMap.of(Language.DE.name(), "Ja");
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new AnswerSummary(answerPosition, tooSmallAnswerInfo));

		final String expected = "The language map must contain all languages.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when answerInfo contains blank info")
	void shouldThrowExceptionWhenAnswerInfoContainsBlankInfo() {
		final ImmutableMap<String, String> blankAnswerInfo = ImmutableMap.of(
				Language.DE.name(), "Ja",
				Language.FR.name(), "Oui",
				Language.IT.name(), "Sì",
				Language.RM.name(), "  "
		);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new AnswerSummary(answerPosition, blankAnswerInfo));

		final String expected = "String to validate must not be blank.";
		assertEquals(expected, exception.getMessage());
	}
}