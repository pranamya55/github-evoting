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
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.PlaintextEqualityProof;
import ch.post.it.evoting.domain.MapperSetUp;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

@DisplayName("An VotingServerEncryptedVotePayload")
class VotingServerEncryptedVotePayloadTest extends MapperSetUp {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_SET_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_ID = uuidGenerator.generate();
	private static final Hash hash = HashFactory.createHash();

	private static ObjectNode rootNode;
	private static VotingServerEncryptedVotePayload votingServerEncryptedVotePayload;

	@BeforeAll
	static void setUpAll() {
		final GqGroup gqGroup = GroupTestData.getGqGroup();
		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);
		final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(ZqGroup.sameOrderAs(gqGroup));

		// Create payload.
		final ContextIds contextIds = new ContextIds(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, VERIFICATION_CARD_ID);
		final ElGamalMultiRecipientCiphertext encryptedVote = elGamalGenerator.genRandomCiphertext(2);
		final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote = elGamalGenerator.genRandomCiphertext(1);
		final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(2);
		final ExponentiationProof exponentiationProof = new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementMember());
		final PlaintextEqualityProof plaintextEqualityProof = new PlaintextEqualityProof(zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementVector(2));

		final EncryptedVerifiableVote encryptedVerifiableVote = new EncryptedVerifiableVote(contextIds, encryptedVote, exponentiatedEncryptedVote,
				encryptedPartialChoiceReturnCodes, exponentiationProof, plaintextEqualityProof);

		final VotingServerEncryptedVotePayload payload = new VotingServerEncryptedVotePayload(gqGroup, encryptedVerifiableVote);
		final ImmutableByteArray payloadHash = hash.recursiveHash(payload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		payload.setSignature(signature);

		votingServerEncryptedVotePayload = payload;

		// Create expected Json.
		rootNode = mapper.createObjectNode();

		final JsonNode encryptionGroupNode = SerializationUtils.createEncryptionGroupNode(gqGroup);
		rootNode.set("encryptionGroup", encryptionGroupNode);

		final ObjectNode contextIdNode = mapper.createObjectNode();
		contextIdNode.put("electionEventId", ELECTION_EVENT_ID);
		contextIdNode.put("verificationCardSetId", VERIFICATION_CARD_SET_ID);
		contextIdNode.put("verificationCardId", VERIFICATION_CARD_ID);

		final ObjectNode voteNode = mapper.createObjectNode();
		final ObjectNode encryptedVoteNode = SerializationUtils.createCiphertextNode(encryptedVote);
		final ObjectNode exponentiatedEncryptedVoteNode = SerializationUtils.createCiphertextNode(exponentiatedEncryptedVote);
		final ObjectNode encryptedPartialChoiceReturnCodesNode = SerializationUtils.createCiphertextNode(encryptedPartialChoiceReturnCodes);
		final ObjectNode exponentiationProofNode = SerializationUtils.createExponentiationProofNode(exponentiationProof);
		final ObjectNode plaintextEqualityProofNode = SerializationUtils.createPlaintextEqualityProofNode(plaintextEqualityProof);
		voteNode.set("contextIds", contextIdNode);
		voteNode.set("encryptedVote", encryptedVoteNode);
		voteNode.set("exponentiatedEncryptedVote", exponentiatedEncryptedVoteNode);
		voteNode.set("encryptedPartialChoiceReturnCodes", encryptedPartialChoiceReturnCodesNode);
		voteNode.set("exponentiationProof", exponentiationProofNode);
		voteNode.set("plaintextEqualityProof", plaintextEqualityProofNode);

		rootNode.set("encryptedVerifiableVote", voteNode);

		final JsonNode signatureNode = SerializationUtils.createSignatureNode(signature);
		rootNode.set("signature", signatureNode);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializePayload() throws JsonProcessingException {
		final String serializedPayload = mapper.writeValueAsString(votingServerEncryptedVotePayload);

		assertEquals(rootNode.toString(), serializedPayload);
	}

	@Test
	@DisplayName("deserialized gives expected payload")
	void deserializePayload() throws JsonProcessingException {
		final VotingServerEncryptedVotePayload deserializedPayload = mapper.readValue(rootNode.toString(), VotingServerEncryptedVotePayload.class);

		assertEquals(votingServerEncryptedVotePayload, deserializedPayload);
	}

	@Test
	@DisplayName("serialized then deserialized gives original payload")
	void cycle() throws JsonProcessingException {
		final VotingServerEncryptedVotePayload deserializedPayload = mapper
				.readValue(mapper.writeValueAsString(votingServerEncryptedVotePayload), VotingServerEncryptedVotePayload.class);

		assertEquals(votingServerEncryptedVotePayload, deserializedPayload);
	}

}
