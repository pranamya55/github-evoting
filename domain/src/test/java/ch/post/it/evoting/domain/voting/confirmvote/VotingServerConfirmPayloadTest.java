/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.ConversionUtils.bigIntegerToBase64;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.domain.MapperSetUp;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

@DisplayName("VotingServerConfirmPayload")
class VotingServerConfirmPayloadTest extends MapperSetUp {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final byte[] randomBytes = new byte[10];

	private GqGroup encryptionGroup;
	private ConfirmationKey confirmationKey;
	private VotingServerConfirmPayload votingServerConfirmPayload;
	private ObjectNode rootNode;

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();
		final String verificationCardId = uuidGenerator.generate();
		final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);

		encryptionGroup = GroupTestData.getGqGroup();
		final GqElement element = new GqGroupGenerator(encryptionGroup).genMember();

		confirmationKey = new ConfirmationKey(contextIds, element);

		final int confirmationAttemptId = 0;

		// Generate random bytes for signature content and create payload signature.
		SECURE_RANDOM.nextBytes(randomBytes);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(new ImmutableByteArray(randomBytes));

		votingServerConfirmPayload = new VotingServerConfirmPayload(encryptionGroup, confirmationKey, confirmationAttemptId, signature);

		// Create expected json
		rootNode = mapper.createObjectNode();

		final JsonNode encryptionGroupNode = SerializationUtils.createEncryptionGroupNode(encryptionGroup);
		rootNode.set("encryptionGroup", encryptionGroupNode);

		final ObjectNode contextIdsNode = mapper.createObjectNode();
		contextIdsNode.put("electionEventId", electionEventId);
		contextIdsNode.put("verificationCardSetId", verificationCardSetId);
		contextIdsNode.put("verificationCardId", verificationCardId);

		final ObjectNode confirmationKeyNode = mapper.createObjectNode();
		confirmationKeyNode.set("contextIds", contextIdsNode);
		confirmationKeyNode.put("element", bigIntegerToBase64(confirmationKey.element().getValue()));
		rootNode.set("confirmationKey", confirmationKeyNode);

		rootNode.put("confirmationAttemptId", 0);

		final JsonNode signatureNode = SerializationUtils.createSignatureNode(signature);
		rootNode.set("signature", signatureNode);
	}

	@Test
	@DisplayName("construction with null parameters throws a NullPointerException")
	void constructWithNullParametersThrows() {
		final int confirmationAttemptId = 0;
		assertThrows(NullPointerException.class, () -> new VotingServerConfirmPayload(null, confirmationKey, confirmationAttemptId));
		assertThrows(NullPointerException.class, () -> new VotingServerConfirmPayload(encryptionGroup, null, confirmationAttemptId));
	}

	@Test
	@DisplayName("construction with confirmation key element not from encryption group throws IllegalArgumentException")
	void constructWithConfirmationKeyNotInEncryptionGroupThrows() {
		final int confirmationAttemptId = 0;
		final GqGroup differentEncryptionGroup = GroupTestData.getDifferentGqGroup(encryptionGroup);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VotingServerConfirmPayload(differentEncryptionGroup, confirmationKey, confirmationAttemptId));
		assertEquals("The confirmation key must be in the encryption group", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("serializing a ConfirmationKey gives the expected json")
	void serializeConfirmationKey() throws JsonProcessingException {
		final String serializedVotingServerConfirmPayload = mapper.writeValueAsString(votingServerConfirmPayload);
		assertEquals(rootNode.toString(), serializedVotingServerConfirmPayload);
	}

	@Test
	@DisplayName("deserializing a ConfirmationKey gives the expected ConfirmationKey")
	void deserializeConfirmationKey() throws IOException {
		final VotingServerConfirmPayload deserializedVotingServerConfirmPayload = mapper.readValue(rootNode.toString(),
				VotingServerConfirmPayload.class);
		assertEquals(votingServerConfirmPayload, deserializedVotingServerConfirmPayload);
	}

	@Test
	@DisplayName("serializing then deserializing a ConfirmationKey gives the original ConfirmationKey")
	void cycle() throws IOException {
		final String serializedVotingServerConfirmPayload = mapper.writeValueAsString(votingServerConfirmPayload);
		final VotingServerConfirmPayload deserializedVotingServerConfirmPayload = mapper.readValue(serializedVotingServerConfirmPayload,
				VotingServerConfirmPayload.class);
		assertEquals(votingServerConfirmPayload, deserializedVotingServerConfirmPayload);
	}

	@Test
	@DisplayName("equals returns expected value")
	void testEquals() {
		final int confirmationAttemptId = 0;
		final GqGroupGenerator gqGroupGenerator = new GqGroupGenerator(encryptionGroup);
		final GqElement element1 = gqGroupGenerator.genMember();
		final ContextIds contextIds = confirmationKey.contextIds();
		final ConfirmationKey confirmationKey1 = new ConfirmationKey(contextIds, element1);
		final VotingServerConfirmPayload payload1 = new VotingServerConfirmPayload(encryptionGroup, confirmationKey1, confirmationAttemptId);

		final GqElement element2 = gqGroupGenerator.otherElement(element1);
		final ConfirmationKey confirmationKey2 = new ConfirmationKey(contextIds, element2);
		final VotingServerConfirmPayload payload2 = new VotingServerConfirmPayload(encryptionGroup, confirmationKey2, confirmationAttemptId);

		final VotingServerConfirmPayload payload3 = new VotingServerConfirmPayload(encryptionGroup, confirmationKey1, confirmationAttemptId);

		assertNotEquals(null, payload1);
		assertNotEquals(payload1, payload2);
		assertEquals(payload1, payload3);
	}

	@Test
	@DisplayName("hashCode of equal VotingServerConfirmPayloads is equal")
	void testHashCode() {
		final int confirmationAttemptId = 0;
		final VotingServerConfirmPayload payload = new VotingServerConfirmPayload(votingServerConfirmPayload.getEncryptionGroup(), confirmationKey,
				confirmationAttemptId, votingServerConfirmPayload.getSignature());

		assertEquals(votingServerConfirmPayload.hashCode(), payload.hashCode());
	}
}