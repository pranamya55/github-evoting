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
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.VoteTexts;

@Converter
public class VotesTextsConverter implements AttributeConverter<ImmutableList<VoteTexts>, byte[]> {

	private final ObjectMapper objectMapper;

	public VotesTextsConverter(final ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public byte[] convertToDatabaseColumn(final ImmutableList<VoteTexts> votesTexts) {
		checkNotNull(votesTexts);

		try {
			return objectMapper.writeValueAsBytes(votesTexts);
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException("Failed to serialize votesTexts.", e);
		}
	}

	@Override
	public ImmutableList<VoteTexts> convertToEntityAttribute(final byte[] votesTexts) {
		checkNotNull(votesTexts);

		try {
			return objectMapper.readValue(votesTexts, new TypeReference<>() {});
		} catch (final IOException e) {
			throw new UncheckedIOException("Failed to deserialize votesTexts.", e);
		}
	}

}
