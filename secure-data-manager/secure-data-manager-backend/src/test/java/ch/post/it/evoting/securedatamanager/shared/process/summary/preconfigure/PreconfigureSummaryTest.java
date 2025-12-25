/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary.preconfigure;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.securedatamanager.shared.process.summary.SummaryGenerator;

class PreconfigureSummaryTest {

	private GqGroup gqGroup;
	private int maximumNumberOfVotingOptions;
	private int maximumNumberOfSelections;
	private int maximumNumberOfWriteInsPlusOne;
	private ImmutableList<VerificationCardSetSummary> verificationCardSets;

	@BeforeEach
	void setUp() {
		final SummaryGenerator summaryGenerator = new SummaryGenerator();
		final PreconfigureSummary preconfigureSummary = summaryGenerator.generatePreconfigureSummary();
		gqGroup = preconfigureSummary.getEncryptionGroup();
		maximumNumberOfVotingOptions = preconfigureSummary.getMaximumNumberOfVotingOptions();
		maximumNumberOfSelections = preconfigureSummary.getMaximumNumberOfSelections();
		maximumNumberOfWriteInsPlusOne = preconfigureSummary.getMaximumNumberOfWriteInsPlusOne();
		verificationCardSets = preconfigureSummary.getVerificationCardSets();
	}

