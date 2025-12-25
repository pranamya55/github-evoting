/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.converters;

import java.io.IOException;
import java.io.UncheckedIOException;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.mapper.EncryptionGroupUtils;

@Converter
public class EncryptionGroupConverter implements AttributeConverter<GqGroup, byte[]> {

	private final ObjectMapper objectMapper;

	public EncryptionGroupConverter(final ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public byte[] convertToDatabaseColumn(final GqGroup encryptionGroup) {
		try {
			return objectMapper.writeValueAsBytes(encryptionGroup);
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException("Failed to serialize the encryption group.", e);
		}
	}

	@Override
	public GqGroup convertToEntityAttribute(final byte[] bytes) {
		// Use object mapper cache to avoid deserializing too many times the GqGroups.
		final JsonNode encryptionGroupNode;
		try {
			encryptionGroupNode = objectMapper.readTree(bytes);
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not retrieve the group.", e);
		}

		return EncryptionGroupUtils.getEncryptionGroup(objectMapper, encryptionGroupNode);
	}

}
