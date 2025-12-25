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
import ch.post.it.evoting.domain.generators.DisputeResolverResolvedConfirmedVotesPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("A disputeResolverConfirmedVotesPayload")
class DisputeResolverResolvedConfirmedVotesPayloadTest {

	private static final ObjectMapper mapper = DomainObjectMapper.getNewInstance();

	private static DisputeResolverResolvedConfirmedVotesPayload disputeResolverResolvedConfirmedVotesPayload;
	private static String electionEventId;
	private static ImmutableList<ResolvedConfirmedVote> resolvedConfirmedVotes;
	private static ObjectNode rootNode;

	@BeforeAll
	static void setupAll() throws JsonProcessingException {

		// Create payload.
		final DisputeResolverResolvedConfirmedVotesPayloadGenerator generator = new DisputeResolverResolvedConfirmedVotesPayloadGenerator();
		disputeResolverResolvedConfirmedVotesPayload = generator.generate();
		electionEventId = disputeResolverResolvedConfirmedVotesPayload.getElectionEventId();
		resolvedConfirmedVotes = disputeResolverResolvedConfirmedVotesPayload.getResolvedConfirmedVotes();
		final CryptoPrimitivesSignature signature = disputeResolverResolvedConfirmedVotesPayload.getSignature();

		// Create expected Json.
		rootNode = mapper.createObjectNode();
		rootNode.set("electionEventId", mapper.readTree(mapper.writeValueAsString(electionEventId)));
		rootNode.set("resolvedConfirmedVotes", mapper.readTree(mapper.writeValueAsString(resolvedConfirmedVotes)));

		rootNode.set("signature", SerializationUtils.createSignatureNode(signature));
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializePayload() throws JsonProcessingException {
		final String serializedPayload = mapper.writeValueAsString(disputeResolverResolvedConfirmedVotesPayload);
		assertEquals(rootNode.toString(), serializedPayload);
	}

	@Test
	@DisplayName("deserialized gives expected payload")
	void deserializePayload() throws IOException {
		final DisputeResolverResolvedConfirmedVotesPayload deserializedPayload = mapper.readValue(rootNode.toString(),
				DisputeResolverResolvedConfirmedVotesPayload.class);
		assertEquals(disputeResolverResolvedConfirmedVotesPayload, deserializedPayload);
	}

	@Test
	@DisplayName("serialized then deserialized gives original payload")
	void cycle() throws IOException {
		final DisputeResolverResolvedConfirmedVotesPayload deserializedPayload =
				mapper.readValue(mapper.writeValueAsString(disputeResolverResolvedConfirmedVotesPayload),
						DisputeResolverResolvedConfirmedVotesPayload.class);

		assertEquals(disputeResolverResolvedConfirmedVotesPayload, deserializedPayload);
	}

	@Test
	@DisplayName("constructed with invalid fields throws an exception")
	void invalidFieldsThrows() {
		assertAll(
				() -> assertThrows(FailedValidationException.class,
						() -> new DisputeResolverResolvedConfirmedVotesPayload("invalidId", resolvedConfirmedVotes)),
				() -> assertThrows(NullPointerException.class,
						() -> new DisputeResolverResolvedConfirmedVotesPayload(null, resolvedConfirmedVotes)),
				() -> assertThrows(NullPointerException.class,
						() -> new DisputeResolverResolvedConfirmedVotesPayload(electionEventId, null))
		);
	}

	@Test
	@DisplayName("constructed with duplicated verification card ids throws an exception")
	void duplicatedConfirmedVerificationCardIdsThrows() {
		final ImmutableList<ResolvedConfirmedVote> duplicatedVerificationCardIds = resolvedConfirmedVotes.stream()
				.map(resolvedConfirmedVote -> new ResolvedConfirmedVote(electionEventId, resolvedConfirmedVote.verificationCardSetId(),
						resolvedConfirmedVote.hashedLongVoteCastReturnCodeShares()))
				.collect(toImmutableList());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new DisputeResolverResolvedConfirmedVotesPayload(electionEventId, duplicatedVerificationCardIds));

		final String expected = "The verification card ids must be unique.";
		assertEquals(expected, exception.getMessage());
	}
}
