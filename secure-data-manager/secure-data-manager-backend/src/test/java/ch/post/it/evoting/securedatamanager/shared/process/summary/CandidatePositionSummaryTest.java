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

class CandidatePositionSummaryTest {

	private String candidateIdentification;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final CandidatePositionSummary candidatePositionSummary = summaryGenerator.generateCandidatePositionSummary();
		candidateIdentification = candidatePositionSummary.candidateIdentification();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new CandidatePositionSummary(candidateIdentification));
	}

	@Test
	@DisplayName("should throw exception when candidateIdentification is null")
	void shouldThrowExceptionWhenCandidateIdentificationIsNull() {
		assertThrows(NullPointerException.class, () -> new CandidatePositionSummary(null));
	}

	@Test
	@DisplayName("should throw exception when candidateIdentification is invalid")
	void shouldThrowExceptionWhenCandidateIdentificationIsInvalid() {
		assertThrows(FailedValidationException.class, () -> new CandidatePositionSummary(" invalid id "));
	}

}
