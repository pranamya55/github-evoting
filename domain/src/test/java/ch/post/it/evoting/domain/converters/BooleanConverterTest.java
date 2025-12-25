/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BooleanConverterTest {

	private final BooleanConverter booleanConverter = new BooleanConverter();

	@Test
	void convertToDatabaseColumnReturnsExpectedValue() {
		assertNull(booleanConverter.convertToDatabaseColumn(null));
		assertEquals('Y', booleanConverter.convertToDatabaseColumn(true));
		assertEquals('N', booleanConverter.convertToDatabaseColumn(false));
	}

	@Test
	void convertToEntityAttributeReturnsExpectedValue() {
		assertNull(booleanConverter.convertToEntityAttribute(null));
		assertTrue(booleanConverter.convertToEntityAttribute('Y'));
		assertFalse(booleanConverter.convertToEntityAttribute('N'));
	}
}