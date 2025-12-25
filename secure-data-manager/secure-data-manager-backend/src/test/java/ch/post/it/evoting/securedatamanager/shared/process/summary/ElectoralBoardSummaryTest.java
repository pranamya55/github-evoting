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

class ElectoralBoardSummaryTest {

	private String electoralBoardId;
	private String electoralBoardName;
	private String electoralBoardDescription;
	private ImmutableList<String> members;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final ElectoralBoardSummary electoralBoardSummary = summaryGenerator.generateElectoralBoardSummary();
		electoralBoardId = electoralBoardSummary.getElectoralBoardId();
		electoralBoardName = electoralBoardSummary.getElectoralBoardName();
		electoralBoardDescription = electoralBoardSummary.getElectoralBoardDescription();
		members = electoralBoardSummary.getMembers();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new ElectoralBoardSummary.Builder()
				.electoralBoardId(electoralBoardId)
				.electoralBoardName(electoralBoardName)
				.electoralBoardDescription(electoralBoardDescription)
				.members(members)
				.build());
	}

	@Test
	@DisplayName("should throw exception when electoralBoardId is null")
	void shouldThrowExceptionWhenElectoralBoardIdIsNull() {
		final ElectoralBoardSummary.Builder builder = new ElectoralBoardSummary.Builder()
				.electoralBoardId(null)
				.electoralBoardName(electoralBoardName)
				.electoralBoardDescription(electoralBoardDescription)
				.members(members);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when electoralBoardName is null")
	void shouldThrowExceptionWhenElectoralBoardNameIsNull() {
		final ElectoralBoardSummary.Builder builder = new ElectoralBoardSummary.Builder()
				.electoralBoardId(electoralBoardId)
				.electoralBoardName(null)
				.electoralBoardDescription(electoralBoardDescription)
				.members(members);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when electoralBoardDescription is null")
	void shouldThrowExceptionWhenElectoralBoardDescriptionIsNull() {
		final ElectoralBoardSummary.Builder builder = new ElectoralBoardSummary.Builder()
				.electoralBoardId(electoralBoardId)
				.electoralBoardName(electoralBoardName)
				.electoralBoardDescription(null)
				.members(members);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when members is null")
	void shouldThrowExceptionWhenMembersIsNull() {
		final ElectoralBoardSummary.Builder builder = new ElectoralBoardSummary.Builder()
				.electoralBoardId(electoralBoardId)
				.electoralBoardName(electoralBoardName)
				.electoralBoardDescription(electoralBoardDescription)
				.members(null);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when members is empty")
	void shouldThrowExceptionWhenMembersIsEmpty() {
		final ElectoralBoardSummary.Builder builder = new ElectoralBoardSummary.Builder()
				.electoralBoardId(electoralBoardId)
				.electoralBoardName(electoralBoardName)
				.electoralBoardDescription(electoralBoardDescription)
				.members(ImmutableList.of());
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The electoral board summary members must not be empty.";
		assertEquals(expected, exception.getMessage());
	}
}