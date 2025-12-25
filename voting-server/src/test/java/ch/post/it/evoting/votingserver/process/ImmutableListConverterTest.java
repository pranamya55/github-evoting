/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.UncheckedIOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.converters.ImmutableListConverter;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

class ImmutableListConverterTest {

	private static final Random RANDOM = RandomFactory.createRandom();

	private static ImmutableListConverter converter;

	private ImmutableList<String> list;

	@BeforeAll
	static void setupAll() {
		converter = new ImmutableListConverter(DomainObjectMapper.getNewInstance());
	}

	@BeforeEach
	void setup() {
		list = RANDOM.genUniqueDecimalStrings(4, 10);
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
		final byte[] bytes = assertDoesNotThrow(() -> converter.convertToDatabaseColumn(list));
		final ImmutableList<String> convertedList = assertDoesNotThrow(() -> converter.convertToEntityAttribute(bytes));
		assertEquals(list, convertedList);
	}
}
