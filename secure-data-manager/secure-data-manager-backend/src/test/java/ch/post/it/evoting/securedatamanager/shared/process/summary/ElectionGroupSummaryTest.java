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

class ElectionGroupSummaryTest {

	private String electionGroupId;
	private ImmutableList<DescriptionSummary> electionGroupDescription;
	private int electionGroupPosition;
	private ImmutableList<AuthorizationSummary> authorizations;
	private ImmutableList<ElectionSummary> elections;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final ElectionGroupSummary electionGroupSummary = summaryGenerator.generateElectionGroupSummary();
		electionGroupId = electionGroupSummary.electionGroupId();
		electionGroupDescription = electionGroupSummary.electionGroupDescription();
		electionGroupPosition = electionGroupSummary.electionGroupPosition();
		authorizations = electionGroupSummary.authorizations();
		elections = electionGroupSummary.elections();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(
				() -> new ElectionGroupSummary(electionGroupId, electionGroupDescription, electionGroupPosition, authorizations, elections));
	}

	@Test
	@DisplayName("should throw exception when electionGroupId is null")
	void shouldThrowExceptionWhenElectionGroupIdIsNull() {
		assertThrows(NullPointerException.class,
				() -> new ElectionGroupSummary(null, electionGroupDescription, electionGroupPosition, authorizations, elections));
	}

	@Test
	@DisplayName("should throw exception when electionGroupDescription is null")
	void shouldThrowExceptionWhenElectionGroupDescriptionIsNull() {
		assertThrows(NullPointerException.class,
				() -> new ElectionGroupSummary(electionGroupId, null, electionGroupPosition, authorizations, elections));
	}

	@Test
	@DisplayName("should throw exception when electionGroupPosition is negative")
	void shouldThrowExceptionWhenElectionGroupPositionIsNegative() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ElectionGroupSummary(electionGroupId, electionGroupDescription, -3, authorizations, elections));

		final String expected = "The election group position must be strictly positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when authorizations is null")
	void shouldThrowExceptionWhenAuthorizationsIsNull() {
		assertThrows(NullPointerException.class,
				() -> new ElectionGroupSummary(electionGroupId, electionGroupDescription, electionGroupPosition, null, elections));
	}

	@Test
	@DisplayName("should throw exception when elections is null")
	void shouldThrowExceptionWhenElectionsIsNull() {
		assertThrows(NullPointerException.class,
				() -> new ElectionGroupSummary(electionGroupId, electionGroupDescription, electionGroupPosition, authorizations, null));
	}
}
