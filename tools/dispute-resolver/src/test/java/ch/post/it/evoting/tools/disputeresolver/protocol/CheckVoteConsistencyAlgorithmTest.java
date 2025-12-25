/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.protocol;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedVerificationCardsPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.generators.ExtractedVerificationCardGenerator;

@DisplayName("CheckVoteConsistencyAlgorithm with")
class CheckVoteConsistencyAlgorithmTest {

	private static ImmutableList<ImmutableList<ExtractedVerificationCard>> input;
	private static CheckVoteConsistencyAlgorithm checkVoteConsistencyAlgorithm;

	@BeforeAll
	static void setUpAll() {
		final ControlComponentExtractedVerificationCardsPayloadGenerator generator = new ControlComponentExtractedVerificationCardsPayloadGenerator();

		input = generator.generate().stream()
				.map(ControlComponentExtractedVerificationCardsPayload::getExtractedVerificationCards)
				.collect(toImmutableList());

		checkVoteConsistencyAlgorithm = new CheckVoteConsistencyAlgorithm(BaseEncodingFactory.createBase64(), HashFactory.createHash());
	}

	@Test
	@DisplayName("null input throws NullPointerException.")
	void nullInputThrows() {
		assertThrows(NullPointerException.class, () -> checkVoteConsistencyAlgorithm.checkVoteConsistency(null));
	}

	@Test
	@DisplayName("not enough extracted verification cards throws IllegalArgumentException.")
	void notEnoughExtractedVerificationCardsThrows() {
		final ImmutableList<ImmutableList<ExtractedVerificationCard>> notEnoughExtractedVerificationCard = input.subList(0, 3);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> checkVoteConsistencyAlgorithm.checkVoteConsistency(notEnoughExtractedVerificationCard));

		final String expected = "There must be as many CCR's extracted verification cards as node ids.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("too many extracted verification cards throws IllegalArgumentException.")
	void tooManyExtractedVerificationCardsThrows() {
		final ImmutableList<ImmutableList<ExtractedVerificationCard>> tooManyExtractedVerificationCard = input.append(input.getFirst());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> checkVoteConsistencyAlgorithm.checkVoteConsistency(tooManyExtractedVerificationCard));

		final String expected = "There must be as many CCR's extracted verification cards as node ids.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("inconsistent encrypted votes returns false.")
	void invalidReturnsFalse() {
		final ExtractedVerificationCard otherExtractedVerificationCard = new ExtractedVerificationCardGenerator().generate();
		final ImmutableList<ImmutableList<ExtractedVerificationCard>> inconsistentVote = input.subList(0, 3)
				.append(ImmutableList.of(otherExtractedVerificationCard));

		assertFalse(() -> checkVoteConsistencyAlgorithm.checkVoteConsistency(inconsistentVote));
	}

	@Test
	@DisplayName("consistent encrypted votes returns true.")
	void validReturnsTrue() {
		assertTrue(() -> checkVoteConsistencyAlgorithm.checkVoteConsistency(input));
	}
}