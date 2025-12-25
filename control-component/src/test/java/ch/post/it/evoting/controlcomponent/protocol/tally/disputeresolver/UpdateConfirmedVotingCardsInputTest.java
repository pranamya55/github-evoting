/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.disputeresolver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.tally.disputeresolver.ResolvedConfirmedVote;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.Constants;

@DisplayName("Constructing an UpdateConfirmedVotingCardsInput with")
class UpdateConfirmedVotingCardsInputTest {

	private static final UUIDGenerator UUID_GENERATOR = UUIDGenerator.getInstance();
	private static final Random RANDOM = RandomFactory.createRandom();

	@Test
	@DisplayName("duplicates in list throws an IllegalArgumentException")
	void updateConfirmedVotingCardsWithDuplicatesInListThrows() {
		final String verificationCardId = UUID_GENERATOR.generate();
		final String verificationCardSetId = UUID_GENERATOR.generate();
		final ImmutableList<String> hashedLongVoteCastReturnCodeShares = Stream.generate(
						() -> RANDOM.genRandomString(Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH, Base64Alphabet.getInstance()))
				.limit(4)
				.collect(ImmutableList.toImmutableList());
		final ImmutableList<ResolvedConfirmedVote> listWithDuplicates = Stream.generate(
						() -> new ResolvedConfirmedVote(verificationCardId, verificationCardSetId, hashedLongVoteCastReturnCodeShares))
				.limit(3)
				.collect(ImmutableList.toImmutableList());
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new UpdateConfirmedVotingCardsInput(listWithDuplicates));
		assertEquals("The list of resolved confirmed votes must not contain duplicate verification card ids.", exception.getMessage());
	}

	@Test
	@DisplayName("empty list does not throw")
	void updateConfirmedVotingCardsWithEmptyListDoesNotThrow() {
		final ImmutableList<ResolvedConfirmedVote> emptyList = ImmutableList.emptyList();
		assertDoesNotThrow(() -> new UpdateConfirmedVotingCardsInput(emptyList));
	}
}