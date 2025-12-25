/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.disputeresolver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.Constants;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("Constructing an UpdateConfirmedVotingCardsContext with")
class UpdateConfirmedVotingCardsContextTest {

	private static final UUIDGenerator UUID_GENERATOR = UUIDGenerator.getInstance();
	private static final Random RANDOM = RandomFactory.createRandom();

	private int ccrjIndex;
	private String electionEventId;
	private ImmutableMap<String, ImmutableList<String>> longVoteCastReturnCodesAllowLists;

	@BeforeEach
	void setUp() {
		ControlComponentNode.ids().stream().findAny().ifPresent(id -> ccrjIndex = id);
		electionEventId = UUID_GENERATOR.generate();
		longVoteCastReturnCodesAllowLists = genLongVoteCastReturnCodesAllowLists(
				() -> RANDOM.genRandomString(Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH, Base64Alphabet.getInstance()));
	}

	@Test
	@DisplayName("null parameters throws a NullPointerException")
	void nullParametersShouldThrowNullPointerException() {
		assertThrows(NullPointerException.class, () -> new UpdateConfirmedVotingCardsContext(ccrjIndex, null, longVoteCastReturnCodesAllowLists));
		assertThrows(NullPointerException.class, () -> new UpdateConfirmedVotingCardsContext(ccrjIndex, electionEventId, null));
	}

	@Test
	@DisplayName("empty longVoteCastReturnCodesAllowLists throws an IllegalArgumentException")
	void emptyLongVoteCastReturnCodesAllowListShouldThrowIllegalArgumentException() {
		final ImmutableMap<String, ImmutableList<String>> emptyMap = ImmutableMap.emptyMap();
		assertThrows(IllegalArgumentException.class,
				() -> new UpdateConfirmedVotingCardsContext(ccrjIndex, electionEventId, emptyMap));
	}

	@DisplayName("invalid long vote cast return code length throws an IllegalArgumentException")
	@Test
	void invalidLongVoteCastReturnCodesLengthShouldThrowIllegalArgumentException() {
		final ImmutableMap<String, ImmutableList<String>> lVCCAllowListsWithTooShortCode = genLongVoteCastReturnCodesAllowLists(
				() -> RANDOM.genRandomString(Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH - 1, Base64Alphabet.getInstance()));
		assertThrows(IllegalArgumentException.class,
				() -> new UpdateConfirmedVotingCardsContext(ccrjIndex, electionEventId, lVCCAllowListsWithTooShortCode));

		final ImmutableMap<String, ImmutableList<String>> lVCCAllowListsWithTooLongCode = genLongVoteCastReturnCodesAllowLists(
				() -> RANDOM.genRandomString(Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH + 1, Base64Alphabet.getInstance()));
		assertThrows(IllegalArgumentException.class,
				() -> new UpdateConfirmedVotingCardsContext(ccrjIndex, electionEventId, lVCCAllowListsWithTooLongCode));
	}

	@Test
	@DisplayName("invalid long vote cast return code alphabet throws a FailedValidationException")
	void invalidLongVoteCastReturnCodesAlphabetShouldThrowFailedValidationException() {
		final ImmutableMap<String, ImmutableList<String>> lVCCAllowListsWithInvalidCode = genLongVoteCastReturnCodesAllowLists(
				() -> "Not Base64 Encoded!!! Not Base64 Encoded!!!?");
		assertThrows(FailedValidationException.class,
				() -> new UpdateConfirmedVotingCardsContext(ccrjIndex, electionEventId, lVCCAllowListsWithInvalidCode));
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParametersShouldNotThrowException() {
		assertDoesNotThrow(() -> new UpdateConfirmedVotingCardsContext(ccrjIndex, electionEventId, longVoteCastReturnCodesAllowLists));
	}

	private ImmutableMap<String, ImmutableList<String>> genLongVoteCastReturnCodesAllowLists(final Supplier<String> longVoteCastReturnCodeGenerator) {
		return Stream.generate(
						() -> new ImmutableMap.Entry<>(UUID_GENERATOR.generate(),
								Stream.generate(longVoteCastReturnCodeGenerator)
										.limit(3)
										.collect(ImmutableList.toImmutableList())))
				.limit(4)
				.collect(ImmutableMap.toImmutableMap());
	}
}