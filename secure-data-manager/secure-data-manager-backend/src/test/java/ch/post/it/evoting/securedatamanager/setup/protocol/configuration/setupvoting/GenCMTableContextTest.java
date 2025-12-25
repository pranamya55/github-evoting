/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("GenCMTableContest built with")
class GenCMTableContextTest extends TestGroupSetup {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final int MAXIMUM_NUMBER_OF_VOTING_OPTIONS = 10;

	private static String electionEventId;
	private static ImmutableList<String> correctnessInformation;
	private static ImmutableList<String> verificationCardIds;

	@BeforeEach
	void setUp() {
		initializeParameters();
	}

	@ParameterizedTest
	@MethodSource("provideNullInputsForGenCMTableContext")
	@DisplayName("any null parameter throws NullPointerException")
	void anyNullParamThrows(final GqGroup encryptionGroup, final String electionEventId, final ImmutableList<String> verificationCardIds,
			final ImmutableList<String> correctnessInformation) {

		final GenCMTableContext.Builder builder = new GenCMTableContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardIds(verificationCardIds)
				.setMaximumNumberOfVotingOptions(MAXIMUM_NUMBER_OF_VOTING_OPTIONS)
				.setCorrectnessInformation(correctnessInformation);

		assertThrows(NullPointerException.class, builder::build);
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUUIDInputsForGenCMTableContext")
	@DisplayName("with invalid UUID throws FailedValidationException")
	void invalidUUIDThrows(final GqGroup encryptionGroup, final String electionEventId, final ImmutableList<String> verificationCardIds,
			final ImmutableList<String> correctnessInformation) {

		final GenCMTableContext.Builder builder = new GenCMTableContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardIds(verificationCardIds)
				.setMaximumNumberOfVotingOptions(MAXIMUM_NUMBER_OF_VOTING_OPTIONS)
				.setCorrectnessInformation(correctnessInformation);

		assertThrows(FailedValidationException.class, builder::build);
	}

	@Test
	@DisplayName("correctness information not in range throws IllegalArgumentException")
	void notInRangeCorrectnessInformationThrows() {
		// Too few.
		final GenCMTableContext.Builder tooFewBuilder = new GenCMTableContext.Builder()
				.setEncryptionGroup(gqGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardIds(verificationCardIds)
				.setMaximumNumberOfVotingOptions(MAXIMUM_NUMBER_OF_VOTING_OPTIONS)
				.setCorrectnessInformation(ImmutableList.emptyList());

		final IllegalArgumentException tooFewException = assertThrows(IllegalArgumentException.class, tooFewBuilder::build);
		assertEquals("The correctness information must not be empty.", Throwables.getRootCause(tooFewException).getMessage());

		// Too many.
		final ImmutableList<String> tooManyCorrectnessInformation = IntStream.range(0, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS + 1)
				.mapToObj(i -> uuidGenerator.generate())
				.collect(toImmutableList());
		final GenCMTableContext.Builder tooManyBuilder = new GenCMTableContext.Builder()
				.setEncryptionGroup(gqGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardIds(verificationCardIds)
				.setMaximumNumberOfVotingOptions(MAXIMUM_NUMBER_OF_VOTING_OPTIONS)
				.setCorrectnessInformation(tooManyCorrectnessInformation);

		final IllegalArgumentException tooManyException = assertThrows(IllegalArgumentException.class, tooManyBuilder::build);
		assertEquals(String.format(
						"The correctness information must be smaller or equal to the maximum supported number of voting options. [n: %s, n_sup: %s]",
						tooManyCorrectnessInformation.size(), MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS),
				Throwables.getRootCause(tooManyException).getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParamsDoesNotThrow() {
		final GenCMTableContext genCMTableContext = assertDoesNotThrow(() -> new GenCMTableContext.Builder()
				.setEncryptionGroup(gqGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardIds(verificationCardIds)
				.setMaximumNumberOfVotingOptions(MAXIMUM_NUMBER_OF_VOTING_OPTIONS)
				.setCorrectnessInformation(correctnessInformation)
				.build());

		assertEquals(gqGroup, genCMTableContext.getEncryptionGroup());
		assertEquals(electionEventId, genCMTableContext.getElectionEventId());
		assertEquals(verificationCardIds, genCMTableContext.getVerificationCardIds());
		assertEquals(MAXIMUM_NUMBER_OF_VOTING_OPTIONS, genCMTableContext.getMaximumNumberOfVotingOptions());
	}

	private static void initializeParameters() {
		electionEventId = uuidGenerator.generate();
		final int N_e = secureRandom.nextInt(1, MAXIMUM_NUMBER_OF_VOTING_OPTIONS);
		verificationCardIds = IntStream.range(0, N_e)
				.mapToObj(i -> uuidGenerator.generate())
				.collect(toImmutableList());

		final int n = secureRandom.nextInt(1, 5);
		correctnessInformation = IntStream.range(0, n)
				.mapToObj(i -> uuidGenerator.generate())
				.collect(toImmutableList());
	}

	private static Stream<Arguments> provideNullInputsForGenCMTableContext() {
		initializeParameters();

		return Stream.of(
				Arguments.of(null, electionEventId, verificationCardIds, correctnessInformation),
				Arguments.of(gqGroup, null, verificationCardIds, correctnessInformation),
				Arguments.of(gqGroup, electionEventId, null, correctnessInformation),
				Arguments.of(gqGroup, electionEventId, verificationCardIds, null)
		);
	}

	private static Stream<Arguments> provideInvalidUUIDInputsForGenCMTableContext() {
		initializeParameters();

		return Stream.of(
				Arguments.of(gqGroup, "invalidUUID", verificationCardIds, correctnessInformation),
				Arguments.of(gqGroup, electionEventId, ImmutableList.of("invalidUUID"), correctnessInformation)
		);
	}

}
