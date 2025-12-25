/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.sendvote;

import static ch.post.it.evoting.evotinglibraries.domain.validations.JsonSchemaValidation.validate;
import static ch.post.it.evoting.votingserver.process.voting.JsonSchemaConstants.SEND_VOTE_PAYLOAD_SCHEMA;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mapper.EncryptionGroupUtils;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationChallenge;

public class SendVotePayloadDeserializer extends JsonDeserializer<SendVotePayload> {

	@Override
	public SendVotePayload deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
			throws IOException {

		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		final JsonNode node = validate(objectMapper.readTree(jsonParser), SEND_VOTE_PAYLOAD_SCHEMA);

		final ContextIds contextIds = objectMapper.readValue(node.get("contextIds").toString(), ContextIds.class);

		final JsonNode encryptionGroupNode = node.get("encryptionGroup");
		final GqGroup encryptionGroup = EncryptionGroupUtils.getEncryptionGroup(objectMapper, encryptionGroupNode);

		final EncryptedVerifiableVote encryptedVerifiableVote = objectMapper.reader()
				.withAttribute("group", encryptionGroup)
				.readValue(node.get("encryptedVerifiableVote"), EncryptedVerifiableVote.class);

		final AuthenticationChallenge authenticationChallenge = objectMapper.readValue(node.get("authenticationChallenge").toString(),
				AuthenticationChallenge.class);

		return new SendVotePayload(contextIds, encryptionGroup, encryptedVerifiableVote, authenticationChallenge);
	}

}
