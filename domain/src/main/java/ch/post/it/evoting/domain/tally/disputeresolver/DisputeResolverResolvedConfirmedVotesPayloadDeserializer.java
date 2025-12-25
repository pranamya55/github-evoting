/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally.disputeresolver;

import static ch.post.it.evoting.domain.voting.JsonSchemaConstants.DISPUTE_RESOLVER_RESOLVED_CONFIRMED_VOTES_PAYLOAD_SCHEMA;
import static ch.post.it.evoting.evotinglibraries.domain.validations.JsonSchemaValidation.validate;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

public class DisputeResolverResolvedConfirmedVotesPayloadDeserializer
		extends JsonDeserializer<DisputeResolverResolvedConfirmedVotesPayload> {

	@Override
	public DisputeResolverResolvedConfirmedVotesPayload deserialize(final JsonParser parser, final DeserializationContext deserializationContext)
			throws IOException {

		final ObjectMapper mapper = (ObjectMapper) parser.getCodec();
		final JsonNode node = validate(mapper.readTree(parser), DISPUTE_RESOLVER_RESOLVED_CONFIRMED_VOTES_PAYLOAD_SCHEMA);

		final String electionEventId = node.get("electionEventId").asText();

		final ImmutableList<ResolvedConfirmedVote> resolvedConfirmedVotes = ImmutableList.of(mapper.reader()
				.readValue(node.get("resolvedConfirmedVotes").toString(), ResolvedConfirmedVote[].class));

		final CryptoPrimitivesSignature signature = mapper.reader()
				.readValue(node.get("signature").toString(), CryptoPrimitivesSignature.class);

		return new DisputeResolverResolvedConfirmedVotesPayload(electionEventId, resolvedConfirmedVotes, signature);
	}
}
