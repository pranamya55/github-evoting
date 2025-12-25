/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthorizationObjectSummaryTest {

	private String domainOfInfluenceId;
	private String domainOfInfluenceType;
	private String domainOfInfluenceName;
	private String countingCircleId;
	private String countingCircleName;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final AuthorizationObjectSummary authorizationObjectSummary = summaryGenerator.generateAuthorizationObjectSummary();
		domainOfInfluenceId = authorizationObjectSummary.getDomainOfInfluenceId();
		domainOfInfluenceType = authorizationObjectSummary.getDomainOfInfluenceType();
		domainOfInfluenceName = authorizationObjectSummary.getDomainOfInfluenceName();
		countingCircleId = authorizationObjectSummary.getCountingCircleId();
		countingCircleName = authorizationObjectSummary.getCountingCircleName();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new AuthorizationObjectSummary.Builder()
				.domainOfInfluenceId(domainOfInfluenceId)
				.domainOfInfluenceType(domainOfInfluenceType)
				.domainOfInfluenceName(domainOfInfluenceName)
				.countingCircleId(countingCircleId)
				.countingCircleName(countingCircleName)
				.build());
	}

	@Test
	@DisplayName("should throw exception when domainOfInfluenceId is null")
	void shouldThrowExceptionWhenDomainOfInfluenceIdIsNull() {
		final AuthorizationObjectSummary.Builder builder = new AuthorizationObjectSummary.Builder()
				.domainOfInfluenceId(null)
				.domainOfInfluenceType(domainOfInfluenceType)
				.domainOfInfluenceName(domainOfInfluenceName)
				.countingCircleId(countingCircleId)
				.countingCircleName(countingCircleName);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when domainOfInfluenceType is null")
	void shouldThrowExceptionWhenDomainOfInfluenceTypeIsNull() {
		final AuthorizationObjectSummary.Builder builder = new AuthorizationObjectSummary.Builder()
				.domainOfInfluenceId(domainOfInfluenceId)
				.domainOfInfluenceType(null)
				.domainOfInfluenceName(domainOfInfluenceName)
				.countingCircleId(countingCircleId)
				.countingCircleName(countingCircleName);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when domainOfInfluenceName is null")
	void shouldThrowExceptionWhenDomainOfInfluenceNameIsNull() {
		final AuthorizationObjectSummary.Builder builder = new AuthorizationObjectSummary.Builder()
				.domainOfInfluenceId(domainOfInfluenceId)
				.domainOfInfluenceType(domainOfInfluenceType)
				.domainOfInfluenceName(null)
				.countingCircleId(countingCircleId)
				.countingCircleName(countingCircleName);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when countingCircleId is null")
	void shouldThrowExceptionWhenCountingCircleIdIsNull() {
		final AuthorizationObjectSummary.Builder builder = new AuthorizationObjectSummary.Builder()
				.domainOfInfluenceId(domainOfInfluenceId)
				.domainOfInfluenceType(domainOfInfluenceType)
				.domainOfInfluenceName(domainOfInfluenceName)
				.countingCircleId(null)
				.countingCircleName(countingCircleName);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when countingCircleName is null")
	void shouldThrowExceptionWhenCountingCircleNameIsNull() {
		final AuthorizationObjectSummary.Builder builder = new AuthorizationObjectSummary.Builder()
				.domainOfInfluenceId(domainOfInfluenceId)
				.domainOfInfluenceType(domainOfInfluenceType)
				.domainOfInfluenceName(domainOfInfluenceName)
				.countingCircleId(countingCircleId)
				.countingCircleName(null);
		assertThrows(NullPointerException.class, builder::build);
	}
}