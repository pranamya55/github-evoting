/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally.disputeresolver;

import static ch.post.it.evoting.domain.voting.JsonSchemaConstants.CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_SCHEMA;
import static ch.post.it.evoting.evotinglibraries.domain.validations.JsonSchemaValidation.validate;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

public class ControlComponentExtractedElectionEventPayloadDeserializer extends JsonDeserializer<ControlComponentExtractedElectionEventPayload> {

	@Override
	public ControlComponentExtractedElectionEventPayload deserialize(final JsonParser parser, final DeserializationContext deserializationContext)
			throws IOException {

		final ObjectMapper mapper = (ObjectMapper) parser.getCodec();
		final JsonNode node = validate(mapper.readTree(parser), CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_SCHEMA);

		final int nodeId = mapper.readValue(node.get("nodeId").toString(), Integer.class);
		final ExtractedElectionEvent extractedElectionEvent = mapper.reader()
				.readValue(node.get("extractedElectionEvent").toString(), ExtractedElectionEvent.class);
		final CryptoPrimitivesSignature signature = mapper.reader()
				.readValue(node.get("signature").toString(), CryptoPrimitivesSignature.class);

		return new ControlComponentExtractedElectionEventPayload(nodeId, extractedElectionEvent, signature);
	}
}
