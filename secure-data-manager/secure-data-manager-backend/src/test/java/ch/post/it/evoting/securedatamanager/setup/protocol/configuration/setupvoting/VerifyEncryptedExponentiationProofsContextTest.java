/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("VerifyEncryptedExponentiationProofsContext with")
class VerifyEncryptedExponentiationProofsContextTest {

	private static GqGroup encryptionGroup;
	private static int nodeId;
	private static String electionEventId;
	private static ImmutableList<String> verificationCardIds;
	private static int numberOfVotingOptions;

	@BeforeAll
	static void setUpAll() {
		encryptionGroup = GroupTestData.getGqGroup();
		nodeId = ControlComponentNode.THREE.id();

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		verificationCardIds = Stream.generate(uuidGenerator::generate)
				.limit(5)
				.collect(toImmutableList());

		final Random random = RandomFactory.createRandom();
		numberOfVotingOptions = random.genRandomInteger(MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS) + 1; // must be in range [1, n_sup]
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameter throws NullPointerException")
	void nullParametersThrows(final GqGroup encryptionGroup, final String electionEventId, final ImmutableList<String> verificationCardIds) {
		assertThrows(NullPointerException.class, () ->
				new VerifyEncryptedExponentiationProofsContext(encryptionGroup, nodeId, electionEventId, verificationCardIds, numberOfVotingOptions));
	}

	@ParameterizedTest
	@MethodSource("provideInvalidIds")
	@DisplayName("invalid ids throws FailedValidationException")
	void invalidIdsThrows(final String electionEventId, final ImmutableList<String> verificationCardIds) {
		assertThrows(FailedValidationException.class, () ->
				new VerifyEncryptedExponentiationProofsContext(encryptionGroup, nodeId, electionEventId, verificationCardIds, numberOfVotingOptions));
	}

	@Test
	@DisplayName("invalid node id throws IllegalArgumentException")
	void invalidNodeIdThrows() {
		final int invalidNodeId = -5;
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
				new VerifyEncryptedExponentiationProofsContext(encryptionGroup, invalidNodeId, electionEventId, verificationCardIds,
						numberOfVotingOptions));

		final String expected = String.format("The CCR's index must be in the range [1, %s]. [j: %s]", ControlComponentNode.ids().size(),
				invalidNodeId);
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("too small number of voting options throws IllegalArgumentException")
	void tooSmallNumberOfVotingOptionsThrows() {
		final int tooSmallNumberOfVotingOptions = 0;
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
				new VerifyEncryptedExponentiationProofsContext(encryptionGroup, nodeId, electionEventId, verificationCardIds,
						tooSmallNumberOfVotingOptions));

		final String expected = String.format("The number of voting options must be strictly positive. [n: %s]", tooSmallNumberOfVotingOptions);
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("too big number of voting options throws IllegalArgumentException")
	void tooBigNumberOfVotingOptionsThrows() {
		final int tooBigNumberOfVotingOptions = MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS + 1;
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
				new VerifyEncryptedExponentiationProofsContext(encryptionGroup, nodeId, electionEventId, verificationCardIds,
						tooBigNumberOfVotingOptions));

		final String expected = String.format(
				"The number of voting options must be smaller or equal to the maximum supported number of voting options. [n: %s, n_sup: %s]",
				tooBigNumberOfVotingOptions, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParametersDoesNotThrow() {
		assertDoesNotThrow(() -> new VerifyEncryptedExponentiationProofsContext(encryptionGroup, nodeId, electionEventId, verificationCardIds,
				numberOfVotingOptions));
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, electionEventId, verificationCardIds),
				Arguments.of(encryptionGroup, null, verificationCardIds),
				Arguments.of(encryptionGroup, electionEventId, null)
		);
	}

	private static Stream<Arguments> provideInvalidIds() {
		return Stream.of(
				Arguments.of("invalid UUID", verificationCardIds),
				Arguments.of(electionEventId, ImmutableList.of("invalid UUID"))
		);
	}
}