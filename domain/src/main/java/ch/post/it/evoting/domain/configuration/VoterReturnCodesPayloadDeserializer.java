/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mapper.EncryptionGroupUtils;

public class VoterReturnCodesPayloadDeserializer extends JsonDeserializer<VoterReturnCodesPayload> {

	@Override
	public VoterReturnCodesPayload deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {

		final ObjectMapper mapper = DomainObjectMapper.getNewInstance();

		final JsonNode node = mapper.readTree(parser);

		final JsonNode encryptionGroupNode = node.get("encryptionGroup");
		final GqGroup encryptionGroup = EncryptionGroupUtils.getEncryptionGroup(mapper, encryptionGroupNode);

		final String electionEventId = mapper.readValue(node.get("electionEventId").toString(), String.class);
		final String verificationCardSetId = mapper.readValue(node.get("verificationCardSetId").toString(), String.class);

		final ImmutableList<VoterReturnCodes> voterReturnCodes = ImmutableList.of(mapper.reader()
				.withAttribute("group", encryptionGroup)
				.readValue(node.get("voterReturnCodes").toString(), VoterReturnCodes[].class));

		return new VoterReturnCodesPayload(encryptionGroup, electionEventId, verificationCardSetId, voterReturnCodes);
	}
}