	@Test
	@DisplayName("should create instance with valid inputs")
	void shouldCreateInstanceWithValidInputs() {
		assertDoesNotThrow(() -> new PreconfigureSummary.Builder()
				.withEncryptionGroup(gqGroup)
				.withMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions)
				.withMaximumNumberOfSelections(maximumNumberOfSelections)
				.withMaximumNumberOfWriteInsPlusOne(maximumNumberOfWriteInsPlusOne)
				.withVerificationCardSets(verificationCardSets)
				.build());
	}

	@Test
	@DisplayName("should throw exception when encryptionGroup is null")
	void shouldThrowExceptionWhenEncryptionGroupIsNull() {
		final PreconfigureSummary.Builder builder = new PreconfigureSummary.Builder()
				.withEncryptionGroup(null)
				.withMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions)
				.withMaximumNumberOfSelections(maximumNumberOfSelections)
				.withMaximumNumberOfWriteInsPlusOne(maximumNumberOfWriteInsPlusOne)
				.withVerificationCardSets(verificationCardSets);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("should throw exception when maximumNumberOfVotingOptions is zero")
	void shouldThrowExceptionWhenMaximumNumberOfVotingOptionsIsZero() {
		final PreconfigureSummary.Builder builder = new PreconfigureSummary.Builder()
				.withEncryptionGroup(gqGroup)
				.withMaximumNumberOfVotingOptions(0)
				.withMaximumNumberOfSelections(maximumNumberOfSelections)
				.withMaximumNumberOfWriteInsPlusOne(maximumNumberOfWriteInsPlusOne)
				.withVerificationCardSets(verificationCardSets);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The maximum number of voting options must be greater than 0.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when maximumNumberOfVotingOptions is negative")
	void shouldThrowExceptionWhenMaximumNumberOfVotingOptionsIsNegative() {
		final PreconfigureSummary.Builder builder = new PreconfigureSummary.Builder()
				.withEncryptionGroup(gqGroup)
				.withMaximumNumberOfVotingOptions(-2)
				.withMaximumNumberOfSelections(maximumNumberOfSelections)
				.withMaximumNumberOfWriteInsPlusOne(maximumNumberOfWriteInsPlusOne)
				.withVerificationCardSets(verificationCardSets);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The maximum number of voting options must be greater than 0.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when maximumNumberOfVotingOptions is too big")
	void shouldThrowExceptionWhenMaximumNumberOfVotingOptionsIsTooBig() {
		final PreconfigureSummary.Builder builder = new PreconfigureSummary.Builder()
				.withEncryptionGroup(gqGroup)
				.withMaximumNumberOfVotingOptions(MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS + 1)
				.withMaximumNumberOfSelections(maximumNumberOfSelections)
				.withMaximumNumberOfWriteInsPlusOne(maximumNumberOfWriteInsPlusOne)
				.withVerificationCardSets(verificationCardSets);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The maximum number of voting options must be smaller or equal to the maximum supported number of voting options.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when maximumNumberOfSelections is zero")
	void shouldThrowExceptionWhenMaximumNumberOfSelectionsIsZero() {
		final PreconfigureSummary.Builder builder = new PreconfigureSummary.Builder()
				.withEncryptionGroup(gqGroup)
				.withMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions)
				.withMaximumNumberOfSelections(0)
				.withMaximumNumberOfWriteInsPlusOne(maximumNumberOfWriteInsPlusOne)
				.withVerificationCardSets(verificationCardSets);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The maximum number of selections must be greater than 0.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when maximumNumberOfSelections is negative")
	void shouldThrowExceptionWhenMaximumNumberOfSelectionsIsNegative() {
		final PreconfigureSummary.Builder builder = new PreconfigureSummary.Builder()
				.withEncryptionGroup(gqGroup)
				.withMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions)
				.withMaximumNumberOfSelections(-2)
				.withMaximumNumberOfWriteInsPlusOne(maximumNumberOfWriteInsPlusOne)
				.withVerificationCardSets(verificationCardSets);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The maximum number of selections must be greater than 0.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when maximumNumberOfSelections is too big")
	void shouldThrowExceptionWhenMaximumNumberOfSelectionsIsTooBig() {
		final PreconfigureSummary.Builder builder = new PreconfigureSummary.Builder()
				.withEncryptionGroup(gqGroup)
				.withMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions)
				.withMaximumNumberOfSelections(MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS + 1)
				.withMaximumNumberOfWriteInsPlusOne(maximumNumberOfWriteInsPlusOne)
				.withVerificationCardSets(verificationCardSets);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The maximum number of selections must be smaller or equal to the maximum supported number of selections.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when maximumNumberOfWriteInsPlusOne is zero")
	void shouldThrowExceptionWhenMaximumNumberOfWriteInsPlusOneIsZero() {
		final PreconfigureSummary.Builder builder = new PreconfigureSummary.Builder()
				.withEncryptionGroup(gqGroup)
				.withMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions)
				.withMaximumNumberOfSelections(maximumNumberOfSelections)
				.withMaximumNumberOfWriteInsPlusOne(0)
				.withVerificationCardSets(verificationCardSets);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The maximum number of write-ins + 1 must be greater than 0.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when maximumNumberOfWriteInsPlusOne is negative")
	void shouldThrowExceptionWhenMaximumNumberOfWriteInsPlusOneIsNegative() {
		final PreconfigureSummary.Builder builder = new PreconfigureSummary.Builder()
				.withEncryptionGroup(gqGroup)
				.withMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions)
				.withMaximumNumberOfSelections(maximumNumberOfSelections)
				.withMaximumNumberOfWriteInsPlusOne(-2)
				.withVerificationCardSets(verificationCardSets);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The maximum number of write-ins + 1 must be greater than 0.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when maximumNumberOfWriteInsPlusOne is too big")
	void shouldThrowExceptionWhenMaximumNumberOfWriteInsPlusOneIsTooBig() {
		final PreconfigureSummary.Builder builder = new PreconfigureSummary.Builder()
				.withEncryptionGroup(gqGroup)
				.withMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions)
				.withMaximumNumberOfSelections(maximumNumberOfSelections)
				.withMaximumNumberOfWriteInsPlusOne(MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 2)
				.withVerificationCardSets(verificationCardSets);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expected = "The maximum number of write-ins + 1 must be smaller or equal to the maximum supported number of write-ins + 1.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("should throw exception when verificationCardSets is null")
	void shouldThrowExceptionWhenVerificationCardSetsIsNull() {
		final PreconfigureSummary.Builder builder = new PreconfigureSummary.Builder()
				.withEncryptionGroup(gqGroup)
				.withMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions)
				.withMaximumNumberOfSelections(maximumNumberOfSelections)
				.withMaximumNumberOfWriteInsPlusOne(maximumNumberOfWriteInsPlusOne)
				.withVerificationCardSets(null);
		assertThrows(NullPointerException.class, builder::build);
	}
}