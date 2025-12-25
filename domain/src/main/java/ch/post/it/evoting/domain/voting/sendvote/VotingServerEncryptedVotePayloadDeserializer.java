/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.sendvote;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mapper.EncryptionGroupUtils;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

public class VotingServerEncryptedVotePayloadDeserializer extends JsonDeserializer<VotingServerEncryptedVotePayload> {

	@Override
	public VotingServerEncryptedVotePayload deserialize(final JsonParser parser, final DeserializationContext deserializationContext)
			throws IOException {

		final ObjectMapper mapper = DomainObjectMapper.getNewInstance();

		final JsonNode node = mapper.readTree(parser);
		final JsonNode encryptionGroupNode = node.get("encryptionGroup");
		final GqGroup encryptionGroup = EncryptionGroupUtils.getEncryptionGroup(mapper, encryptionGroupNode);

		final EncryptedVerifiableVote vote = mapper.reader().withAttribute("group", encryptionGroup)
				.readValue(node.get("encryptedVerifiableVote"), EncryptedVerifiableVote.class);

		final CryptoPrimitivesSignature signature = mapper.readValue(node.get("signature").toString(), CryptoPrimitivesSignature.class);

		return new VotingServerEncryptedVotePayload(encryptionGroup, vote, signature);
	}
}
