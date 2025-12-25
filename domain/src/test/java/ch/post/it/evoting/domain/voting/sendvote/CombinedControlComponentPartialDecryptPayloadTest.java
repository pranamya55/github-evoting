/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.sendvote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.domain.MapperSetUp;
import ch.post.it.evoting.domain.generators.ControlComponentPartialDecryptPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;

@DisplayName("A ControlComponentPartialDecryptPayload")
class CombinedControlComponentPartialDecryptPayloadTest extends MapperSetUp {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final int NUMBER_OF_SELECTIONS = 10;

	private static ObjectNode rootNode;
	private static ControlComponentPartialDecryptPayloadGenerator payloadGenerator;
	private static CombinedControlComponentPartialDecryptPayload combinedControlComponentPartialDecryptPayload;
	private static GqGroup gqGroup;
	private static ContextIds contextIds;

	@BeforeAll
	static void setUpAll() {
		payloadGenerator = new ControlComponentPartialDecryptPayloadGenerator();

		// Create payload.
		final ImmutableList<ControlComponentPartialDecryptPayload> payloads = payloadGenerator.generate();
		gqGroup = payloads.getFirst().getEncryptionGroup();
		contextIds = payloads.getFirst().getPartiallyDecryptedEncryptedPCC().contextIds();

		// Create combined payloads.
		combinedControlComponentPartialDecryptPayload = new CombinedControlComponentPartialDecryptPayload(payloads);

		// Create expected Json.

		// Combined node.
		rootNode = mapper.createObjectNode();

		// Payload node.
		final ArrayNode payloadsNode = mapper.createArrayNode();
		for (int i = 0; i < 4; i++) {
			final ObjectNode payloadNode = mapper.createObjectNode();
			final JsonNode encryptionGroupNode = SerializationUtils.createEncryptionGroupNode(gqGroup);
			payloadNode.set("encryptionGroup", encryptionGroupNode);

			final ObjectNode contextIdsNode = mapper.createObjectNode();
			contextIdsNode.put("electionEventId", contextIds.electionEventId());
			contextIdsNode.put("verificationCardSetId", contextIds.verificationCardSetId());
			contextIdsNode.put("verificationCardId", contextIds.verificationCardId());

			final ObjectNode partiallyDecryptedEncryptedPCCNode = mapper.createObjectNode();
			final PartiallyDecryptedEncryptedPCC partiallyDecryptedEncryptedPCC = payloads.get(i).getPartiallyDecryptedEncryptedPCC();
			final ArrayNode exponentiationProofsNode = SerializationUtils.createExponentiationProofsNode(
					partiallyDecryptedEncryptedPCC.exponentiationProofs());
			final ArrayNode exponentiatedGammasNode = SerializationUtils.createGqGroupVectorNode(
					partiallyDecryptedEncryptedPCC.exponentiatedGammas());
			partiallyDecryptedEncryptedPCCNode.set("contextIds", contextIdsNode);
			partiallyDecryptedEncryptedPCCNode.put("nodeId", i + 1);
			partiallyDecryptedEncryptedPCCNode.set("exponentiatedGammas", exponentiatedGammasNode);
			partiallyDecryptedEncryptedPCCNode.set("exponentiationProofs", exponentiationProofsNode);

			payloadNode.set("partiallyDecryptedEncryptedPCC", partiallyDecryptedEncryptedPCCNode);

			final JsonNode payloadSignatureNode = SerializationUtils.createSignatureNode(payloads.get(i).getSignature());
			payloadNode.set("signature", payloadSignatureNode);
			payloadsNode.add(payloadNode);
		}
		rootNode.set("controlComponentPartialDecryptPayloads", payloadsNode);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializePayload() throws JsonProcessingException {
		final String serializedPayload = mapper.writeValueAsString(combinedControlComponentPartialDecryptPayload);

		assertEquals(rootNode.toString(), serializedPayload);
	}

	@Test
	@DisplayName("deserialized gives expected payload")
	void deserializePayload() throws JsonProcessingException {
		final CombinedControlComponentPartialDecryptPayload deserializedPayload = mapper.readValue(rootNode.toString(),
				CombinedControlComponentPartialDecryptPayload.class);

		assertEquals(combinedControlComponentPartialDecryptPayload, deserializedPayload);
	}

	@Test
	@DisplayName("serialized then deserialized gives original payload")
	void cycle() throws JsonProcessingException {
		final CombinedControlComponentPartialDecryptPayload deserializedPayload = mapper.readValue(
				mapper.writeValueAsString(combinedControlComponentPartialDecryptPayload), CombinedControlComponentPartialDecryptPayload.class);

		assertEquals(combinedControlComponentPartialDecryptPayload, deserializedPayload);
	}

	@Test
	@DisplayName("not enough contributions throws IllegalArgumentException")
	void notEnoughContributions() {
		final ImmutableList<ControlComponentPartialDecryptPayload> emptyList = ImmutableList.emptyList();

		final IllegalArgumentException exceptionEmptyList = assertThrows(IllegalArgumentException.class,
				() -> new CombinedControlComponentPartialDecryptPayload(emptyList));
		assertEquals("The list of Control Component payloads must not be empty.",
				Throwables.getRootCause(exceptionEmptyList).getMessage());

		final ImmutableList<ControlComponentPartialDecryptPayload> payloads = payloadGenerator.generate().subList(0, 3);

		final IllegalArgumentException exceptionTooSmallList = assertThrows(IllegalArgumentException.class,
				() -> new CombinedControlComponentPartialDecryptPayload(payloads));
		assertEquals(String.format("There must be exactly %s %ss.", ControlComponentNode.ids().size(), payloads.get(0).getClass().getSimpleName()),
				Throwables.getRootCause(exceptionTooSmallList).getMessage());
	}

	@Test
	@DisplayName("inconsistent groups throws IllegalArgumentException")
	void inconsistentGroups() {
		final ImmutableList<ControlComponentPartialDecryptPayload> payloads = payloadGenerator.generate().subList(0, 3);

		final GqGroup otherGqGroup = GroupTestData.getGroupP59();
		final ControlComponentPartialDecryptPayloadGenerator otherGroupPayloadGenerator = new ControlComponentPartialDecryptPayloadGenerator(
				otherGqGroup);

		final ImmutableList<ControlComponentPartialDecryptPayload> inconsistent = payloads.append(
				otherGroupPayloadGenerator.generate(contextIds, 4, NUMBER_OF_SELECTIONS));

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new CombinedControlComponentPartialDecryptPayload(inconsistent));
		assertEquals("All ControlComponentPartialDecryptPayloads must have the same group.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("inconsistent contextIds throws IllegalArgumentException")
	void inconsistentContextIds() {
		final ImmutableList<ControlComponentPartialDecryptPayload> payloads = payloadGenerator.generate().subList(0, 3);
		final String otherVerificationCardId = uuidGenerator.generate();
		final ContextIds otherContextIds = new ContextIds(contextIds.electionEventId(), contextIds.verificationCardSetId(), otherVerificationCardId);

		final ImmutableList<ControlComponentPartialDecryptPayload> inconsistent = payloads.append(
				payloadGenerator.generate(otherContextIds, 4, NUMBER_OF_SELECTIONS));

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new CombinedControlComponentPartialDecryptPayload(inconsistent));
		assertEquals("All control component partial decrypt payloads must have the same contextIds.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("inconsistent nodeIds throws IllegalArgumentException")
	void inconsistentNodeIds() {
		final ImmutableList<ControlComponentPartialDecryptPayload> payloads = payloadGenerator.generate().subList(0, 3);

		final ImmutableList<ControlComponentPartialDecryptPayload> inconsistent = payloads.append(
				payloadGenerator.generate(contextIds, 1, NUMBER_OF_SELECTIONS));

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new CombinedControlComponentPartialDecryptPayload(inconsistent));
		assertEquals("A ControlComponentPartialDecryptPayload for each node id must be present.",
				Throwables.getRootCause(exception).getMessage());
	}

}
