/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

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

public class VerificationCardSecretKeyPayloadDeserializer extends JsonDeserializer<VerificationCardSecretKeyPayload> {

	@Override
	public VerificationCardSecretKeyPayload deserialize(final JsonParser parser, final DeserializationContext deserializationContext)
			throws IOException {

		final ObjectMapper mapper = DomainObjectMapper.getNewInstance();

		final JsonNode node = mapper.readTree(parser);
		final JsonNode encryptionGroupNode = node.get("encryptionGroup");
		final GqGroup encryptionGroup = EncryptionGroupUtils.getEncryptionGroup(mapper, encryptionGroupNode);
		final String electionEventId = mapper.readValue(node.get("electionEventId").toString(), String.class);
		final String verificationCardSetId = mapper.readValue(node.get("verificationCardSetId").toString(), String.class);

		final ImmutableList<VerificationCardSecretKey> verificationCardSecretKeys = ImmutableList.of(mapper.reader()
				.withAttribute("group", encryptionGroup)
				.readValue(node.get("verificationCardSecretKeys"), VerificationCardSecretKey[].class));

		return new VerificationCardSecretKeyPayload(encryptionGroup, electionEventId, verificationCardSetId, verificationCardSecretKeys);
	}
}
