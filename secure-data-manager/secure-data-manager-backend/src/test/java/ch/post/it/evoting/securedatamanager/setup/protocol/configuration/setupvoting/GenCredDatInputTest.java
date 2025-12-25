/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.ID_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base16Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class GenCredDatInputTest {

	private static final int BOUND = 12;
	private static final Alphabet base16Alphabet = Base16Alphabet.getInstance();

	private final Random rand = RandomFactory.createRandom();
	private final SecureRandom srand = new SecureRandom();
	private final ZqGroup zqGroup = GroupTestData.getZqGroup();
	private final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(zqGroup);
	private final int size = srand.nextInt(BOUND) + 1;
	private final GroupVector<ZqElement, ZqGroup> verificationCardSecretKeys =
			Stream.generate(zqGroupGenerator::genRandomZqElementMember).limit(size).collect(GroupVector.toGroupVector());
	private final ImmutableList<String> startVotingKeys = Stream.generate(ElectionSetupUtils::genStartVotingKey)
			.limit(size)
			.collect(toImmutableList());

	@Test
	@DisplayName("happyPath")
	void happyPath() {

		final GenCredDatInput input = assertDoesNotThrow(() -> new GenCredDatInput(verificationCardSecretKeys, startVotingKeys));

		assertTrue(input.verificationCardSecretKeys().stream()
						.allMatch(vl -> verificationCardSecretKeys.stream().allMatch(vr -> vr.getGroup().getQ().equals(vl.getGroup().getQ()))),
				"Input verificationCardSecretKeys has different GqGroup");
		assertTrue(input.verificationCardSecretKeys().stream()
						.allMatch(elt1 -> verificationCardSecretKeys.stream().anyMatch(elt2 -> elt2.equals(elt1))),
				"Input verificationCardSecretKeys does not contains all elements");
		assertTrue(input.startVotingKeys().containsAll(startVotingKeys), "Input startVotingKeys does not contains all elements");
	}

	@Nested
	@DisplayName("A null arguments throws a NullPointerException")
	class NullArgumentThrowsNullPointerException {

		@Test
		@DisplayName("all arguments null")
		void constructWithNullArguments() {
			final NullPointerException ex =
					assertThrows(NullPointerException.class, () -> new GenCredDatInput(null, null));

			final String expectedMessage = null;

			assertEquals(expectedMessage, ex.getMessage());
		}

		@Test
		@DisplayName("verificationCardSecretKeys argument null")
		void verificationCardSecretKeysTest() {
			final NullPointerException ex =
					assertThrows(NullPointerException.class, () -> new GenCredDatInput(null, startVotingKeys));

			final String expectedMessage = null;

			assertEquals(expectedMessage, ex.getMessage());
		}

		@Test
		@DisplayName("startVotingKeys argument null")
		void startVotingKeysTest() {
			final NullPointerException ex =
					assertThrows(NullPointerException.class, () -> new GenCredDatInput(verificationCardSecretKeys, null));

			final String expectedMessage = null;

			assertEquals(expectedMessage, ex.getMessage());
		}

		@Test
		@DisplayName("all argument not null")
		void allArgumentsTest() {
			assertDoesNotThrow(() -> new GenCredDatInput(verificationCardSecretKeys, startVotingKeys));
		}
	}

	@Nested
	@DisplayName("Invalid arguments throws an Exception")
	class InvalidArgumentThrowsException {

		@Test
		@DisplayName("verificationCardSecretKeys argument empty")
		void emptyVerificationCardSecretKeysTest() {
			final GroupVector<ZqElement, ZqGroup> emptyVerificationCardSecretKeys = GroupVector.empty();

			final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
					() -> new GenCredDatInput(emptyVerificationCardSecretKeys, startVotingKeys));

			final String expectedMessage = "All vectors must have the same size.";

			assertEquals(expectedMessage, ex.getMessage());

		}

		@Test
		@DisplayName("verificationCardSecretKeys and startVotingKeys argument empty")
		void emptyVerificationCardSecretKeysAndStartVotingKeysTest() {
			final GroupVector<ZqElement, ZqGroup> emptyVerificationCardSecretKeys = GroupVector.empty();
			final ImmutableList<String> emptyStartVotingKeys = ImmutableList.emptyList();

			final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
					() -> new GenCredDatInput(emptyVerificationCardSecretKeys, emptyStartVotingKeys));

			final String expectedMessage = "The vector of verification card secret key must not be empty.";

			assertEquals(expectedMessage, ex.getMessage());

		}

		@Test
		@DisplayName("verificationCardSecretKeys argument invalid size")
		void invalidVerificationCardSecretKeysTest() {
			final GroupVector<ZqElement, ZqGroup> invalidVerificationCardSecretKeys = Stream.generate(zqGroupGenerator::genRandomZqElementMember)
					.limit(size + 1)
					.collect(GroupVector.toGroupVector());

			final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
					() -> new GenCredDatInput(invalidVerificationCardSecretKeys, startVotingKeys));

			final String expectedMessage = "All vectors must have the same size.";

			assertEquals(expectedMessage, ex.getMessage());

		}

		@Test
		@DisplayName("startVotingKeys argument empty")
		void invalidStartVotingKeysTest() {
			final ImmutableList<String> emptyStartVotingKeys = ImmutableList.emptyList();

			final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
					() -> new GenCredDatInput(verificationCardSecretKeys, emptyStartVotingKeys));

			final String expectedMessage = "All vectors must have the same size.";

			assertEquals(expectedMessage, ex.getMessage());

		}

		@Test
		@DisplayName("startVotingKeys has element with item =/ length")
		void elementsDifferentLengthStartVotingKeysTest() {
			final ImmutableList<String> startVotingKeysElementDifferentLength = Stream.generate(
							() -> rand.genRandomString(ID_LENGTH - 1, base16Alphabet))
					.limit(size)
					.collect(toImmutableList());

			final FailedValidationException ex = assertThrows(FailedValidationException.class,
					() -> new GenCredDatInput(verificationCardSecretKeys, startVotingKeysElementDifferentLength));

			final String expectedMessage = "The given string has not the expected Start Voting Key length.";

			assertTrue(ex.getMessage().startsWith(expectedMessage));
		}
	}
}
