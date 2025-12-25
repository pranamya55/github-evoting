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

class ElectionSummaryTest {

	private String electionId;
	private int electionPosition;
	private int electionType;
	private int primarySecondaryType;
	private ImmutableList<DescriptionSummary> electionDescription;
	private int numberOfMandates;
	private boolean writeInsAllowed;
	private int candidateAccumulation;
	private ImmutableList<CandidateSummary> candidates;
	private ImmutableList<ListSummary> lists;
	private ImmutableList<ListUnionSummary> listUnions;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final ElectionSummary electionSummary = summaryGenerator.generateElectionSummary();
		electionId = electionSummary.getElectionId();
		electionPosition = electionSummary.getElectionPosition();
		electionType = electionSummary.getElectionType();
		primarySecondaryType = electionSummary.getPrimarySecondaryType();
		electionDescription = electionSummary.getElectionDescription();
		numberOfMandates = electionSummary.getNumberOfMandates();
		writeInsAllowed = electionSummary.isWriteInsAllowed();
		candidateAccumulation = electionSummary.getCandidateAccumulation();
		candidates = electionSummary.getCandidates();
		lists = electionSummary.getLists();
		listUnions = electionSummary.getListUnions();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new ElectionSummary.Builder()
				.electionId(electionId)
				.electionPosition(electionPosition)
				.electionType(electionType)
				.primarySecondaryType(primarySecondaryType)
				.electionDescription(electionDescription)
				.numberOfMandates(numberOfMandates)
				.writeInsAllowed(writeInsAllowed)
				.candidateAccumulation(candidateAccumulation)
				.candidates(candidates)
				.lists(lists)
				.listUnions(listUnions)
				.build());
	}

	@Test
	@DisplayName("should throw exception when electionId is null")
	void shouldThrowExceptionWhenElectionIdIsNull() {
		final ElectionSummary.Builder builder = new ElectionSummary.Builder()
				.electionId(null)
				.electionPosition(electionPosition)
				.electionType(electionType)
				.primarySecondaryType(primarySecondaryType)
				.electionDescription(electionDescription)
				.numberOfMandates(numberOfMandates)
				.writeInsAllowed(writeInsAllowed)
				.candidateAccumulation(candidateAccumulation)
				.candidates(candidates)
				.lists(lists);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when electionPosition is negative")
	void shouldThrowExceptionWhenElectionPositionIsNegative() {
		final ElectionSummary.Builder builder = new ElectionSummary.Builder()
				.electionId(electionId)
				.electionPosition(-2)
				.electionType(electionType)
				.primarySecondaryType(primarySecondaryType)
				.electionDescription(electionDescription)
				.numberOfMandates(numberOfMandates)
				.writeInsAllowed(writeInsAllowed)
				.candidateAccumulation(candidateAccumulation)
				.candidates(candidates)
				.lists(lists);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The election position must be positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when electionType is negative")
	void shouldThrowExceptionWhenElectionTypeIsNegative() {
		final ElectionSummary.Builder builder = new ElectionSummary.Builder()
				.electionId(electionId)
				.electionPosition(electionPosition)
				.electionType(-2)
				.primarySecondaryType(primarySecondaryType)
				.electionDescription(electionDescription)
				.numberOfMandates(numberOfMandates)
				.writeInsAllowed(writeInsAllowed)
				.candidateAccumulation(candidateAccumulation)
				.candidates(candidates)
				.lists(lists);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The election type must be positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when primarySecondaryType is out of range")
	void shouldThrowExceptionWhenPrimarySecondaryTypeIsOutOfRange() {
		final ElectionSummary.Builder builder = new ElectionSummary.Builder()
				.electionId(electionId)
				.electionPosition(electionPosition)
				.electionType(electionType)
				.primarySecondaryType(4)
				.electionDescription(electionDescription)
				.numberOfMandates(numberOfMandates)
				.writeInsAllowed(writeInsAllowed)
				.candidateAccumulation(candidateAccumulation)
				.candidates(candidates)
				.lists(lists);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The primary secondary type must be equal to 0, 1 or 2.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when electionDescription is null")
	void shouldThrowExceptionWhenElectionDescriptionIsNull() {
		final ElectionSummary.Builder builder = new ElectionSummary.Builder()
				.electionId(electionId)
				.electionPosition(electionPosition)
				.electionType(electionType)
				.primarySecondaryType(primarySecondaryType)
				.electionDescription(null)
				.numberOfMandates(numberOfMandates)
				.writeInsAllowed(writeInsAllowed)
				.candidateAccumulation(candidateAccumulation)
				.candidates(candidates)
				.lists(lists);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when numberOfMandates is zero")
	void shouldThrowExceptionWhenNumberOfMandatesIsZero() {
		final ElectionSummary.Builder builder = new ElectionSummary.Builder()
				.electionId(electionId)
				.electionPosition(electionPosition)
				.electionType(electionType)
				.primarySecondaryType(primarySecondaryType)
				.electionDescription(electionDescription)
				.numberOfMandates(0)
				.writeInsAllowed(writeInsAllowed)
				.candidateAccumulation(candidateAccumulation)
				.candidates(candidates)
				.lists(lists);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The number of mandates must be strictly positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when numberOfMandates is negative")
	void shouldThrowExceptionWhenNumberOfMandatesIsNegative() {
		final ElectionSummary.Builder builder = new ElectionSummary.Builder()
				.electionId(electionId)
				.electionPosition(electionPosition)
				.electionType(electionType)
				.primarySecondaryType(primarySecondaryType)
				.electionDescription(electionDescription)
				.numberOfMandates(-50)
				.writeInsAllowed(writeInsAllowed)
				.candidateAccumulation(candidateAccumulation)
				.candidates(candidates)
				.lists(lists);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The number of mandates must be strictly positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when candidateAccumulation is negative")
	void shouldThrowExceptionWhenCandidateAccumulationIsNegative() {
		final ElectionSummary.Builder builder = new ElectionSummary.Builder()
				.electionId(electionId)
				.electionPosition(electionPosition)
				.electionType(electionType)
				.primarySecondaryType(primarySecondaryType)
				.electionDescription(electionDescription)
				.numberOfMandates(numberOfMandates)
				.writeInsAllowed(writeInsAllowed)
				.candidateAccumulation(-50)
				.candidates(candidates)
				.lists(lists);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The candidate accumulation must be positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when candidates is null")
	void shouldThrowExceptionWhenCandidatesIsNull() {
		final ElectionSummary.Builder builder = new ElectionSummary.Builder()
				.electionId(electionId)
				.electionPosition(electionPosition)
				.electionType(electionType)
				.primarySecondaryType(primarySecondaryType)
				.electionDescription(electionDescription)
				.numberOfMandates(numberOfMandates)
				.writeInsAllowed(writeInsAllowed)
				.candidateAccumulation(candidateAccumulation)
				.candidates(null)
				.lists(lists);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when lists is null")
	void shouldThrowExceptionWhenListsIsNull() {
		final ElectionSummary.Builder builder = new ElectionSummary.Builder()
				.electionId(electionId)
				.electionPosition(electionPosition)
				.electionType(electionType)
				.primarySecondaryType(primarySecondaryType)
				.electionDescription(electionDescription)
				.numberOfMandates(numberOfMandates)
				.writeInsAllowed(writeInsAllowed)
				.candidateAccumulation(candidateAccumulation)
				.candidates(candidates)
				.lists(null);
		assertThrows(NullPointerException.class, builder::build);
	}
}
