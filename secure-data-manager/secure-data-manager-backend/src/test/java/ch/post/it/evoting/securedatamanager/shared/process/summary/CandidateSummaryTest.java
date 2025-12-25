/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CandidateSummaryTest {

	private String candidateId;
	private String familyName;
	private String firstName;
	private String callName;
	private LocalDate dateOfBirth;
	private boolean isIncumbent;
	private String referenceOnPosition;
	private String eligibility;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final CandidateSummary candidateSummary = summaryGenerator.generateCandidateSummary();
		candidateId = candidateSummary.getCandidateId();
		familyName = candidateSummary.getFamilyName();
		firstName = candidateSummary.getFirstName();
		callName = candidateSummary.getCallName();
		dateOfBirth = candidateSummary.getDateOfBirth();
		isIncumbent = candidateSummary.isIncumbent();
		referenceOnPosition = candidateSummary.getReferenceOnPosition();
		eligibility = candidateSummary.getEligibility();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new CandidateSummary.Builder()
				.candidateId(candidateId)
				.familyName(familyName)
				.firstName(firstName)
				.callName(callName)
				.dateOfBirth(dateOfBirth)
				.isIncumbent(isIncumbent)
				.referenceOnPosition(referenceOnPosition)
				.eligibility(eligibility)
				.build());
	}

	@Test
	@DisplayName("should throw exception when candidateId is null")
	void shouldThrowExceptionWhenCandidateIdIsNull() {
		final CandidateSummary.Builder builder = new CandidateSummary.Builder()
				.candidateId(null)
				.familyName(familyName)
				.firstName(firstName)
				.callName(callName)
				.dateOfBirth(dateOfBirth)
				.isIncumbent(isIncumbent)
				.referenceOnPosition(referenceOnPosition)
				.eligibility(eligibility);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when familyName is null")
	void shouldThrowExceptionWhenFamilyNameIsNull() {
		final CandidateSummary.Builder builder = new CandidateSummary.Builder()
				.candidateId(candidateId)
				.familyName(null)
				.firstName(firstName)
				.callName(callName)
				.dateOfBirth(dateOfBirth)
				.isIncumbent(isIncumbent)
				.referenceOnPosition(referenceOnPosition)
				.eligibility(eligibility);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when firstName is null")
	void shouldThrowExceptionWhenFirstNameIsNull() {
		final CandidateSummary.Builder builder = new CandidateSummary.Builder()
				.candidateId(candidateId)
				.familyName(familyName)
				.firstName(null)
				.callName(callName)
				.dateOfBirth(dateOfBirth)
				.isIncumbent(isIncumbent)
				.referenceOnPosition(referenceOnPosition)
				.eligibility(eligibility);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when callName is null")
	void shouldThrowExceptionWhenCallNameIsNull() {
		final CandidateSummary.Builder builder = new CandidateSummary.Builder()
				.candidateId(candidateId)
				.familyName(familyName)
				.firstName(firstName)
				.callName(null)
				.dateOfBirth(dateOfBirth)
				.isIncumbent(isIncumbent)
				.referenceOnPosition(referenceOnPosition)
				.eligibility(eligibility);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when dateOfBirth is null")
	void shouldThrowExceptionWhenDateOfBirthIsNull() {
		final CandidateSummary.Builder builder = new CandidateSummary.Builder()
				.candidateId(candidateId)
				.familyName(familyName)
				.firstName(firstName)
				.callName(callName)
				.dateOfBirth(null)
				.isIncumbent(isIncumbent)
				.referenceOnPosition(referenceOnPosition)
				.eligibility(eligibility);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when referenceOnPosition is null")
	void shouldThrowExceptionWhenReferenceOnPositionIsNull() {
		final CandidateSummary.Builder builder = new CandidateSummary.Builder()
				.candidateId(candidateId)
				.familyName(familyName)
				.firstName(firstName)
				.callName(callName)
				.dateOfBirth(dateOfBirth)
				.isIncumbent(isIncumbent)
				.referenceOnPosition(null)
				.eligibility(eligibility);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when eligibility is null")
	void shouldThrowExceptionWhenEligibilityIsNull() {
		final CandidateSummary.Builder builder = new CandidateSummary.Builder()
				.candidateId(candidateId)
				.familyName(familyName)
				.firstName(firstName)
				.callName(callName)
				.dateOfBirth(dateOfBirth)
				.isIncumbent(isIncumbent)
				.referenceOnPosition(referenceOnPosition)
				.eligibility(null);
		assertThrows(NullPointerException.class, builder::build);
	}
}
