/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

class AuthorizationSummaryTest {

	private String authorizationId;
	private String authorizationName;
	private boolean isTest;
	private LocalDateTime fromDate;
	private LocalDateTime toDate;
	private long numberOfVoters;
	private ImmutableList<AuthorizationObjectSummary> authorizationObjects;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final AuthorizationSummary authorizationSummary = summaryGenerator.generateAuthorizationSummary();
		authorizationId = authorizationSummary.getAuthorizationId();
		authorizationName = authorizationSummary.getAuthorizationName();
		isTest = authorizationSummary.isTest();
		fromDate = authorizationSummary.getFromDate();
		toDate = authorizationSummary.getToDate();
		numberOfVoters = authorizationSummary.getNumberOfVoters();
		authorizationObjects = authorizationSummary.getAuthorizationObjects();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new AuthorizationSummary.Builder()
				.authorizationId(authorizationId)
				.authorizationName(authorizationName)
				.isTest(isTest)
				.fromDate(fromDate)
				.toDate(toDate)
				.numberOfVoters(numberOfVoters)
				.authorizationObjects(authorizationObjects)
				.build());
	}

	@Test
	@DisplayName("should throw exception when authorizationId is null")
	void shouldThrowExceptionWhenAuthorizationIdIsNull() {
		final AuthorizationSummary.Builder builder = new AuthorizationSummary.Builder()
				.authorizationId(null)
				.authorizationName(authorizationName)
				.isTest(isTest)
				.fromDate(fromDate)
				.toDate(toDate)
				.numberOfVoters(numberOfVoters)
				.authorizationObjects(authorizationObjects);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when authorizationName is null")
	void shouldThrowExceptionWhenAuthorizationNameIsNull() {
		final AuthorizationSummary.Builder builder = new AuthorizationSummary.Builder()
				.authorizationId(authorizationId)
				.authorizationName(null)
				.isTest(isTest)
				.fromDate(fromDate)
				.toDate(toDate)
				.numberOfVoters(numberOfVoters)
				.authorizationObjects(authorizationObjects);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when fromDate is null")
	void shouldThrowExceptionWhenFromDateIsNull() {
		final AuthorizationSummary.Builder builder = new AuthorizationSummary.Builder()
				.authorizationId(authorizationId)
				.authorizationName(authorizationName)
				.isTest(isTest)
				.fromDate(null)
				.toDate(toDate)
				.numberOfVoters(numberOfVoters)
				.authorizationObjects(authorizationObjects);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when toDate is null")
	void shouldThrowExceptionWhenToDateIsNull() {
		final AuthorizationSummary.Builder builder = new AuthorizationSummary.Builder()
				.authorizationId(authorizationId)
				.authorizationName(authorizationName)
				.isTest(isTest)
				.fromDate(fromDate)
				.toDate(null)
				.numberOfVoters(numberOfVoters)
				.authorizationObjects(authorizationObjects);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when fromDate is after toDate")
	void shouldThrowExceptionWhenFromDateIsAfterToDate() {
		final AuthorizationSummary.Builder builder = new AuthorizationSummary.Builder()
				.authorizationId(authorizationId)
				.authorizationName(authorizationName)
				.isTest(isTest)
				.fromDate(fromDate)
				.toDate(fromDate.minusSeconds(30))
				.numberOfVoters(numberOfVoters)
				.authorizationObjects(authorizationObjects);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The fromDate must not be after the toDate.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when numberOfVoters is zero")
	void shouldThrowExceptionWhenNumberOfVotersIsZero() {
		final AuthorizationSummary.Builder builder = new AuthorizationSummary.Builder()
				.authorizationId(authorizationId)
				.authorizationName(authorizationName)
				.isTest(isTest)
				.fromDate(fromDate)
				.toDate(toDate)
				.numberOfVoters(0)
				.authorizationObjects(authorizationObjects);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The number of voters must be strictly positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when numberOfVoters is negative")
	void shouldThrowExceptionWhenNumberOfVotersIsNegative() {
		final AuthorizationSummary.Builder builder = new AuthorizationSummary.Builder()
				.authorizationId(authorizationId)
				.authorizationName(authorizationName)
				.isTest(isTest)
				.fromDate(fromDate)
				.toDate(toDate)
				.numberOfVoters(-1)
				.authorizationObjects(authorizationObjects);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The number of voters must be strictly positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when authorizationObjects is null")
	void shouldThrowExceptionWhenAuthorizationObjectsIsNull() {
		final AuthorizationSummary.Builder builder = new AuthorizationSummary.Builder()
				.authorizationId(authorizationId)
				.authorizationName(authorizationName)
				.isTest(isTest)
				.fromDate(fromDate)
				.toDate(toDate)
				.numberOfVoters(numberOfVoters)
				.authorizationObjects(null);
		assertThrows(NullPointerException.class, builder::build);
	}
}
