/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class ListUnionSummaryTest {

	private String listUnionIdentification;
	private ImmutableList<ListUnionDescriptionInfo> listUnionDescriptionInfos;
	private int listUnionType;
	private ImmutableList<String> referencedLists;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final ListUnionSummary listUnionSummary = summaryGenerator.generateListUnionSummary();

		listUnionIdentification = listUnionSummary.listUnionIdentification();
		listUnionDescriptionInfos = listUnionSummary.listUnionDescription();
		listUnionType = listUnionSummary.listUnionType();
		referencedLists = listUnionSummary.referencedLists();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new ListUnionSummary(listUnionIdentification, listUnionDescriptionInfos, listUnionType, referencedLists));
	}

	@Test
	@DisplayName("should throw exception when any parameter is null")
	void shouldThrowExceptionWhenAnyParameterIsNull() {
		assertThrows(NullPointerException.class,
				() -> new ListUnionSummary(null, listUnionDescriptionInfos, listUnionType, referencedLists));
		assertThrows(NullPointerException.class,
				() -> new ListUnionSummary(listUnionIdentification, null, listUnionType, referencedLists));
		assertThrows(NullPointerException.class,
				() -> new ListUnionSummary(listUnionIdentification, listUnionDescriptionInfos, listUnionType, null));
	}

	@Test
	@DisplayName("should throw exception when identification is invalid")
	void shouldThrowExceptionWhenIdentificationIsNull() {
		assertThrows(FailedValidationException.class,
				() -> new ListUnionSummary(" invalid id ", listUnionDescriptionInfos, listUnionType, referencedLists));
	}

	@Test
	@DisplayName("should throw exception when type is invalid")
	void shouldThrowExceptionWhenTypeIsNull() {
		assertThrows(IllegalArgumentException.class,
				() -> new ListUnionSummary(listUnionIdentification, listUnionDescriptionInfos, -1, referencedLists));
	}

	@Test
	@DisplayName("should throw exception when referenced lists is empty")
	void shouldThrowExceptionWhenReferencedListsIsNull() {
		assertThrows(IllegalArgumentException.class, this::createListUnionSummaryWithEmptyList);
	}

	private void createListUnionSummaryWithEmptyList() {
		new ListUnionSummary(
				listUnionIdentification,
				listUnionDescriptionInfos,
				listUnionType,
				ImmutableList.emptyList()
		);
	}

}
