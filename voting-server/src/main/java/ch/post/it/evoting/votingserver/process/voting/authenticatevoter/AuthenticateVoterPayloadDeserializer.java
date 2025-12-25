/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.authenticatevoter;

import static ch.post.it.evoting.evotinglibraries.domain.validations.JsonSchemaValidation.validate;
import static ch.post.it.evoting.votingserver.process.voting.JsonSchemaConstants.AUTHENTICATE_VOTER_PAYLOAD_SCHEMA;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationChallenge;

public class AuthenticateVoterPayloadDeserializer extends JsonDeserializer<AuthenticateVoterPayload> {

	@Override
	public AuthenticateVoterPayload deserialize(final JsonParser parser, final DeserializationContext context)
			throws IOException {

		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		final JsonNode node = validate(objectMapper.readTree(parser), AUTHENTICATE_VOTER_PAYLOAD_SCHEMA);

		final String electionEventId = objectMapper.readValue(node.get("electionEventId").toString(), String.class);
		final AuthenticationChallenge authenticationChallenge = objectMapper.readValue(node.get("authenticationChallenge").toString(),
				AuthenticationChallenge.class);

		return new AuthenticateVoterPayload(electionEventId, authenticationChallenge);
	}
}