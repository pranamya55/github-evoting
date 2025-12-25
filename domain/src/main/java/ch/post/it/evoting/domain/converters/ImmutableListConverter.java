/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.converters;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

@Converter
public class ImmutableListConverter implements AttributeConverter<ImmutableList<String>, byte[]> {

	private final ObjectMapper objectMapper;

	public ImmutableListConverter(final ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public byte[] convertToDatabaseColumn(final ImmutableList<String> list) {
		checkNotNull(list);
		try {
			return objectMapper.writeValueAsBytes(list);
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException("Failed to serialize list to bytes.", e);
		}
	}

	@Override
	public ImmutableList<String> convertToEntityAttribute(final byte[] bytes) {
		checkNotNull(bytes);
		try {
			return objectMapper.readValue(bytes, new TypeReference<>() {
			});
		} catch (final IOException e) {
			throw new UncheckedIOException("Failed to deserialize bytes into list.", e);
		}
	}

}
