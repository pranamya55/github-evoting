/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting;

import static ch.post.it.evoting.evotinglibraries.domain.ConversionUtils.bigIntegerToBase64;

import java.io.IOException;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

class BigIntegerSerializer extends JsonSerializer<BigInteger> {

	@Override
	public void serialize(final BigInteger bigInteger, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider)
			throws IOException {
		jsonGenerator.writeString(bigIntegerToBase64(bigInteger));
	}
}