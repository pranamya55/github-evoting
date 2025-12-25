/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary.preconfigure;

import static ch.post.it.evoting.evotinglibraries.domain.validations.GracePeriodValidation.MAXIMUM_GRACE_PERIOD;
import static ch.post.it.evoting.evotinglibraries.domain.validations.GracePeriodValidation.MINIMUM_GRACE_PERIOD;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.securedatamanager.shared.process.summary.SummaryGenerator;

class VerificationCardSetSummaryTest {

	private String verificationCardSetAlias;
	private boolean testBallotBox;
	private int numberOfEligibleVoters;
	private int numberOfVotingOptions;
	private int gracePeriod;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final VerificationCardSetSummary verificationCardSetSummary = summaryGenerator.generateVerificationCardSetSummary();
		verificationCardSetAlias = verificationCardSetSummary.getVerificationCardSetAlias();
		testBallotBox = verificationCardSetSummary.isTestBallotBox();
		numberOfEligibleVoters = verificationCardSetSummary.getNumberOfEligibleVoters();
		numberOfVotingOptions = verificationCardSetSummary.getNumberOfVotingOptions();
		gracePeriod = verificationCardSetSummary.getGracePeriod();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new VerificationCardSetSummary.Builder()
				.setVerificationCardSetAlias(verificationCardSetAlias)
				.setTestBallotBox(testBallotBox)
				.setNumberOfEligibleVoters(numberOfEligibleVoters)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setGracePeriod(gracePeriod)
				.build());
	}

	@Test
	@DisplayName("should throw exception when verificationCardSetAlias is null")
	void shouldThrowExceptionWhenVerificationCardSetAliasIsNull() {
		final VerificationCardSetSummary.Builder builder = new VerificationCardSetSummary.Builder()
				.setVerificationCardSetAlias(null)
				.setTestBallotBox(testBallotBox)
				.setNumberOfEligibleVoters(numberOfEligibleVoters)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setGracePeriod(gracePeriod);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when numberOfEligibleVoters is zero")
	void shouldThrowExceptionWhenNumberOfEligibleVotersIsZero() {
		final VerificationCardSetSummary.Builder builder = new VerificationCardSetSummary.Builder()
				.setVerificationCardSetAlias(verificationCardSetAlias)
				.setTestBallotBox(testBallotBox)
				.setNumberOfEligibleVoters(0)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setGracePeriod(gracePeriod);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The number of eligible voters must be strictly positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when numberOfEligibleVoters is negative")
	void shouldThrowExceptionWhenNumberOfEligibleVotersIsNegative() {
		final VerificationCardSetSummary.Builder builder = new VerificationCardSetSummary.Builder()
				.setVerificationCardSetAlias(verificationCardSetAlias)
				.setTestBallotBox(testBallotBox)
				.setNumberOfEligibleVoters(-2)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setGracePeriod(gracePeriod);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The number of eligible voters must be strictly positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when numberOfVotingOptions is zero")
	void shouldThrowExceptionWhenNumberOfVotingOptionsIsZero() {
		final VerificationCardSetSummary.Builder builder = new VerificationCardSetSummary.Builder()
				.setVerificationCardSetAlias(verificationCardSetAlias)
				.setTestBallotBox(testBallotBox)
				.setNumberOfEligibleVoters(numberOfEligibleVoters)
				.setNumberOfVotingOptions(0)
				.setGracePeriod(gracePeriod);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The number of voting options must be strictly positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when numberOfVotingOptions is negative")
	void shouldThrowExceptionWhenNumberOfVotingOptionsIsNegative() {
		final VerificationCardSetSummary.Builder builder = new VerificationCardSetSummary.Builder()
				.setVerificationCardSetAlias(verificationCardSetAlias)
				.setTestBallotBox(testBallotBox)
				.setNumberOfEligibleVoters(numberOfEligibleVoters)
				.setNumberOfVotingOptions(-1)
				.setGracePeriod(gracePeriod);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The number of voting options must be strictly positive.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when gracePeriod is too small")
	void shouldThrowExceptionWhenGracePeriodIsTooSmall() {
		final VerificationCardSetSummary.Builder builder = new VerificationCardSetSummary.Builder()
				.setVerificationCardSetAlias(verificationCardSetAlias)
				.setTestBallotBox(testBallotBox)
				.setNumberOfEligibleVoters(numberOfEligibleVoters)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setGracePeriod(MINIMUM_GRACE_PERIOD - 1);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = String.format("The grace period must be in the range [%s, %s]. [gracePeriod: %s]", MINIMUM_GRACE_PERIOD,
				MAXIMUM_GRACE_PERIOD, MINIMUM_GRACE_PERIOD - 1);
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when gracePeriod is too big")
	void shouldThrowExceptionWhenGracePeriodIsTooBig() {
		final VerificationCardSetSummary.Builder builder = new VerificationCardSetSummary.Builder()
				.setVerificationCardSetAlias(verificationCardSetAlias)
				.setTestBallotBox(testBallotBox)
				.setNumberOfEligibleVoters(numberOfEligibleVoters)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setGracePeriod(MAXIMUM_GRACE_PERIOD + 1);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = String.format("The grace period must be in the range [%s, %s]. [gracePeriod: %s]", MINIMUM_GRACE_PERIOD,
				MAXIMUM_GRACE_PERIOD, MAXIMUM_GRACE_PERIOD + 1);
		assertEquals(expected, exception.getMessage());
	}
}