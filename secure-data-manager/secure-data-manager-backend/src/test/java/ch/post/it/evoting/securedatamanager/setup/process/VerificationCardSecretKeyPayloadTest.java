/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("A VerificationCardSecretKey")
class VerificationCardSecretKeyPayloadTest {

	private static final int BOUND = 12;

	private static final ObjectMapper mapper = DomainObjectMapper.getNewInstance();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final SecureRandom srand = new SecureRandom();
	private static final int SIZE = srand.nextInt(BOUND) + 1;
	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_SET_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_ID = uuidGenerator.generate();

	private static VerificationCardSecretKeyPayload verificationCardSecretKeyPayload;
	private static ObjectNode rootNode;

	@BeforeAll
	static void setupAll() throws JsonProcessingException {
		final ImmutableList<VerificationCardSecretKey> verificationCardSecretKeys = ImmutableList.of(
				new VerificationCardSecretKey(VERIFICATION_CARD_ID, SerializationUtils.getPrivateKey()));
		verificationCardSecretKeyPayload = new VerificationCardSecretKeyPayload(SerializationUtils.getGqGroup(), ELECTION_EVENT_ID,
				VERIFICATION_CARD_SET_ID, verificationCardSecretKeys);

		// Create expected Json.
		rootNode = mapper.createObjectNode();
		rootNode.set("encryptionGroup", mapper.readTree(mapper.writeValueAsString(SerializationUtils.getGqGroup())));
		rootNode.set("electionEventId", mapper.readTree(mapper.writeValueAsString(ELECTION_EVENT_ID)));
		rootNode.set("verificationCardSetId", mapper.readTree(mapper.writeValueAsString(VERIFICATION_CARD_SET_ID)));

		final ObjectNode verificationCardSecretKeyNode = mapper.createObjectNode();
		verificationCardSecretKeyNode.set("verificationCardId", mapper.readTree(mapper.writeValueAsString(VERIFICATION_CARD_ID)));
		verificationCardSecretKeyNode.set("privateKey", mapper.readTree(mapper.writeValueAsString(SerializationUtils.getPrivateKey())));

		final ArrayNode verificationCardSecretKeyArray = mapper.createArrayNode();
		verificationCardSecretKeyArray.add(verificationCardSecretKeyNode);
		rootNode.set("verificationCardSecretKeys", verificationCardSecretKeyArray);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializePayload() throws JsonProcessingException {
		final String serializedPayload = mapper.writeValueAsString(verificationCardSecretKeyPayload);
		assertEquals(rootNode.toString(), serializedPayload);
	}

	@Test
	@DisplayName("deserialized gives expected payload")
	void deserializePayload() throws IOException {
		final VerificationCardSecretKeyPayload deserializedPayload = mapper.readValue(rootNode.toString(), VerificationCardSecretKeyPayload.class);
		assertEquals(verificationCardSecretKeyPayload, deserializedPayload);
	}

	@Test
	@DisplayName("serialized then deserialized gives original payload")
	void cycle() throws IOException {
		final VerificationCardSecretKeyPayload deserializedPayload = mapper
				.readValue(mapper.writeValueAsString(verificationCardSecretKeyPayload), VerificationCardSecretKeyPayload.class);

		assertEquals(verificationCardSecretKeyPayload, deserializedPayload);
	}

	@Nested
	@DisplayName("constructed with")
	class CheckConstructor extends TestGroupSetup {
		private final ImmutableList<VerificationCardSecretKey> verificationCardSecretKeys = ImmutableList.of(
				new VerificationCardSecretKey(VERIFICATION_CARD_ID,
						elGamalGenerator.genRandomPrivateKey(SIZE)));

		@Test
		@DisplayName("any null input throws a NullPointerException.")
		void checksNulls() {

			assertAll(
					() -> assertThrows(NullPointerException.class, () ->
							new VerificationCardSecretKeyPayload(null, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, verificationCardSecretKeys)),
					() -> assertThrows(NullPointerException.class, () ->
							new VerificationCardSecretKeyPayload(gqGroup, null, VERIFICATION_CARD_SET_ID, verificationCardSecretKeys)),
					() -> assertThrows(NullPointerException.class, () ->
							new VerificationCardSecretKeyPayload(gqGroup, ELECTION_EVENT_ID, null, verificationCardSecretKeys))
			);
		}

		@Test
		@DisplayName("any invalid input throws a FailedValidationException.")
		void checksInvalidValues() {

			final String invalidElectionEventId = "invalidElectionEventId";
			final String invalidVerificationCardSetId = "invalidVerificationCardSetId";

			assertAll(
					() -> assertThrows(FailedValidationException.class,
							() -> new VerificationCardSecretKeyPayload(gqGroup, invalidElectionEventId, VERIFICATION_CARD_SET_ID,
									verificationCardSecretKeys
							)),
					() -> assertThrows(FailedValidationException.class, () ->
							new VerificationCardSecretKeyPayload(gqGroup, ELECTION_EVENT_ID, invalidVerificationCardSetId, verificationCardSecretKeys
							))
			);
		}

		@Test
		@DisplayName("inputs with different groups throws an IllegalArgumentException.")
		void checksDifferentGroup() {
			final ImmutableList<VerificationCardSecretKey> privateKeyDifferentGroup = ImmutableList.of(
					new VerificationCardSecretKey(VERIFICATION_CARD_ID, otherGroupElGamalGenerator.genRandomPrivateKey(SIZE)));

			assertAll(
					() -> {
						final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () ->
								new VerificationCardSecretKeyPayload(gqGroup, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, privateKeyDifferentGroup));
						assertEquals("The verification card secret keys' group must be of same order as the encryption group.",
								illegalArgumentException.getMessage());
					},

					() -> {
						final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () ->
								new VerificationCardSecretKeyPayload(otherGqGroup, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID,
										verificationCardSecretKeys));
						assertEquals("The verification card secret keys' group must be of same order as the encryption group.",
								illegalArgumentException.getMessage());
					}
			);
		}
	}
}
