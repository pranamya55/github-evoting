/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.domain.converters.BooleanConverter;

@DisplayName("BooleanConverter calling")
class BooleanConverterTest {

	private static final BooleanConverter booleanConverter = new BooleanConverter();

	@Test
	@DisplayName("convertToDatabaseColumn with given boolean returns expected character")
	void convertToDatabaseColumnWithBooleanReturnsExpectedCharacter() {
		assertEquals('Y', booleanConverter.convertToDatabaseColumn(true));
		assertEquals('N', booleanConverter.convertToDatabaseColumn(false));
	}

	@Test
	@DisplayName("convertToDatabaseColumn with null argument returns null")
	void convertToDatabaseColumnWithNullReturnsNull() {
		assertNull(booleanConverter.convertToDatabaseColumn(null));
	}

	@Test
	@DisplayName("convertToEntityAttribute with given character returns expected boolean")
	void convertToEntityAttributeWithYReturnsTrue() {
		assertTrue(booleanConverter.convertToEntityAttribute('Y'));
		assertFalse(booleanConverter.convertToEntityAttribute('N'));
	}

	@Test
	@DisplayName("convertToEntityAttribute with null argument returns null")
	void convertToEntityAttributeWithNullReturnsNull() {
		assertNull(booleanConverter.convertToEntityAttribute(null));
	}
}
