/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally.disputeresolver;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.domain.generators.ControlComponentExtractedElectionEventPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

@DisplayName("A controlComponentExtractedElectionEventPayload")
class ControlComponentExtractedElectionEventPayloadTest {

	private static final ObjectMapper mapper = DomainObjectMapper.getNewInstance();

	private static ControlComponentExtractedElectionEventPayload controlComponentExtractedElectionEventPayload;
	private static int nodeId;
	private static ExtractedElectionEvent extractedElectionEvent;
	private static ObjectNode rootNode;

	@BeforeAll
	static void setupAll() throws JsonProcessingException {

		// Create payload.
		final ControlComponentExtractedElectionEventPayloadGenerator generator = new ControlComponentExtractedElectionEventPayloadGenerator();
		controlComponentExtractedElectionEventPayload = generator.generate().getLast();
		nodeId = controlComponentExtractedElectionEventPayload.getNodeId();
		extractedElectionEvent = controlComponentExtractedElectionEventPayload.getExtractedElectionEvent();
		final CryptoPrimitivesSignature signature = controlComponentExtractedElectionEventPayload.getSignature();

		// Create expected Json.
		rootNode = mapper.createObjectNode();
		rootNode.set("nodeId", mapper.readTree(mapper.writeValueAsString(nodeId)));
		rootNode.set("extractedElectionEvent", mapper.readTree(mapper.writeValueAsString(extractedElectionEvent)));

		rootNode.set("signature", SerializationUtils.createSignatureNode(signature));
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializePayload() throws JsonProcessingException {
		final String serializedPayload = mapper.writeValueAsString(controlComponentExtractedElectionEventPayload);
		assertEquals(rootNode.toString(), serializedPayload);
	}

	@Test
	@DisplayName("deserialized gives expected payload")
	void deserializePayload() throws IOException {
		final ControlComponentExtractedElectionEventPayload deserializedPayload = mapper.readValue(rootNode.toString(),
				ControlComponentExtractedElectionEventPayload.class);
		assertEquals(controlComponentExtractedElectionEventPayload, deserializedPayload);
	}

	@Test
	@DisplayName("serialized then deserialized gives original payload")
	void cycle() throws IOException {
		final ControlComponentExtractedElectionEventPayload deserializedPayload =
				mapper.readValue(mapper.writeValueAsString(controlComponentExtractedElectionEventPayload),
						ControlComponentExtractedElectionEventPayload.class);

		assertEquals(controlComponentExtractedElectionEventPayload, deserializedPayload);
	}

	@Test
	@DisplayName("constructed with invalid fields throws an exception")
	void testInvalidFields() {
		assertAll(
				() -> assertThrows(IllegalArgumentException.class,
						() -> new ControlComponentExtractedElectionEventPayload(-1, extractedElectionEvent)),
				() -> assertThrows(NullPointerException.class,
						() -> new ControlComponentExtractedElectionEventPayload(nodeId, null))
		);
	}
}
