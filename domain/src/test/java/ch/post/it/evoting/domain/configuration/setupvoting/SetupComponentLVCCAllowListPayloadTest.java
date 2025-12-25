/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration.setupvoting;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Base64;

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
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("A setupComponentLVCCAllowListPayload")
class SetupComponentLVCCAllowListPayloadTest {

	private static final ObjectMapper mapper = DomainObjectMapper.getNewInstance();
	private static final Hash hash = HashFactory.createHash();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_SET_ID = uuidGenerator.generate();
	private static final ImmutableList<String> longVoteCastReturnCodesAllowList = ImmutableList.of(
			Base64.getEncoder().encodeToString(new byte[] { 1, 2 }));

	private static SetupComponentLVCCAllowListPayload setupComponentLVCCAllowListPayload;
	private static CryptoPrimitivesSignature signature;
	private static ObjectNode rootNode;

	@BeforeAll
	static void setupAll() throws JsonProcessingException {

		// Create payload.
		setupComponentLVCCAllowListPayload = new SetupComponentLVCCAllowListPayload(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID,
				longVoteCastReturnCodesAllowList);

		final ImmutableByteArray payloadHash = hash.recursiveHash(setupComponentLVCCAllowListPayload);

		signature = new CryptoPrimitivesSignature(payloadHash);
		setupComponentLVCCAllowListPayload.setSignature(signature);

		// Create expected Json.
		rootNode = mapper.createObjectNode();
		rootNode.set("electionEventId", mapper.readTree(mapper.writeValueAsString(ELECTION_EVENT_ID)));
		rootNode.set("verificationCardSetId", mapper.readTree(mapper.writeValueAsString(VERIFICATION_CARD_SET_ID)));
		rootNode.set("longVoteCastReturnCodesAllowList", mapper.readTree(mapper.writeValueAsString(longVoteCastReturnCodesAllowList)));
		rootNode.set("signature", mapper.readTree(mapper.writeValueAsString(signature)));
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializePayload() throws JsonProcessingException {
		final String serializedPayload = mapper.writeValueAsString(setupComponentLVCCAllowListPayload);
		assertEquals(rootNode.toString(), serializedPayload);
	}

	@Test
	@DisplayName("deserialized gives expected result")
	void deserializePayload() throws IOException {
		final SetupComponentLVCCAllowListPayload deserializedPayload = mapper.readValue(rootNode.toString(),
				SetupComponentLVCCAllowListPayload.class);
		assertEquals(setupComponentLVCCAllowListPayload, deserializedPayload);
		assertEquals(ELECTION_EVENT_ID, setupComponentLVCCAllowListPayload.getElectionEventId());
		assertEquals(longVoteCastReturnCodesAllowList, setupComponentLVCCAllowListPayload.getLongVoteCastReturnCodesAllowList());
		assertEquals(signature, setupComponentLVCCAllowListPayload.getSignature());
	}

	@Test
	@DisplayName("serialized then deserialized gives original object")
	void cycle() throws IOException {
		final SetupComponentLVCCAllowListPayload deserializedPayload = mapper.readValue(
				mapper.writeValueAsString(setupComponentLVCCAllowListPayload), SetupComponentLVCCAllowListPayload.class);

		assertEquals(setupComponentLVCCAllowListPayload, deserializedPayload);
	}

	@Test
	@DisplayName("constructed with invalid fields throws an exception")
	void testInvalidSetupComponentLVCCAllowList() {
		final ImmutableList<String> emptyAllowList = ImmutableList.emptyList();
		final ImmutableList<String> emptyStringInAllowList = ImmutableList.of("");
		final ImmutableList<String> whitespaceOnlyStringInAllowList = ImmutableList.of("    ");
		final ImmutableList<String> notBase64InAllowList = ImmutableList.of("not base64");

		assertAll(
				() -> assertThrows(NullPointerException.class,
						() -> new SetupComponentLVCCAllowListPayload(null, VERIFICATION_CARD_SET_ID, longVoteCastReturnCodesAllowList, signature)),
				() -> assertThrows(FailedValidationException.class,
						() -> new SetupComponentLVCCAllowListPayload("invalidElectionEventId", VERIFICATION_CARD_SET_ID,
								longVoteCastReturnCodesAllowList, signature)),
				() -> assertThrows(NullPointerException.class,
						() -> new SetupComponentLVCCAllowListPayload(ELECTION_EVENT_ID, null, longVoteCastReturnCodesAllowList, signature)),
				() -> assertThrows(FailedValidationException.class,
						() -> new SetupComponentLVCCAllowListPayload(ELECTION_EVENT_ID, "invalidVerificationCardSetId",
								longVoteCastReturnCodesAllowList, signature)),
				() -> assertThrows(NullPointerException.class,
						() -> new SetupComponentLVCCAllowListPayload(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, null, signature)),
				() -> assertThrows(NullPointerException.class,
						() -> new SetupComponentLVCCAllowListPayload(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, longVoteCastReturnCodesAllowList,
								null)),

				() -> assertThrows(NullPointerException.class,
						() -> new SetupComponentLVCCAllowListPayload(null, VERIFICATION_CARD_SET_ID, longVoteCastReturnCodesAllowList)),
				() -> assertThrows(FailedValidationException.class,
						() -> new SetupComponentLVCCAllowListPayload("invalidElectionEventId", VERIFICATION_CARD_SET_ID,
								longVoteCastReturnCodesAllowList)),
				() -> assertThrows(NullPointerException.class,
						() -> new SetupComponentLVCCAllowListPayload(ELECTION_EVENT_ID, null, longVoteCastReturnCodesAllowList)),
				() -> assertThrows(FailedValidationException.class,
						() -> new SetupComponentLVCCAllowListPayload(ELECTION_EVENT_ID, "invalidVerificationCardSetId",
								longVoteCastReturnCodesAllowList)),
				() -> assertThrows(NullPointerException.class,
						() -> new SetupComponentLVCCAllowListPayload(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, null)),

				() -> assertThrows(IllegalArgumentException.class,
						() -> new SetupComponentLVCCAllowListPayload(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, emptyAllowList)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> new SetupComponentLVCCAllowListPayload(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, emptyStringInAllowList)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> new SetupComponentLVCCAllowListPayload(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, whitespaceOnlyStringInAllowList)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> new SetupComponentLVCCAllowListPayload(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, notBase64InAllowList)));
	}

	@Test
	@DisplayName("equals give expected result")
	void testEquality() {
		final SetupComponentLVCCAllowListPayload same = new SetupComponentLVCCAllowListPayload(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID,
				longVoteCastReturnCodesAllowList, signature);
		final SetupComponentLVCCAllowListPayload different = new SetupComponentLVCCAllowListPayload(uuidGenerator.generate(),
				VERIFICATION_CARD_SET_ID, longVoteCastReturnCodesAllowList, signature);

		assertAll(
				() -> assertEquals(setupComponentLVCCAllowListPayload, setupComponentLVCCAllowListPayload),
				() -> assertEquals(setupComponentLVCCAllowListPayload, same),
				() -> assertNotEquals(setupComponentLVCCAllowListPayload, different),
				() -> assertNotEquals(null, setupComponentLVCCAllowListPayload),
				() -> assertNotEquals(1L, setupComponentLVCCAllowListPayload),
				() -> assertEquals(setupComponentLVCCAllowListPayload.hashCode(), same.hashCode())
		);
	}
}
