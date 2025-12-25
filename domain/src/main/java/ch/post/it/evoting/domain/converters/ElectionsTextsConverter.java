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
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.ElectionTexts;

@Converter
public class ElectionsTextsConverter implements AttributeConverter<ImmutableList<ElectionTexts>, byte[]> {

	private final ObjectMapper objectMapper;

	public ElectionsTextsConverter(final ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public byte[] convertToDatabaseColumn(final ImmutableList<ElectionTexts> electionsTexts) {
		checkNotNull(electionsTexts);

		try {
			return objectMapper.writeValueAsBytes(electionsTexts);
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException("Failed to serialize electionsTexts.", e);
		}
	}

	@Override
	public ImmutableList<ElectionTexts> convertToEntityAttribute(final byte[] electionsTexts) {
		checkNotNull(electionsTexts);

		try {
			return objectMapper.readValue(electionsTexts, new TypeReference<>() {});
		} catch (final IOException e) {
			throw new UncheckedIOException("Failed to deserialize electionsTexts.", e);
		}
	}

}
