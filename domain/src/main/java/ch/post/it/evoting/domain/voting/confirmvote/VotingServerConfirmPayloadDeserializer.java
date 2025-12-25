/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.confirmvote;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mapper.EncryptionGroupUtils;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

public class VotingServerConfirmPayloadDeserializer extends JsonDeserializer<VotingServerConfirmPayload> {

	@Override
	public VotingServerConfirmPayload deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
			throws IOException {
		final ObjectMapper mapper = DomainObjectMapper.getNewInstance();
		final JsonNode node = mapper.readTree(jsonParser);
		final JsonNode encryptionGroupNode = node.get("encryptionGroup");
		final GqGroup encryptionGroup = EncryptionGroupUtils.getEncryptionGroup(mapper, encryptionGroupNode);

		final ConfirmationKey payload = mapper.reader().withAttribute("group", encryptionGroup)
				.readValue(node.get("confirmationKey"), ConfirmationKey.class);

		final int confirmationAttemptId = mapper.readValue(node.get("confirmationAttemptId").toString(), Integer.class);

		final CryptoPrimitivesSignature signature = mapper.readValue(node.get("signature").toString(), CryptoPrimitivesSignature.class);

		return new VotingServerConfirmPayload(encryptionGroup, payload, confirmationAttemptId, signature);
	}
}