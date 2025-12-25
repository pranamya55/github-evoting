/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.ConversionUtils.bigIntegerToBase64;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.SecureRandom;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.domain.MapperSetUp;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

@DisplayName("A ControlComponentlVCCSharePayload")
class ControlComponentlVCCSharePayloadTest extends MapperSetUp {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_SET_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_ID = uuidGenerator.generate();
	private static final int NODE_ID = 1;
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final byte[] randomBytes = new byte[10];

	private static ObjectNode rootNode;
	private static ControlComponentlVCCSharePayload controlComponentlVCCSharePayload;
	private static ConfirmationKey confirmationKey;
	private static GqGroup encryptionGroup;

	@BeforeAll
	static void setUpAll() {
		encryptionGroup = SerializationUtils.getGqGroup();
		final GqElement longVoteCastCode = SerializationUtils.getLongChoiceCodes(1).getFirst();

		// Generate random bytes for signature content and create payload signature.
		secureRandom.nextBytes(randomBytes);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(new ImmutableByteArray(randomBytes));

		// Create payload.
		final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare = new LongVoteCastReturnCodeShare(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID,
				VERIFICATION_CARD_ID, NODE_ID, longVoteCastCode);

		final GqElement randomGqElement = new GqGroupGenerator(encryptionGroup).genMember();
		final ContextIds contextIds = new ContextIds(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, VERIFICATION_CARD_ID);
		confirmationKey = new ConfirmationKey(contextIds, randomGqElement);

		controlComponentlVCCSharePayload = new ControlComponentlVCCSharePayload(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, VERIFICATION_CARD_ID,
				NODE_ID,
				encryptionGroup, longVoteCastReturnCodeShare, confirmationKey, true, signature);

		// Create expected Json.
		rootNode = mapper.createObjectNode();
		rootNode.put("electionEventId", ELECTION_EVENT_ID);
		rootNode.put("verificationCardSetId", VERIFICATION_CARD_SET_ID);
		rootNode.put("verificationCardId", VERIFICATION_CARD_ID);
		rootNode.put("nodeId", NODE_ID);

		final JsonNode encryptionGroupNode = SerializationUtils.createEncryptionGroupNode(encryptionGroup);
		rootNode.set("encryptionGroup", encryptionGroupNode);

		final ObjectNode shareNode = mapper.createObjectNode();
		shareNode.put("electionEventId", ELECTION_EVENT_ID);
		shareNode.put("verificationCardSetId", VERIFICATION_CARD_SET_ID);
		shareNode.put("verificationCardId", VERIFICATION_CARD_ID);
		shareNode.put("nodeId", NODE_ID);

		final JsonNode lvccNode = SerializationUtils.createLVCCNode(longVoteCastCode);
		shareNode.set("longVoteCastReturnCodeShare", lvccNode);

		rootNode.set("longVoteCastReturnCodeShare", shareNode);

		final ObjectNode contextIdsNode = mapper.createObjectNode();
		contextIdsNode.put("electionEventId", ELECTION_EVENT_ID);
		contextIdsNode.put("verificationCardSetId", VERIFICATION_CARD_SET_ID);
		contextIdsNode.put("verificationCardId", VERIFICATION_CARD_ID);

		final ObjectNode confirmationKeyNode = mapper.createObjectNode();
		confirmationKeyNode.set("contextIds", contextIdsNode);
		confirmationKeyNode.put("element", bigIntegerToBase64(confirmationKey.element().getValue()));
		rootNode.set("confirmationKey", confirmationKeyNode);

		rootNode.put("isVerified", true);

		final JsonNode signatureNode = SerializationUtils.createSignatureNode(signature);
		rootNode.set("signature", signatureNode);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializePayload() throws JsonProcessingException {
		final String serializedPayload = mapper.writeValueAsString(controlComponentlVCCSharePayload);

		assertEquals(rootNode.toString(), serializedPayload);
	}

	@Test
	@DisplayName("deserialized gives expected payload")
	void deserializePayload() throws JsonProcessingException {
		final ControlComponentlVCCSharePayload deserializedPayload = mapper.readValue(rootNode.toString(),
				ControlComponentlVCCSharePayload.class);

		assertEquals(controlComponentlVCCSharePayload, deserializedPayload);
	}

	@Test
	@DisplayName("serialized then deserialized gives original payload")
	void cycle() throws JsonProcessingException {
		final ControlComponentlVCCSharePayload deserializedPayload = mapper
				.readValue(mapper.writeValueAsString(controlComponentlVCCSharePayload), ControlComponentlVCCSharePayload.class);

		assertEquals(controlComponentlVCCSharePayload, deserializedPayload);
	}

	@Test
	@DisplayName("serialized optional gives expected json")
	void serializePayloadOptional() throws JsonProcessingException {

		final ObjectNode rootNodeLocal = mapper.createObjectNode();
		rootNodeLocal.put("electionEventId", ELECTION_EVENT_ID);
		rootNodeLocal.put("verificationCardSetId", VERIFICATION_CARD_SET_ID);
		rootNodeLocal.put("verificationCardId", VERIFICATION_CARD_ID);
		rootNodeLocal.put("nodeId", NODE_ID);

		final JsonNode encryptionGroupNode = SerializationUtils.createEncryptionGroupNode(encryptionGroup);
		rootNodeLocal.set("encryptionGroup", encryptionGroupNode);

		final ObjectNode contextIdsNode = mapper.createObjectNode();
		contextIdsNode.put("electionEventId", ELECTION_EVENT_ID);
		contextIdsNode.put("verificationCardSetId", VERIFICATION_CARD_SET_ID);
		contextIdsNode.put("verificationCardId", VERIFICATION_CARD_ID);

		final ObjectNode confirmationKeyNode = mapper.createObjectNode();
		confirmationKeyNode.set("contextIds", contextIdsNode);
		confirmationKeyNode.put("element", bigIntegerToBase64(confirmationKey.element().getValue()));
		rootNodeLocal.set("confirmationKey", confirmationKeyNode);

		rootNodeLocal.put("isVerified", false);

		// Generate random bytes for signature content and create payload signature.
		final byte[] signatureRandomBytes = new byte[10];
		secureRandom.nextBytes(signatureRandomBytes);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(new ImmutableByteArray(signatureRandomBytes));

		final JsonNode signatureNode = SerializationUtils.createSignatureNode(signature);
		rootNodeLocal.set("signature", signatureNode);

		final ControlComponentlVCCSharePayload payload = new ControlComponentlVCCSharePayload(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID,
				VERIFICATION_CARD_ID, NODE_ID, encryptionGroup, confirmationKey, false);
		payload.setSignature(signature);
		final String serializedPayload = mapper.writeValueAsString(payload);

		assertEquals(rootNodeLocal.toString(), serializedPayload);
	}

	@Test
	@DisplayName("serialized then deserialized with optional gives original payload")
	void cycleOptional() throws JsonProcessingException {
		final ObjectNode rootNodeLocal = mapper.createObjectNode();
		rootNodeLocal.put("electionEventId", ELECTION_EVENT_ID);
		rootNodeLocal.put("verificationCardSetId", VERIFICATION_CARD_SET_ID);
		rootNodeLocal.put("verificationCardId", VERIFICATION_CARD_ID);
		rootNodeLocal.put("nodeId", NODE_ID);

		final JsonNode encryptionGroupNode = SerializationUtils.createEncryptionGroupNode(encryptionGroup);
		rootNodeLocal.set("encryptionGroup", encryptionGroupNode);

		rootNodeLocal.put("isVerified", false);

		final ObjectNode contextIdsNode = mapper.createObjectNode();
		contextIdsNode.put("electionEventId", ELECTION_EVENT_ID);
		contextIdsNode.put("verificationCardSetId", VERIFICATION_CARD_SET_ID);
		contextIdsNode.put("verificationCardId", VERIFICATION_CARD_ID);

		final ObjectNode confirmationKeyNode = mapper.createObjectNode();
		confirmationKeyNode.set("contextIds", contextIdsNode);
		confirmationKeyNode.put("element", "0x" + confirmationKey.element().getValue().toString(16).toUpperCase(Locale.ENGLISH));
		rootNodeLocal.set("confirmationKey", confirmationKeyNode);

		// Generate random bytes for signature content and create payload signature.
		final byte[] signatureRandomBytes = new byte[10];
		secureRandom.nextBytes(signatureRandomBytes);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(new ImmutableByteArray(signatureRandomBytes));

		final JsonNode signatureNode = SerializationUtils.createSignatureNode(signature);
		rootNodeLocal.set("signature", signatureNode);

		final ControlComponentlVCCSharePayload payload = new ControlComponentlVCCSharePayload(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID,
				VERIFICATION_CARD_ID, NODE_ID, encryptionGroup,
				confirmationKey, false);
		payload.setSignature(signature);

		final ControlComponentlVCCSharePayload deserializedPayload = mapper
				.readValue(mapper.writeValueAsString(payload), ControlComponentlVCCSharePayload.class);

		assertEquals(payload, deserializedPayload);
	}

}
