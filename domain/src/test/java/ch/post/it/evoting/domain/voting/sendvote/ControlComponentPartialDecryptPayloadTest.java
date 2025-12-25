/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.sendvote;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.domain.MapperSetUp;
import ch.post.it.evoting.domain.generators.ControlComponentPartialDecryptPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;

@DisplayName("A ControlComponentPartialDecryptPayload")
class ControlComponentPartialDecryptPayloadTest extends MapperSetUp {

	private static ObjectNode rootNode;
	private static ControlComponentPartialDecryptPayload controlComponentPartialDecryptPayload;

	@BeforeAll
	static void setUpAll() {
		// Create payload.
		final ControlComponentPartialDecryptPayloadGenerator controlComponentPartialDecryptPayloadGenerator = new ControlComponentPartialDecryptPayloadGenerator();
		controlComponentPartialDecryptPayload = controlComponentPartialDecryptPayloadGenerator.generate().getFirst();
		final PartiallyDecryptedEncryptedPCC partiallyDecryptedEncryptedPCC = controlComponentPartialDecryptPayload.getPartiallyDecryptedEncryptedPCC();
		final ContextIds contextIds = partiallyDecryptedEncryptedPCC.contextIds();

		// Create expected Json.
		rootNode = mapper.createObjectNode();

		final JsonNode encryptionGroupNode = SerializationUtils.createEncryptionGroupNode(controlComponentPartialDecryptPayload.getEncryptionGroup());
		rootNode.set("encryptionGroup", encryptionGroupNode);

		final ObjectNode contextIdsNode = mapper.createObjectNode();
		contextIdsNode.put("electionEventId", contextIds.electionEventId());
		contextIdsNode.put("verificationCardSetId", contextIds.verificationCardSetId());
		contextIdsNode.put("verificationCardId", contextIds.verificationCardId());

		final ObjectNode partiallyDecryptedEncryptedPCCNode = mapper.createObjectNode();
		final ArrayNode exponentiationProofsNode = SerializationUtils.createExponentiationProofsNode(
				partiallyDecryptedEncryptedPCC.exponentiationProofs());
		final ArrayNode exponentiatedGammasNode = SerializationUtils.createGqGroupVectorNode(partiallyDecryptedEncryptedPCC.exponentiatedGammas());
		partiallyDecryptedEncryptedPCCNode.set("contextIds", contextIdsNode);
		partiallyDecryptedEncryptedPCCNode.put("nodeId", controlComponentPartialDecryptPayload.getNodeId());
		partiallyDecryptedEncryptedPCCNode.set("exponentiatedGammas", exponentiatedGammasNode);
		partiallyDecryptedEncryptedPCCNode.set("exponentiationProofs", exponentiationProofsNode);

		rootNode.set("partiallyDecryptedEncryptedPCC", partiallyDecryptedEncryptedPCCNode);

		final JsonNode signatureNode = SerializationUtils.createSignatureNode(controlComponentPartialDecryptPayload.getSignature());
		rootNode.set("signature", signatureNode);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializePayload() throws JsonProcessingException {
		final String serializedPayload = mapper.writeValueAsString(controlComponentPartialDecryptPayload);

		assertEquals(rootNode.toString(), serializedPayload);
	}

	@Test
	@DisplayName("deserialized gives expected payload")
	void deserializePayload() throws JsonProcessingException {
		final ControlComponentPartialDecryptPayload deserializedPayload = mapper.readValue(rootNode.toString(),
				ControlComponentPartialDecryptPayload.class);

		assertEquals(controlComponentPartialDecryptPayload, deserializedPayload);
	}

	@Test
	@DisplayName("serialized then deserialized gives original payload")
	void cycle() throws JsonProcessingException {
		final ControlComponentPartialDecryptPayload deserializedPayload = mapper.readValue(
				mapper.writeValueAsString(controlComponentPartialDecryptPayload), ControlComponentPartialDecryptPayload.class);

		assertEquals(controlComponentPartialDecryptPayload, deserializedPayload);
	}

}
