/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

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

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("An electoralBoardHashesPayload")
class ElectoralBoardHashesPayloadTest {

	private static final ObjectMapper mapper = DomainObjectMapper.getNewInstance();
	private static final Hash hash = HashFactory.createHash();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();

	private static ElectoralBoardHashesPayload electoralBoardHashesPayload;
	private static ImmutableList<ImmutableByteArray> hashes;
	private static ObjectNode rootNode;

	@BeforeAll
	static void setupAll() throws JsonProcessingException {

		// Create payload.
		hashes = ImmutableList.of(ImmutableByteArray.of((byte) 1, (byte) 2), ImmutableByteArray.of((byte) 3, (byte) 4));
		electoralBoardHashesPayload = new ElectoralBoardHashesPayload(ELECTION_EVENT_ID, hashes);

		final ImmutableByteArray payloadHash = hash.recursiveHash(electoralBoardHashesPayload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		electoralBoardHashesPayload.setSignature(signature);

		// Create expected Json.
		rootNode = mapper.createObjectNode();
		rootNode.set("electionEventId", mapper.readTree(mapper.writeValueAsString(ELECTION_EVENT_ID)));
		rootNode.set("electoralBoardHashes", mapper.readTree(mapper.writeValueAsString(hashes)));
		rootNode.set("signature", SerializationUtils.createSignatureNode(signature));
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializePayload() throws JsonProcessingException {
		final String serializedPayload = mapper.writeValueAsString(electoralBoardHashesPayload);
		assertEquals(rootNode.toString(), serializedPayload);
	}

	@Test
	@DisplayName("deserialized gives expected payload")
	void deserializePayload() throws IOException {
		final ElectoralBoardHashesPayload deserializedPayload = mapper.readValue(rootNode.toString(), ElectoralBoardHashesPayload.class);
		assertEquals(electoralBoardHashesPayload, deserializedPayload);
		assertEquals(ELECTION_EVENT_ID, electoralBoardHashesPayload.getElectionEventId());
	}

	@Test
	@DisplayName("serialized then deserialized gives original payload")
	void cycle() throws IOException {
		final ElectoralBoardHashesPayload deserializedPayload =
				mapper.readValue(mapper.writeValueAsString(electoralBoardHashesPayload), ElectoralBoardHashesPayload.class);

		assertEquals(electoralBoardHashesPayload, deserializedPayload);
	}

	@Test
	@DisplayName("constructed with invalid fields throws an exception")
	void testInvalidElectoralBoardHashes() {
		assertAll(
				() -> assertThrows(NullPointerException.class,
						() -> new ElectoralBoardHashesPayload(null, hashes)),
				() -> assertThrows(FailedValidationException.class,
						() -> new ElectoralBoardHashesPayload("invalidElectionEventId", hashes)),
				() -> assertThrows(NullPointerException.class,
						() -> new ElectoralBoardHashesPayload(ELECTION_EVENT_ID, null))
		);
	}
}
