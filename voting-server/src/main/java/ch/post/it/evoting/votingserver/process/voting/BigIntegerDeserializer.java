/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting;

import static ch.post.it.evoting.evotinglibraries.domain.ConversionUtils.base64ToBigInteger;

import java.io.IOException;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

public class BigIntegerDeserializer extends JsonDeserializer<BigInteger> {

	@Override
	public BigInteger deserialize(final JsonParser parser, final DeserializationContext context)
			throws IOException {

		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		final JsonNode node = objectMapper.readTree(parser);
		final String value = node.asText();

		return base64ToBigInteger(value);
	}
}