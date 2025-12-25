/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally.disputeresolver;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.generators.DisputeResolverResolvedConfirmedVotesPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("A ResolvedConfirmedVotes constructed with")
class ResolvedConfirmedVoteTest {

	private static final Random random = RandomFactory.createRandom();
	private static final Base64Alphabet base64Alphabet = Base64Alphabet.getInstance();

	private String verificationCardId;
	private String verificationCardSetId;
	private ImmutableList<String> hashedLongVoteCastReturnCodeShares;

	@BeforeEach
	void setup() {
		final DisputeResolverResolvedConfirmedVotesPayloadGenerator generator = new DisputeResolverResolvedConfirmedVotesPayloadGenerator();
		final ResolvedConfirmedVote resolvedConfirmedVote = generator.generate().getResolvedConfirmedVotes().getFirst();

		verificationCardId = resolvedConfirmedVote.verificationCardId();
		verificationCardSetId = resolvedConfirmedVote.verificationCardSetId();
		hashedLongVoteCastReturnCodeShares = resolvedConfirmedVote.hashedLongVoteCastReturnCodeShares();
	}

	@Test
	@DisplayName("null arguments throws NullPointerException")
	void constructWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class,
				() -> new ResolvedConfirmedVote(null, verificationCardSetId, hashedLongVoteCastReturnCodeShares));
		assertThrows(NullPointerException.class,
				() -> new ResolvedConfirmedVote(verificationCardId, null, hashedLongVoteCastReturnCodeShares));
		assertThrows(NullPointerException.class,
				() -> new ResolvedConfirmedVote(verificationCardId, verificationCardSetId, null));
	}

	@Test
	@DisplayName("an invalid verification card id throws IllegalArgumentException")
	void constructWithInvalidVerificationCardIdThrows() {
		assertThrows(FailedValidationException.class,
				() -> new ResolvedConfirmedVote("invalid verification card id", verificationCardSetId, hashedLongVoteCastReturnCodeShares));
	}

	@Test
	@DisplayName("an invalid verification card set id throws IllegalArgumentException")
	void constructWithInvalidVerificationCardSetIdThrows() {
		assertThrows(FailedValidationException.class,
				() -> new ResolvedConfirmedVote(verificationCardId, "invalid verification card set id", hashedLongVoteCastReturnCodeShares));
	}

	@Test
	@DisplayName("an invalid number of hashedLongVoteCastReturnCodeShares throws IllegalArgumentException")
	void constructWithInvalidNumberOfHashedLVCCSharesThrows() {
		final ImmutableList<String> invalidHashedLongVoteCastReturnCodeShares = Stream.generate(
						() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
				.limit(ControlComponentNode.ids().size() + 1)
				.collect(ImmutableList.toImmutableList());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ResolvedConfirmedVote(verificationCardId, verificationCardSetId,
						invalidHashedLongVoteCastReturnCodeShares));

		final String expected = String.format("There must be exactly %s hashed Long Vote Cast Return Code shares.",
				ControlComponentNode.ids().size());
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("with invalid length of hashedLongVoteCastReturnCodeShares throws an IllegalArgumentException")
	void constructWithInvalidLengthOfHashedLVCCSharesThrows() {
		final ImmutableList<String> invalidLengthHashedLVCCShares = Stream.generate(
						() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH - 1, base64Alphabet))
				.limit(ControlComponentNode.ids().size())
				.collect(ImmutableList.toImmutableList());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ResolvedConfirmedVote(verificationCardId, verificationCardSetId,
						invalidLengthHashedLVCCShares));

		final String expected = String.format("The hashed long Vote Cast Return Code shares must be of size l_HB64. [size: %s, l_HB64: %s]",
				invalidLengthHashedLVCCShares.getFirst().length(), BASE64_ENCODED_HASH_OUTPUT_LENGTH);
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("with invalid base64 encoded hashedLongVoteCastReturnCodeShares throws a FailedValidationException")
	void constructWithInvalidBase64EncodedHashedLVCCSharesThrows() {
		final ImmutableList<String> invalidBase64EncodedHashedLVCCShares = Stream.generate(
						() -> "%" + random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH - 1, base64Alphabet))
				.limit(ControlComponentNode.ids().size())
				.collect(ImmutableList.toImmutableList());

		final FailedValidationException exception = assertThrows(FailedValidationException.class,
				() -> new ResolvedConfirmedVote(verificationCardId, verificationCardSetId,
						invalidBase64EncodedHashedLVCCShares));

		final String expected = String.format("The given string is not a valid Base64 encoded string. [string: %s].",
				invalidBase64EncodedHashedLVCCShares.getFirst());
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid arguments does not throw")
	void constructWithValidArgumentsCreatesInstance() {
		assertDoesNotThrow(() -> new ResolvedConfirmedVote(verificationCardId, verificationCardSetId, hashedLongVoteCastReturnCodeShares));
	}

	@Test
	@DisplayName("empty hashedLongVoteCastReturnCodeShares throws")
	void constructWithEmptyHashedLVCCSharesCreatesInstance() {
		final ImmutableList<String> emptyHashedLongVoteCastReturnCodeShares = ImmutableList.emptyList();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ResolvedConfirmedVote(verificationCardId, verificationCardSetId, emptyHashedLongVoteCastReturnCodeShares));

		final String expected = String.format("There must be exactly %s hashed Long Vote Cast Return Code shares.",
				ControlComponentNode.ids().size());
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}
}
