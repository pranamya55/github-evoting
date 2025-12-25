/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration.setupvoting;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("A longVoteCastReturnCodesAllowListResponsePayload")
class LongVoteCastReturnCodesAllowListResponsePayloadTest {

	private static final ObjectMapper mapper = DomainObjectMapper.getNewInstance();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final int NODE_ID = 1;
	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_SET_ID = uuidGenerator.generate();

	private static LongVoteCastReturnCodesAllowListResponsePayload longVoteCastReturnCodesAllowListResponsePayload;
	private static ObjectNode rootNode;

	@BeforeAll
	static void setupAll() throws JsonProcessingException {

		// Create payload.
		longVoteCastReturnCodesAllowListResponsePayload = new LongVoteCastReturnCodesAllowListResponsePayload(NODE_ID, ELECTION_EVENT_ID,
				VERIFICATION_CARD_SET_ID);

		// Create expected Json.
		rootNode = mapper.createObjectNode();
		rootNode.set("nodeId", mapper.readTree(mapper.writeValueAsString(NODE_ID)));
		rootNode.set("electionEventId", mapper.readTree(mapper.writeValueAsString(ELECTION_EVENT_ID)));
		rootNode.set("verificationCardSetId", mapper.readTree(mapper.writeValueAsString(VERIFICATION_CARD_SET_ID)));
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializePayload() throws JsonProcessingException {
		final String serializedPayload = mapper.writeValueAsString(longVoteCastReturnCodesAllowListResponsePayload);
		assertEquals(rootNode.toString(), serializedPayload);
	}

	@Test
	@DisplayName("deserialized gives expected result")
	void deserializePayload() throws IOException {
		final LongVoteCastReturnCodesAllowListResponsePayload deserializedPayload = mapper.readValue(rootNode.toString(),
				LongVoteCastReturnCodesAllowListResponsePayload.class);
		assertEquals(longVoteCastReturnCodesAllowListResponsePayload, deserializedPayload);
		assertEquals(NODE_ID, longVoteCastReturnCodesAllowListResponsePayload.nodeId());
		assertEquals(ELECTION_EVENT_ID, longVoteCastReturnCodesAllowListResponsePayload.electionEventId());
		assertEquals(VERIFICATION_CARD_SET_ID, longVoteCastReturnCodesAllowListResponsePayload.verificationCardSetId());
	}

	@Test
	@DisplayName("serialized then deserialized gives original object")
	void cycle() throws IOException {
		final LongVoteCastReturnCodesAllowListResponsePayload deserializedPayload = mapper.readValue(
				mapper.writeValueAsString(longVoteCastReturnCodesAllowListResponsePayload), LongVoteCastReturnCodesAllowListResponsePayload.class);

		assertEquals(longVoteCastReturnCodesAllowListResponsePayload, deserializedPayload);
	}

	@Test
	@DisplayName("constructed with invalid fields throws an exception")
	void testInvalidElectionEventContext() {
		assertAll(
				() -> assertThrows(NullPointerException.class,
						() -> new LongVoteCastReturnCodesAllowListResponsePayload(NODE_ID, null, VERIFICATION_CARD_SET_ID)),
				() -> assertThrows(FailedValidationException.class,
						() -> new LongVoteCastReturnCodesAllowListResponsePayload(NODE_ID, "invalidElectionEventId", VERIFICATION_CARD_SET_ID)),
				() -> assertThrows(NullPointerException.class,
						() -> new LongVoteCastReturnCodesAllowListResponsePayload(NODE_ID, ELECTION_EVENT_ID, null)),
				() -> assertThrows(FailedValidationException.class,
						() -> new LongVoteCastReturnCodesAllowListResponsePayload(NODE_ID, ELECTION_EVENT_ID, "invalidVerificationCardSetId")));
	}

	@Test
	@DisplayName("equals give expected result")
	void testEquality() {
		final LongVoteCastReturnCodesAllowListResponsePayload same = new LongVoteCastReturnCodesAllowListResponsePayload(NODE_ID, ELECTION_EVENT_ID,
				VERIFICATION_CARD_SET_ID);
		final LongVoteCastReturnCodesAllowListResponsePayload different = new LongVoteCastReturnCodesAllowListResponsePayload(NODE_ID,
				uuidGenerator.generate(), VERIFICATION_CARD_SET_ID);

		assertAll(
				() -> assertEquals(longVoteCastReturnCodesAllowListResponsePayload, longVoteCastReturnCodesAllowListResponsePayload),
				() -> assertEquals(longVoteCastReturnCodesAllowListResponsePayload, same),
				() -> assertNotEquals(longVoteCastReturnCodesAllowListResponsePayload, different),
				() -> assertNotEquals(null, longVoteCastReturnCodesAllowListResponsePayload),
				() -> assertNotEquals(1L, longVoteCastReturnCodesAllowListResponsePayload),
				() -> assertEquals(longVoteCastReturnCodesAllowListResponsePayload.hashCode(), same.hashCode())
		);
	}
}
