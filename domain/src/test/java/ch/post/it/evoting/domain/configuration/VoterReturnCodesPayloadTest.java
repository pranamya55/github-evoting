/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

package ch.post.it.evoting.domain.configuration;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_CHOICE_RETURN_CODE_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

class VoterReturnCodesPayloadTest {

	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static ObjectNode rootNode;

	private final ObjectMapper mapper = DomainObjectMapper.getNewInstance();
	private final String electionEventId = uuidGenerator.generate();
	private final String verificationCardSetId = uuidGenerator.generate();

	private VoterReturnCodesPayload payload;

	@BeforeEach
	void setup() {
		final GqGroup encryptionGroup = GroupTestData.getGroupP59();

		final int primeSize = 4;
		final int voterReturnCodesSize = 5;
		final List<VoterReturnCodes> voterReturnCodesList = new ArrayList<>();

		for (int i = 0; i < voterReturnCodesSize; i++) {
			final String verificationCardId = uuidGenerator.generate();
			final String voteCastReturnCode = uuidGenerator.generate();
			final GroupVector<PrimeGqElement, GqGroup> primesGqElements =
					PrimeGqElement.PrimeGqElementFactory.getSmallPrimeGroupMembers(encryptionGroup, primeSize);

			final ImmutableList<ChoiceReturnCodeToEncodedVotingOptionEntry> entries =
					IntStream.range(0, primeSize)
							.mapToObj(idx -> new ChoiceReturnCodeToEncodedVotingOptionEntry(
									random.genUniqueDecimalStrings(SHORT_CHOICE_RETURN_CODE_LENGTH, 1).get(0),
									primesGqElements.get(idx)))
							.collect(toImmutableList());

			final GroupVector<ChoiceReturnCodeToEncodedVotingOptionEntry, GqGroup> choiceReturnCodesToEncodedVotingOptions =
					GroupVector.from(entries);

			voterReturnCodesList.add(new VoterReturnCodes(verificationCardId, voteCastReturnCode, choiceReturnCodesToEncodedVotingOptions));
		}

		payload = new VoterReturnCodesPayload(encryptionGroup, electionEventId, verificationCardSetId, ImmutableList.from(voterReturnCodesList));

		// Create expected Json.
		rootNode = mapper.createObjectNode();

		final JsonNode encryptionGroupNode = SerializationUtils.createEncryptionGroupNode(encryptionGroup);
		rootNode.set("encryptionGroup", encryptionGroupNode);
		rootNode.put("electionEventId", electionEventId);
		rootNode.put("verificationCardSetId", verificationCardSetId);

		final ArrayNode voterReturnCodesNodes = mapper.createArrayNode();

		for (final VoterReturnCodes voterReturnCodes : voterReturnCodesList) {
			final ObjectNode voterReturnCodesNode = mapper.createObjectNode();

			voterReturnCodesNode.put("verificationCardId", voterReturnCodes.verificationCardId());
			voterReturnCodesNode.put("voteCastReturnCode", voterReturnCodes.voteCastReturnCode());

			final ArrayNode choiceReturnCodesToEncodedVotingOptionsNodes = mapper.createArrayNode();
			for (final ChoiceReturnCodeToEncodedVotingOptionEntry entry : voterReturnCodes.choiceReturnCodesToEncodedVotingOptions()) {
				final ObjectNode choiceReturnCodeToEncodedVotingOptionEntryNode = mapper.createObjectNode();
				choiceReturnCodeToEncodedVotingOptionEntryNode.put("choiceReturnCode", entry.choiceReturnCode());
				choiceReturnCodeToEncodedVotingOptionEntryNode.put("encodedVotingOption", entry.encodedVotingOption().getValueAsInt());
				choiceReturnCodesToEncodedVotingOptionsNodes.add(choiceReturnCodeToEncodedVotingOptionEntryNode);
			}
			voterReturnCodesNode.set("choiceReturnCodesToEncodedVotingOptions", choiceReturnCodesToEncodedVotingOptionsNodes);

			voterReturnCodesNodes.add(voterReturnCodesNode);
		}
		rootNode.set("voterReturnCodes", voterReturnCodesNodes);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializePayload() throws JsonProcessingException {
		final String serializedPayload = mapper.writeValueAsString(payload);
		assertEquals(rootNode.toString(), serializedPayload);
	}

	@Test
	@DisplayName("deserialized gives expected payload")
	void deserializePayload() throws IOException {
		final VoterReturnCodesPayload deserializedPayload = mapper.readValue(rootNode.toString(), VoterReturnCodesPayload.class);
		assertEquals(payload, deserializedPayload);
	}

	@Test
	@DisplayName("serialized then deserialized gives original payload")
	void cycle() throws IOException {

		// Serialize
		final String payloadSerialized = mapper.writeValueAsString(payload);

		// Deserialize
		final VoterReturnCodesPayload payloadDeserialized = mapper.readValue(payloadSerialized, VoterReturnCodesPayload.class);

		assertEquals(payload, payloadDeserialized);
		assertEquals(payload.electionEventId(), payloadDeserialized.electionEventId());
		assertEquals(payload.verificationCardSetId(), payloadDeserialized.verificationCardSetId());
		assertTrue(payload.voterReturnCodes().stream()
						.allMatch(v1 -> payloadDeserialized.voterReturnCodes().stream().anyMatch(v2 -> areEqual(v1, v2))),
				"The voter return codes are not the same.");
	}

	private boolean areEqual(final VoterReturnCodes v1, final VoterReturnCodes v2) {
		return v1.verificationCardId().equals(v2.verificationCardId()) &&
				v1.voteCastReturnCode().equals(v2.voteCastReturnCode()) &&
				v1.choiceReturnCodesToEncodedVotingOptions().stream().allMatch(
						e1 -> v2.choiceReturnCodesToEncodedVotingOptions().stream()
								.anyMatch(e2 -> e1.choiceReturnCode().equals(e2.choiceReturnCode()) &&
										e1.encodedVotingOption().equals(e2.encodedVotingOption()))
				);
	}
}
