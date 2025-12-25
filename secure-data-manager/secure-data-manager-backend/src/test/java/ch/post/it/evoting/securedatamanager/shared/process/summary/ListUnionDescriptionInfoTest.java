/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class ListUnionDescriptionInfoTest {

	private String language;
	private String listUnionDescription;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final ListUnionSummary listUnionSummary = summaryGenerator.generateListUnionSummary();
		final ListUnionDescriptionInfo listUnionDescriptionInfo = listUnionSummary.listUnionDescription().getFirst();

		language = listUnionDescriptionInfo.language();
		listUnionDescription = listUnionDescriptionInfo.listUnionDescription();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new ListUnionDescriptionInfo(language, listUnionDescription));
	}

	@Test
	@DisplayName("should throw exception when any parameter is null")
	void shouldThrowExceptionWhenAnyParameterIsNull() {
		assertThrows(NullPointerException.class, () -> new ListUnionDescriptionInfo(null, listUnionDescription));
		assertThrows(NullPointerException.class, () -> new ListUnionDescriptionInfo(language, null));
	}

	@Test
	@DisplayName("should throw exception when any parameter is blank")
	void shouldThrowExceptionWhenAnyParameterIsBlank() {
		assertThrows(IllegalArgumentException.class, () -> new ListUnionDescriptionInfo("", listUnionDescription));
		assertThrows(IllegalArgumentException.class, () -> new ListUnionDescriptionInfo(language, ""));
	}

	@Test
	@DisplayName("should throw exception when any parameter is invalid")
	void shouldThrowExceptionWhenAnyParameterIsInvalid() {
		assertThrows(FailedValidationException.class, () -> new ListUnionDescriptionInfo("\uD800", listUnionDescription));
		assertThrows(FailedValidationException.class, () -> new ListUnionDescriptionInfo(language, "\uD800"));
	}

}
