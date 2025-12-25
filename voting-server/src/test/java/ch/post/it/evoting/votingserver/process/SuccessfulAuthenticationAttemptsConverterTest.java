/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.UncheckedIOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

class SuccessfulAuthenticationAttemptsConverterTest {

	private static final Random RANDOM = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static SuccessfulAuthenticationAttemptsConverter converter;

	private SuccessfulAuthenticationAttempts attempts;

	@BeforeAll
	static void setupAll() {
		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		converter = new SuccessfulAuthenticationAttemptsConverter(objectMapper);
	}

	@BeforeEach
	void setup() {
		final ImmutableList<String> successfulAuthenticationChallenges = Stream.generate(
						() -> RANDOM.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
				.limit(5)
				.collect(toImmutableList());
		attempts = new SuccessfulAuthenticationAttempts(successfulAuthenticationChallenges);
	}

	@Test
	void convertToDatabaseColumnWithNullArgumentThrows() {
		assertThrows(NullPointerException.class, () -> converter.convertToDatabaseColumn(null));
	}

	@Test
	void convertToEntityAttributeWithNullArgumentThrows() {
		assertThrows(NullPointerException.class, () -> converter.convertToEntityAttribute(null));
	}

	@Test
	void convertToEntityAttributeWhenByteArrayCannotBeConvertedThenThrows() {
		assertThrows(UncheckedIOException.class, () -> converter.convertToEntityAttribute(new byte[] { 0b0 }));
	}

	@RepeatedTest(100)
	void cycle() {
		final byte[] bytes = assertDoesNotThrow(() -> converter.convertToDatabaseColumn(attempts));
		final SuccessfulAuthenticationAttempts convertedAttempts = assertDoesNotThrow(() -> converter.convertToEntityAttribute(bytes));
		assertEquals(attempts, convertedAttempts);
	}
}
