/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.votingcardmanagement;

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
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.votingserver.process.votingcardmanagement.VotingCardSearchDto.Metadata;

class VotingCardSearchDtoTest {
	private static final ObjectMapper mapper = DomainObjectMapper.getNewInstance();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static ImmutableList<VotingCardDto> votingCards;
	private static Metadata metadata;
	private static VotingCardSearchDto votingCardSearchDto;
	private static ObjectNode rootNode;

	@BeforeAll
	static void setupAll() throws JsonProcessingException {
		votingCards = ImmutableList.of(
				new VotingCardDto(
						uuidGenerator.generate(),
						uuidGenerator.generate(),
						uuidGenerator.generate(),
						uuidGenerator.generate(),
						VerificationCardState.CONFIRMED,
						LocalDateTimeUtils.now()
				)
		);

		metadata = new Metadata(0, 0);

		votingCardSearchDto = new VotingCardSearchDto(votingCards, metadata);

		// Create expected Json.
		rootNode = mapper.createObjectNode();
		rootNode.set("votingCards", mapper.readTree(mapper.writeValueAsString(votingCards)));
		rootNode.set("_metadata", mapper.readTree(mapper.writeValueAsString(metadata)));
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serialize() throws JsonProcessingException {
		final String serializedDto = mapper.writeValueAsString(votingCardSearchDto);
		assertEquals(rootNode.toString(), serializedDto);
	}

	@Test
	@DisplayName("deserialized gives expected Dto")
	void deserialize() throws IOException {
		final VotingCardSearchDto deserializedDto = mapper.readValue(rootNode.toString(), VotingCardSearchDto.class);
		assertEquals(votingCardSearchDto, deserializedDto);
	}

	@Test
	@DisplayName("serialized then deserialized gives original Dto")
	void cycle() throws IOException {
		final VotingCardSearchDto deserializedDto = mapper.readValue(mapper.writeValueAsString(votingCardSearchDto), VotingCardSearchDto.class);

		assertEquals(votingCardSearchDto, deserializedDto);
	}

	@Test
	@DisplayName("created with nulls throws.")
	void nulls() {
		assertThrows(NullPointerException.class, () -> new VotingCardSearchDto(null, null));
		assertThrows(NullPointerException.class, () -> new VotingCardSearchDto(votingCards, null));
		assertThrows(NullPointerException.class, () -> new VotingCardSearchDto(null, metadata));
	}

	@Test
	@DisplayName("created with invalid limit throws.")
	void invalidLimit() {
		final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> new Metadata(-1, 0));
		assertEquals("Limit must be greater than or equal to 0", illegalArgumentException.getMessage());
	}

	@Test
	@DisplayName("created with invalid total count throws.")
	void invalidTotalCount() {
		final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> new Metadata(0, -1));
		assertEquals("Total count must be greater than or equal to 0", illegalArgumentException.getMessage());
	}
}
