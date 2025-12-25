/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;

@Converter
public class ImmutableByteArrayConverter implements AttributeConverter<ImmutableByteArray, byte[]> {

	@Override
	public byte[] convertToDatabaseColumn(final ImmutableByteArray immutableByteArray) {
		return immutableByteArray != null ? immutableByteArray.elements() : null;
	}

	@Override
	public ImmutableByteArray convertToEntityAttribute(final byte[] bytes) {
		return bytes != null ? new ImmutableByteArray(bytes) : null;
	}

}
