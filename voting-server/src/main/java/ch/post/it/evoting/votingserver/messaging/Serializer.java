/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.messaging;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;

@Service
public class Serializer {

	private final ObjectMapper objectMapper;

	public Serializer(final ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public <T> T deserialize(final ImmutableByteArray bytes, final Class<T> clazz) {
		try {
			return objectMapper.readValue(bytes.elements(), clazz);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public <T> T deserialize(final ImmutableByteArray bytes, final TypeReference<T> typeRef) {
		try {
			return objectMapper.readValue(bytes.elements(), typeRef);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public <T> ImmutableByteArray serialize(final T payload) {
		try {
			return new ImmutableByteArray(objectMapper.writeValueAsBytes(payload));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
	}
}
