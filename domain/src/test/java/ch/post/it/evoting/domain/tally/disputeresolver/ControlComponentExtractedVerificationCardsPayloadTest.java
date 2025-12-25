/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally.disputeresolver;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
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

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedVerificationCardsPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("A controlComponentExtractedVerificationCardsPayload")
class ControlComponentExtractedVerificationCardsPayloadTest {

	private static final ObjectMapper mapper = DomainObjectMapper.getNewInstance();

	private static ControlComponentExtractedVerificationCardsPayload controlComponentExtractedVerificationCardsPayload;
	private static String electionEventId;
	private static int nodeId;
	private static GqGroup encryptionGroup;
	private static ImmutableList<ExtractedVerificationCard> extractedVerificationCards;
	private static ObjectNode rootNode;

	@BeforeAll
	static void setupAll() throws JsonProcessingException {

		// Create payload.
		final ControlComponentExtractedVerificationCardsPayloadGenerator generator = new ControlComponentExtractedVerificationCardsPayloadGenerator();
		controlComponentExtractedVerificationCardsPayload = generator.generate().getLast();
		encryptionGroup = controlComponentExtractedVerificationCardsPayload.getEncryptionGroup();
		electionEventId = controlComponentExtractedVerificationCardsPayload.getElectionEventId();
		nodeId = controlComponentExtractedVerificationCardsPayload.getNodeId();
		extractedVerificationCards = controlComponentExtractedVerificationCardsPayload.getExtractedVerificationCards();
		final CryptoPrimitivesSignature signature = controlComponentExtractedVerificationCardsPayload.getSignature();

		// Create expected Json.
		rootNode = mapper.createObjectNode();
		rootNode.set("encryptionGroup", mapper.readTree(mapper.writeValueAsString(encryptionGroup)));
		rootNode.set("electionEventId", mapper.readTree(mapper.writeValueAsString(electionEventId)));
		rootNode.set("nodeId", mapper.readTree(mapper.writeValueAsString(nodeId)));
		rootNode.set("extractedVerificationCards", mapper.readTree(mapper.writeValueAsString(extractedVerificationCards)));

		rootNode.set("signature", SerializationUtils.createSignatureNode(signature));
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializePayload() throws JsonProcessingException {
		final String serializedPayload = mapper.writeValueAsString(controlComponentExtractedVerificationCardsPayload);
		assertEquals(rootNode.toString(), serializedPayload);
	}

	@Test
	@DisplayName("deserialized gives expected payload")
	void deserializePayload() throws IOException {
		final ControlComponentExtractedVerificationCardsPayload deserializedPayload = mapper.readValue(rootNode.toString(),
				ControlComponentExtractedVerificationCardsPayload.class);
		assertEquals(controlComponentExtractedVerificationCardsPayload, deserializedPayload);
	}

	@Test
	@DisplayName("serialized then deserialized gives original payload")
	void cycle() throws IOException {
		final ControlComponentExtractedVerificationCardsPayload deserializedPayload =
				mapper.readValue(mapper.writeValueAsString(controlComponentExtractedVerificationCardsPayload),
						ControlComponentExtractedVerificationCardsPayload.class);

		assertEquals(controlComponentExtractedVerificationCardsPayload, deserializedPayload);
	}

	@Test
	@DisplayName("constructed with invalid fields throws an exception")
	void testInvalidFields() {
		assertAll(
				() -> assertThrows(NullPointerException.class,
						() -> new ControlComponentExtractedVerificationCardsPayload(null, electionEventId, nodeId, extractedVerificationCards)),
				() -> assertThrows(FailedValidationException.class,
						() -> new ControlComponentExtractedVerificationCardsPayload(encryptionGroup, "invalidElectionEventId", nodeId,
								extractedVerificationCards)),
				() -> assertThrows(NullPointerException.class,
						() -> new ControlComponentExtractedVerificationCardsPayload(encryptionGroup, null, nodeId, extractedVerificationCards)),
				() -> assertThrows(NullPointerException.class,
						() -> new ControlComponentExtractedVerificationCardsPayload(encryptionGroup, electionEventId, nodeId, null)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> new ControlComponentExtractedVerificationCardsPayload(encryptionGroup, electionEventId, -1,
								extractedVerificationCards)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> new ControlComponentExtractedVerificationCardsPayload(encryptionGroup, electionEventId, 5, extractedVerificationCards))
		);
	}

	@Test
	@DisplayName("constructed with duplicated verification card ids throws an exception")
	void duplicatedConfirmedVerificationCardIdsThrows() {
		final ImmutableList<ExtractedVerificationCard> duplicatedVerificationCardIds = extractedVerificationCards.stream()
				.map(extractedVerificationCard -> new ExtractedVerificationCard(electionEventId, extractedVerificationCard.verificationCardSetId(),
						extractedVerificationCard.encryptedVote(), extractedVerificationCard.hashedLongVoteCastReturnCodeShares()))
				.collect(toImmutableList());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ControlComponentExtractedVerificationCardsPayload(encryptionGroup, electionEventId, nodeId, duplicatedVerificationCardIds));

		final String expected = "The verification card ids must be unique.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("constructed with different encryption groups throws an exception")
	void differentEncryptionGroupsThrows() {
		final GqGroup otherEncryptionGroup = GroupTestData.getGroupP59();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ControlComponentExtractedVerificationCardsPayload(otherEncryptionGroup, electionEventId, nodeId,
						extractedVerificationCards));

		final String expected = "The encrypted vote's group must be equal to the encryption group.";
		assertEquals(expected, exception.getMessage());
	}
}
