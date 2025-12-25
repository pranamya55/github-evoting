/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.converters;

import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.math.Base64;

@Converter
public class PayloadHashConverter implements AttributeConverter<ImmutableByteArray, String> {

	private final Base64 base64;

	public PayloadHashConverter(final Base64 base64) {
		this.base64 = base64;
	}

	@Override
	public String convertToDatabaseColumn(final ImmutableByteArray immutableByteArray) {
		checkNotNull(immutableByteArray);
		return base64.base64Encode(immutableByteArray);
	}

	@Override
	public ImmutableByteArray convertToEntityAttribute(final String string) {
		checkNotNull(string);
		return base64.base64Decode(string);
	}

}
