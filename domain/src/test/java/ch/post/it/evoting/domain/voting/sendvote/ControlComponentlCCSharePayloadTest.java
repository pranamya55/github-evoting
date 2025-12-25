/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.sendvote;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.domain.MapperSetUp;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

@DisplayName("A ControlComponentlCCSharePayload")
class ControlComponentlCCSharePayloadTest extends MapperSetUp {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_SET_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_ID = uuidGenerator.generate();
	private static final int NODE_ID = 1;
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final byte[] randomBytes = new byte[10];

	private static ObjectNode rootNode;
	private static ControlComponentlCCSharePayload controlComponentLCCSharePayload;

	@BeforeAll
	static void setUpAll() {
		final GqGroup gqGroup = SerializationUtils.getGqGroup();
		final GroupVector<GqElement, GqGroup> longChoiceCodes = SerializationUtils.getLongChoiceCodes(2);

		// Generate random bytes for signature content and create payload signature.
		secureRandom.nextBytes(randomBytes);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(new ImmutableByteArray(randomBytes));

		// Create payload.
		final LongChoiceReturnCodeShare payload = new LongChoiceReturnCodeShare(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, VERIFICATION_CARD_ID,
				NODE_ID,
				longChoiceCodes);

		controlComponentLCCSharePayload = new ControlComponentlCCSharePayload(gqGroup, payload, signature);

		// Create expected Json.
		rootNode = mapper.createObjectNode();

		final JsonNode encryptionGroupNode = SerializationUtils.createEncryptionGroupNode(gqGroup);
		rootNode.set("encryptionGroup", encryptionGroupNode);

		final ObjectNode payloadNode = mapper.createObjectNode();
		payloadNode.put("electionEventId", ELECTION_EVENT_ID);
		payloadNode.put("verificationCardSetId", VERIFICATION_CARD_SET_ID);
		payloadNode.put("verificationCardId", VERIFICATION_CARD_ID);
		payloadNode.put("nodeId", NODE_ID);

		final ArrayNode electionPublicKeyNode = SerializationUtils.createGqGroupVectorNode(longChoiceCodes);
		payloadNode.set("longChoiceReturnCodeShare", electionPublicKeyNode);

		rootNode.set("longChoiceReturnCodeShare", payloadNode);

		final JsonNode signatureNode = SerializationUtils.createSignatureNode(signature);
		rootNode.set("signature", signatureNode);
	}

	@Test
	@DisplayName("longChoiceReturnCodeShares has same GqGroup order as the encryptionGroup")
	void checkGroups() {
		assertEquals(controlComponentLCCSharePayload.getLongChoiceReturnCodeShare().longChoiceReturnCodeShare().getGroup(),
				controlComponentLCCSharePayload.getEncryptionGroup());
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializePayload() throws JsonProcessingException {
		final String serializedPayload = mapper.writeValueAsString(controlComponentLCCSharePayload);

		assertEquals(rootNode.toString(), serializedPayload);
	}

	@Test
	@DisplayName("deserialized gives expected payload")
	void deserializePayload() throws JsonProcessingException {
		final ControlComponentlCCSharePayload deserializedPayload = mapper.readValue(rootNode.toString(), ControlComponentlCCSharePayload.class);

		assertEquals(controlComponentLCCSharePayload, deserializedPayload);
	}

	@Test
	@DisplayName("serialized then deserialized gives original payload")
	void cycle() throws JsonProcessingException {
		final ControlComponentlCCSharePayload deserializedPayload = mapper
				.readValue(mapper.writeValueAsString(controlComponentLCCSharePayload), ControlComponentlCCSharePayload.class);

		assertEquals(controlComponentLCCSharePayload, deserializedPayload);
	}

}
