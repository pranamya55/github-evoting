/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.ID_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base16Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("CombineEncLongCodeSharesContext with")
class CombineEncLongCodeSharesContextTest {

	private final Random random = RandomFactory.createRandom();
	private final Alphabet base16Alphabet = Base16Alphabet.getInstance();

	private GqGroup encryptionGroup;
	private String electionEventId;
	private String verificationCardSetId;
	private ImmutableList<String> verificationCardIds;
	private int numberOfVotingOptions;
	private int maximumNumberOfVotingOptions;

	@BeforeEach
	void setUp() {
		encryptionGroup = GroupTestData.getGqGroup();

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();

		final int numberOfEligibleVoters = random.genRandomInteger(10) + 1; // must be > 0.
		verificationCardIds = Stream.generate(uuidGenerator::generate)
				.limit(numberOfEligibleVoters)
				.collect(toImmutableList());

		maximumNumberOfVotingOptions = random.genRandomInteger(MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS) + 1; // must be in range [1, n_sup].
		numberOfVotingOptions = random.genRandomInteger(maximumNumberOfVotingOptions) + 1; // must be in range [1, n_max]
	}

	@Test
	@DisplayName("The encryptionParameters null")
	void contextWithNullParameters() {
		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(null)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(verificationCardIds)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("The Election Event Id null")
	void contextWithNullElectionEventId() {
		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(null)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(verificationCardIds)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("The Verification Card Set Id null")
	void contextWithNullVerificationCardSetId() {
		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(null)
				.setVerificationCardIds(verificationCardIds)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("The Verification Card Ids null")
	void contextWithNullVerificationCardIds() {
		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(null)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("The Election Event Id invalid UUID")
	void contextWithInvalidElectionEventId() {
		final String invalidElectionEventId = "zdiauzdi134";

		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(invalidElectionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(verificationCardIds)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions);

		final FailedValidationException exception = assertThrows(FailedValidationException.class, builder::build);

		final String expectedMessage = String.format(
				"The given string does not comply with the required format. [string: %s, format: ^[0-9A-F]{32}$].",
				invalidElectionEventId);
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("The Verification Card Set Id invalid UUID")
	void contextWithInvalidVerificationCardSetId() {
		final String invalidVerificationCardSetId = "zdiauzdi134";

		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(invalidVerificationCardSetId)
				.setVerificationCardIds(verificationCardIds)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions);

		final FailedValidationException exception = assertThrows(FailedValidationException.class, builder::build);

		final String expectedMessage = String.format(
				"The given string does not comply with the required format. [string: %s, format: ^[0-9A-F]{32}$].",
				invalidVerificationCardSetId);
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("The number of voting options is negative")
	void contextWithNegativeNumberOfVotingOptions() {
		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(verificationCardIds)
				.setNumberOfVotingOptions(-50)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = "The number of voting options must be strictly positive.";
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("The number of voting options is zero")
	void contextWithZeroNumberOfVotingOptions() {
		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(verificationCardIds)
				.setNumberOfVotingOptions(0)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = "The number of voting options must be strictly positive.";
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("The number of voting options is strictly greater than the maximum number of voting options")
	void contextWithTooBigNumberOfVotingOptions() {
		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(verificationCardIds)
				.setNumberOfVotingOptions(maximumNumberOfVotingOptions + 1)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = String.format(
				"The number of voting options must be smaller or equal to the maximum number of voting options. [n: %s, n_max: %s]",
				maximumNumberOfVotingOptions + 1, maximumNumberOfVotingOptions);
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("The maximum number of voting options is negative")
	void contextWithNegativeMaximumNumberOfVotingOptions() {
		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(verificationCardIds)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setMaximumNumberOfVotingOptions(-50);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = "The maximum number of voting options must be strictly positive.";
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("The maximum number of voting options is zero")
	void contextWithZeroMaximumNumberOfVotingOptions() {
		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(verificationCardIds)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setMaximumNumberOfVotingOptions(0);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = "The maximum number of voting options must be strictly positive.";
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("The maximum number of voting options is strictly greater than the maximum supported number of voting options")
	void contextWithTooBigMaximumNumberOfVotingOptions() {
		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(verificationCardIds)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setMaximumNumberOfVotingOptions(MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS + 1);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = String.format(
				"The maximum number of voting options must be smaller or equal to the maximum supported number of voting options. [n_max: %s, n_sup: %s]",
				MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS + 1, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("Constructor with Vector of Verification Card Ids of invalid UUID format")
	void contextWithInvalidVerificationCardIds() {
		final String verificationCardId_invalid = "F51188102C2421385zS";
		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(ImmutableList.of(verificationCardId_invalid))
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions);

		final FailedValidationException exception = assertThrows(FailedValidationException.class, builder::build);

		final String expectedMessage = String.format(
				"The given string does not comply with the required format. [string: %s, format: ^[0-9A-F]{32}$].",
				verificationCardId_invalid);
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("Constructor with Vector of Verification Card Ids with duplicates")
	void contextWithDuplicatesInVerificationCardIds() {
		final ImmutableList<String> duplicateVerificationCardIds = ImmutableList.of(verificationCardIds.get(0), verificationCardIds.get(0));
		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(duplicateVerificationCardIds)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = "The verification card id list must not have duplicates.";
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("Constructor vector of Verification Card Ids with the padding character =")
	void contextWithPaddingStartVerificationCardIds() {
		final ImmutableList<String> verificationCardIdsElementWithPadding = Stream.generate(
						() -> random.genRandomString(ID_LENGTH - 1, base16Alphabet) + "=")
				.limit(verificationCardIds.size())
				.collect(toImmutableList());

		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(verificationCardIdsElementWithPadding)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions);

		final FailedValidationException ex = assertThrows(FailedValidationException.class, builder::build);

		final String expectedMessage = "The given string does not comply with the required format.";

		assertTrue(ex.getMessage().startsWith(expectedMessage));
	}

	@Test
	@DisplayName("Constructor vector of Verification Card Ids empty")
	void contextWithEmptyVerificationCardIds() {
		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(ImmutableList.of())
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions);

		final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = "The vector of verification card ids must have at least one element.";

		assertTrue(ex.getMessage().startsWith(expectedMessage));
	}

	@Test
	@DisplayName("Constructor valid arguments")
	void contextWithValidArguments() {
		final CombineEncLongCodeSharesContext.Builder builder = new CombineEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(verificationCardIds)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions);

		assertDoesNotThrow(builder::build);
	}
}