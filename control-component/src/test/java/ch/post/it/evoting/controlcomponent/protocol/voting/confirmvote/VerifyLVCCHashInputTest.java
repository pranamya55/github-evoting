/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.controlcomponent.process.LongVoteCastReturnCodesAllowList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

@DisplayName("Building a VerifyLVCCHashInput with")
class VerifyLVCCHashInputTest {

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();

	private ImmutableList<String> otherCCRhlVCC;
	private VerifyLVCCHashInput.Builder inputBuilder;

	@BeforeEach
	void setup() {
		final String hlVCC1 = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		otherCCRhlVCC = Stream.generate(() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
				.limit(ControlComponentNode.ids().size() - 1)
				.collect(toImmutableList());
		inputBuilder = new VerifyLVCCHashInput.Builder()
				.setLongVoteCastReturnCodesAllowList(longVoteCastReturnCode -> true)
				.setCcrjHashedLongVoteCastReturnCode(hlVCC1)
				.setOtherCCRsHashedLongVoteCastReturnCodes(otherCCRhlVCC);
	}

	private static Stream<Arguments> nullArgumentProvider() {
		final String hlVCC1 = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		final ImmutableList<String> otherCCRhlVCC = Stream.generate(() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
				.limit(ControlComponentNode.ids().size() - 1)
				.collect(toImmutableList());
		final LongVoteCastReturnCodesAllowList longVoteCastReturnCodesAllowList = longVoteCastReturnCode -> true;
		return Stream.of(
				Arguments.of(new VerifyLVCCHashInput.Builder()
						.setCcrjHashedLongVoteCastReturnCode(hlVCC1)
						.setOtherCCRsHashedLongVoteCastReturnCodes(otherCCRhlVCC)),
				Arguments.of(new VerifyLVCCHashInput.Builder()
						.setLongVoteCastReturnCodesAllowList(longVoteCastReturnCodesAllowList)
						.setOtherCCRsHashedLongVoteCastReturnCodes(otherCCRhlVCC)),
				Arguments.of(new VerifyLVCCHashInput.Builder()
						.setLongVoteCastReturnCodesAllowList(longVoteCastReturnCodesAllowList)
						.setCcrjHashedLongVoteCastReturnCode(hlVCC1))
		);
	}

	@ParameterizedTest
	@MethodSource("nullArgumentProvider")
	@DisplayName("null arguments throws a NullPointerException")
	void buildVerifyLVCCHashInputWithNullArgumentsThrows(final VerifyLVCCHashInput.Builder builder) {
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("too few other CCRs hashed Long Vote Cast Return Codes throws an IllegalArgumentException")
	void buildVerifyLVCCHashInputWithTooFewOtherCCRsHashedLongVoteCastReturnCodesThrows() {
		otherCCRhlVCC = Stream.generate(() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
				.limit(ControlComponentNode.ids().size() - 2)
				.collect(toImmutableList());
		final VerifyLVCCHashInput.Builder otherInput = inputBuilder.setOtherCCRsHashedLongVoteCastReturnCodes(otherCCRhlVCC);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, otherInput::build);
		assertEquals("The number of other CCRs hashed long Vote Cast Return Codes must be equal to the number of known node ids - 1.",
				exception.getMessage());
	}

	@Test
	@DisplayName("too many other CCRs hashed Long Vote Cast Return Codes throws an IllegalArgumentException")
	void buildVerifyLVCCHashInputWithTooManyOtherCCRsHashedLongVoteCastReturnCodesThrows() {
		otherCCRhlVCC = Stream.generate(() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
				.limit(ControlComponentNode.ids().size())
				.collect(toImmutableList());
		final VerifyLVCCHashInput.Builder otherInput = inputBuilder.setOtherCCRsHashedLongVoteCastReturnCodes(otherCCRhlVCC);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, otherInput::build);
		assertEquals("The number of other CCRs hashed long Vote Cast Return Codes must be equal to the number of known node ids - 1.",
				exception.getMessage());
	}
}
