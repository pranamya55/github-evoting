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
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

public class VoterReturnCodesDeserializer extends JsonDeserializer<VoterReturnCodes> {

	/**
	 * The {@code context} must provide the {@link GqGroup} that will be used to reconstruct the underlying {@link PrimeGqElement}s.
	 */
	@Override
	public VoterReturnCodes deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {

		final ObjectMapper mapper = DomainObjectMapper.getNewInstance();

		final JsonNode node = mapper.readTree(parser);

		final String verificationCardId = mapper.readValue(node.get("verificationCardId").toString(), String.class);
		final String voteCastReturnCode = mapper.readValue(node.get("voteCastReturnCode").toString(), String.class);

		final GqGroup encryptionGroup = (GqGroup) context.getAttribute("group");
		final GroupVector<ChoiceReturnCodeToEncodedVotingOptionEntry, GqGroup> choiceReturnCodesToEncodedVotingOptions = GroupVector.from(
				ImmutableList.of(mapper.reader()
						.withAttribute("group", encryptionGroup)
						.readValue(node.get("choiceReturnCodesToEncodedVotingOptions").toString(), ChoiceReturnCodeToEncodedVotingOptionEntry[].class)
				));

		return new VoterReturnCodes(verificationCardId, voteCastReturnCode, choiceReturnCodesToEncodedVotingOptions);
	}
}
