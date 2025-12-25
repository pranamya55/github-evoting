/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Converter
public class SuccessfulAuthenticationAttemptsConverter implements AttributeConverter<SuccessfulAuthenticationAttempts, byte[]> {

	private final ObjectMapper objectMapper;

	public SuccessfulAuthenticationAttemptsConverter(final ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public byte[] convertToDatabaseColumn(final SuccessfulAuthenticationAttempts successfulAuthenticationAttempts) {
		checkNotNull(successfulAuthenticationAttempts);

		try {
			return objectMapper.writeValueAsBytes(successfulAuthenticationAttempts);
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(String.format("Failed to serialize authentication challenges: %s", successfulAuthenticationAttempts), e);
		}
	}

	@Override
	public SuccessfulAuthenticationAttempts convertToEntityAttribute(final byte[] bytes) {
		checkNotNull(bytes);

		try {
			return objectMapper.readValue(bytes, SuccessfulAuthenticationAttempts.class);
		} catch (final IOException e) {
			throw new UncheckedIOException("Failed to deserialize authentication challenge.", e);
		}
	}
}
