/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.validations.JsonSchemaValidation.validate;
import static ch.post.it.evoting.votingserver.process.voting.JsonSchemaConstants.CONFIRM_VOTE_PAYLOAD_SCHEMA;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mapper.EncryptionGroupUtils;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationChallenge;

public class ConfirmVotePayloadDeserializer extends JsonDeserializer<ConfirmVotePayload> {

	@Override
	public ConfirmVotePayload deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
			throws IOException {

		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		final JsonNode node = validate(objectMapper.readTree(jsonParser), CONFIRM_VOTE_PAYLOAD_SCHEMA);

		final ContextIds contextIds = objectMapper.readValue(node.get("contextIds").toString(), ContextIds.class);

		final JsonNode encryptionGroupNode = node.get("encryptionGroup");
		final GqGroup encryptionGroup = EncryptionGroupUtils.getEncryptionGroup(objectMapper, encryptionGroupNode);

		final GqElement confirmationKey = objectMapper.reader()
				.withAttribute("group", encryptionGroup)
				.readValue(node.get("confirmationKey"), GqElement.class);

		final AuthenticationChallenge authenticationChallenge = objectMapper.readValue(node.get("authenticationChallenge").toString(),
				AuthenticationChallenge.class);

		return new ConfirmVotePayload(contextIds, encryptionGroup, confirmationKey, authenticationChallenge);
	}

}
