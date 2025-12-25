/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BooleanConverter implements AttributeConverter<Boolean, Character> {

	private static final char TRUE = 'Y';
	private static final char FALSE = 'N';

	@Override
	public Character convertToDatabaseColumn(final Boolean aBoolean) {
		if (aBoolean != null) {
			return aBoolean ? TRUE : FALSE;
		}
		return null;
	}

	@Override
	public Boolean convertToEntityAttribute(final Character character) {
		return character != null ? character.equals(TRUE) : null;
	}

}
