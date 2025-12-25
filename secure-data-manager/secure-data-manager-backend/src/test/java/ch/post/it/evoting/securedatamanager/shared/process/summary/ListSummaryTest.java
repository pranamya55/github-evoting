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

class ListSummaryTest {

	private String listId;
	private String listIndentureNumber;
	private ImmutableList<DescriptionSummary> listDescription;
	private int listOrderOfPrecedence;
	private ImmutableList<CandidatePositionSummary> candidatePositions;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final ListSummary listSummary = summaryGenerator.generateListSummary();
		listId = listSummary.listIdentification();
		listIndentureNumber = listSummary.listIndentureNumber();
		listDescription = listSummary.listDescription();
		listOrderOfPrecedence = listSummary.listOrderOfPrecedence();
		candidatePositions = listSummary.candidatePositionsSummary();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new ListSummary(listId, listIndentureNumber, listDescription, listOrderOfPrecedence, candidatePositions));
	}

	@Test
	@DisplayName("should throw exception when listId is null")
	void shouldThrowExceptionWhenListIdIsNull() {
		assertThrows(NullPointerException.class,
				() -> new ListSummary(null, listIndentureNumber, listDescription, listOrderOfPrecedence, candidatePositions));
	}

	@Test
	@DisplayName("should throw exception when listIndentureNumber is null")
	void shouldThrowExceptionWhenListIndentureNumberIsNull() {
		assertThrows(NullPointerException.class, () -> new ListSummary(listId, null, listDescription, listOrderOfPrecedence, candidatePositions));
	}

	@Test
	@DisplayName("should throw exception when listDescription is null")
	void shouldThrowExceptionWhenListDescriptionIsNull() {
		assertThrows(NullPointerException.class, () -> new ListSummary(listId, listIndentureNumber, null, listOrderOfPrecedence, candidatePositions));
	}

	@Test
	@DisplayName("should throw exception when listOrderOfPrecedence is negative")
	void shouldThrowExceptionWhenListOrderOfPrecedenceIsNegative() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ListSummary(listId, listIndentureNumber, listDescription, -1, candidatePositions));

		final String expected = "The list order of precedence must be positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when candidates is null")
	void shouldThrowExceptionWhenCandidatesIsNull() {
		assertThrows(NullPointerException.class, () -> new ListSummary(listId, listIndentureNumber, listDescription, listOrderOfPrecedence, null));
	}
}
