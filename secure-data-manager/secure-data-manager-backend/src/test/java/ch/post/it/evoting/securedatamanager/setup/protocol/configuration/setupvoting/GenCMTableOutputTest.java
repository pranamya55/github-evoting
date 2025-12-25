/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.entry;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_CAST_RETURN_CODE_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_CHOICE_RETURN_CODE_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_VOTE_CAST_RETURN_CODE_LENGTH;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;

@DisplayName("GenCMTableOutput constructed with")
class GenCMTableOutputTest {

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final SecureRandom secureRandom = new SecureRandom();

	private ImmutableList<ImmutableList<String>> shortChoiceReturnCodes;
	private ImmutableList<String> shortVoteCastReturnCodes;
	private ImmutableMap<String, String> returnCodesMappingTable;

	@BeforeEach
	void setUp() {
		final int N_e = secureRandom.nextInt(1, 10);
		final int n = secureRandom.nextInt(1, 5);

		final Map<String, String> returnCodesMappingTableTemp = new TreeMap<>();
		// Encoded Choice Return Codes.
		for (int i = 0; i < N_e * n; i++) {
			returnCodesMappingTableTemp.put(random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet),
					random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet));
		}
		// Encode Vote Cast Return Codes.
		for (int i = 0; i < N_e; i++) {
			returnCodesMappingTableTemp.put(random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet),
					random.genRandomString(BASE64_ENCODED_CAST_RETURN_CODE_LENGTH, base64Alphabet));
		}

		returnCodesMappingTable = ImmutableMap.from(returnCodesMappingTableTemp, TreeMap::new);

		shortChoiceReturnCodes = IntStream.range(0, N_e)
				.mapToObj(i -> random.genUniqueDecimalStrings(SHORT_CHOICE_RETURN_CODE_LENGTH, n))
				.collect(toImmutableList());
		shortVoteCastReturnCodes = random.genUniqueDecimalStrings(SHORT_VOTE_CAST_RETURN_CODE_LENGTH, N_e);
	}

	@Test
	@DisplayName("any null parameter throws NullPointerException")
	void anyNullParamThrows() {
		assertThrows(NullPointerException.class,
				() -> new GenCMTableOutput(null, shortChoiceReturnCodes, shortVoteCastReturnCodes));
		assertThrows(NullPointerException.class,
				() -> new GenCMTableOutput(returnCodesMappingTable, null, shortVoteCastReturnCodes));
		assertThrows(NullPointerException.class,
				() -> new GenCMTableOutput(returnCodesMappingTable, shortChoiceReturnCodes, null));
	}

	@Test
	@DisplayName("empty values throws IllegalArgumentException")
	void emptyValuesThrows() {
		assertAll(
				() -> {
					final ImmutableMap<String, String> emptyReturnCodesMappingTable = ImmutableMap.emptyMap();

					final IllegalArgumentException returnCodesMappingTableException = assertThrows(IllegalArgumentException.class,
							() -> new GenCMTableOutput(emptyReturnCodesMappingTable, shortChoiceReturnCodes, shortVoteCastReturnCodes));
					assertEquals("Return Codes Mapping table must not be empty.", returnCodesMappingTableException.getMessage());
				},
				() -> {
					final ImmutableList<ImmutableList<String>> emptyShortChoiceReturnCodes = ImmutableList.emptyList();

					final IllegalArgumentException shortChoiceReturnCodesException = assertThrows(IllegalArgumentException.class,
							() -> new GenCMTableOutput(returnCodesMappingTable, emptyShortChoiceReturnCodes, shortVoteCastReturnCodes));
					assertEquals("Short Choice Return Codes must not be empty.", shortChoiceReturnCodesException.getMessage());
				},
				() -> {
					final ImmutableList<ImmutableList<String>> emptyShortChoiceReturnCodes = ImmutableList.of(ImmutableList.emptyList());

					final IllegalArgumentException shortChoiceReturnCodesElementsException = assertThrows(IllegalArgumentException.class,
							() -> new GenCMTableOutput(returnCodesMappingTable, emptyShortChoiceReturnCodes, shortVoteCastReturnCodes));
					assertEquals("Short Choice Return Codes must not contain empty lists.", shortChoiceReturnCodesElementsException.getMessage());
				},
				() -> {
					final ImmutableList<String> emptyShortVoteCastReturnCodes = ImmutableList.emptyList();

					final IllegalArgumentException shortVoteCastReturnCodesException = assertThrows(IllegalArgumentException.class,
							() -> new GenCMTableOutput(returnCodesMappingTable, shortChoiceReturnCodes, emptyShortVoteCastReturnCodes));
					assertEquals("Vote Cast Return Codes must not be empty.", shortVoteCastReturnCodesException.getMessage());
				}
		);
	}

	@Test
	@DisplayName("invalid key length throws IllegalArgumentException")
	void invalidKeyLengthThrows() {
		final ImmutableMap<String, String> invalidReturnCodesMappingTable = ImmutableMap.of(
				random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet),
				random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet),
				random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH - 1, base64Alphabet),
				random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet)
		);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenCMTableOutput(invalidReturnCodesMappingTable, shortChoiceReturnCodes, shortVoteCastReturnCodes));
		assertEquals(String.format("The CM table's keys must be valid Base64 string of length %s.", BASE64_ENCODED_HASH_OUTPUT_LENGTH),
				exception.getMessage());
	}

	@Test
	@DisplayName("invalid choice code length throws IllegalArgumentException")
	void invalidChoiceCodeLengthThrows() {
		final ImmutableList<String> choiceCodes = ImmutableList.of(
				random.genRandomString(SHORT_CHOICE_RETURN_CODE_LENGTH, base64Alphabet),
				random.genRandomString(SHORT_CHOICE_RETURN_CODE_LENGTH + 1, base64Alphabet));
		final ImmutableList<ImmutableList<String>> invalidShortChoiceReturnCodes = ImmutableList.of(choiceCodes, choiceCodes);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenCMTableOutput(returnCodesMappingTable, invalidShortChoiceReturnCodes, shortVoteCastReturnCodes));
		assertEquals(String.format("Short Choice Return Codes values must have a length of %s.", SHORT_CHOICE_RETURN_CODE_LENGTH),
				exception.getMessage());
	}

	@Test
	@DisplayName("invalid vote cast code length throws IllegalArgumentException")
	void invalidVoteCastCodeLengthThrows() {
		final ImmutableList<String> invalidShortVoteCastReturnCodes = ImmutableList.of(
				random.genRandomString(SHORT_VOTE_CAST_RETURN_CODE_LENGTH, base64Alphabet),
				random.genRandomString(SHORT_VOTE_CAST_RETURN_CODE_LENGTH + 1, base64Alphabet));

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenCMTableOutput(returnCodesMappingTable, shortChoiceReturnCodes, invalidShortVoteCastReturnCodes));
		assertEquals(String.format("Short Vote Cast Return Codes values must have a length of %s.", SHORT_VOTE_CAST_RETURN_CODE_LENGTH),
				exception.getMessage());
	}

	@Test
	@DisplayName("invalid short code list size throws IllegalArgumentException")
	void invalidShortCodeListSizeThrows() {
		final ImmutableList<String> invalidShortVoteCastReturnCodes = shortVoteCastReturnCodes.append(
				random.genRandomString(SHORT_VOTE_CAST_RETURN_CODE_LENGTH, base64Alphabet));

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenCMTableOutput(returnCodesMappingTable, shortChoiceReturnCodes, invalidShortVoteCastReturnCodes));
		assertEquals("Short Choice Return Codes and short Vote Cast Return Codes must have the same number of elements.", exception.getMessage());
	}

	@Test
	@DisplayName("invalid code mapping table size throws IllegalArgumentException")
	void invalidCodeMappingTableSizeThrows() {
		final ImmutableMap<String, String> invalidReturnCodesMappingTable = Stream.concat(
						returnCodesMappingTable.asMap().entrySet().stream()
								.map(kv -> entry(kv.getKey(), kv.getValue())),
						Stream.of(entry(random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet),
								random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))))
				.collect(toImmutableMap(TreeMap::new));

		final int expectedReturnCodesMappingTableSize = shortChoiceReturnCodes.size() * (shortChoiceReturnCodes.get(0).size() + 1);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenCMTableOutput(invalidReturnCodesMappingTable, shortChoiceReturnCodes, shortVoteCastReturnCodes));
		assertEquals(String.format("Return Codes Mapping table must have a size of %s.", expectedReturnCodesMappingTableSize),
				exception.getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParamsDoesNotThrow() {
		final GenCMTableOutput genCMTableOutput = assertDoesNotThrow(
				() -> new GenCMTableOutput(returnCodesMappingTable, shortChoiceReturnCodes, shortVoteCastReturnCodes));

		assertEquals(returnCodesMappingTable, genCMTableOutput.returnCodesMappingTable());
		assertEquals(shortChoiceReturnCodes, genCMTableOutput.shortChoiceReturnCodes());
		assertEquals(shortVoteCastReturnCodes, genCMTableOutput.shortVoteCastReturnCodes());
	}

}
