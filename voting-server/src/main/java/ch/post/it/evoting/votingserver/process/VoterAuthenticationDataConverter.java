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

import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationData;

@Converter
public class VoterAuthenticationDataConverter implements AttributeConverter<SetupComponentVoterAuthenticationData, byte[]> {

	private final ObjectMapper objectMapper;

	public VoterAuthenticationDataConverter(final ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public byte[] convertToDatabaseColumn(final SetupComponentVoterAuthenticationData setupComponentVoterAuthenticationData) {
		checkNotNull(setupComponentVoterAuthenticationData);

		final String verificationCardId = setupComponentVoterAuthenticationData.verificationCardId();

		try {
			return objectMapper.writeValueAsBytes(setupComponentVoterAuthenticationData);
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(
					String.format("Failed to serialize voter authentication data. [verificationCardId: %s]", verificationCardId), e);
		}
	}

	@Override
	public SetupComponentVoterAuthenticationData convertToEntityAttribute(final byte[] bytes) {
		checkNotNull(bytes);

		try {
			return objectMapper.readValue(bytes, SetupComponentVoterAuthenticationData.class);
		} catch (final IOException e) {
			throw new UncheckedIOException("Failed to deserialize voter authentication data.", e);
		}
	}
}
