/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

public class SetupComponentPublicKeysResponsePayloadDeserializer extends JsonDeserializer<SetupComponentPublicKeysResponsePayload> {
	@Override
	public SetupComponentPublicKeysResponsePayload deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext) throws IOException {
		final ObjectMapper mapper = DomainObjectMapper.getNewInstance();

		final JsonNode node = mapper.readTree(jsonParser);
		final int nodeId = node.get("nodeId").asInt();
		final String electionEventId = mapper.readValue(node.get("electionEventId").toString(), String.class);

		return new SetupComponentPublicKeysResponsePayload(nodeId, electionEventId);
	}
}
