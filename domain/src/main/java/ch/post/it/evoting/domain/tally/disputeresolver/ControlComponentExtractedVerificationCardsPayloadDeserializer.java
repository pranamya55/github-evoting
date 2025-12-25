/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally.disputeresolver;

import static ch.post.it.evoting.domain.voting.JsonSchemaConstants.CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_SCHEMA;
import static ch.post.it.evoting.evotinglibraries.domain.validations.JsonSchemaValidation.validate;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;
import ch.post.it.evoting.evotinglibraries.domain.mapper.EncryptionGroupUtils;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

public class ControlComponentExtractedVerificationCardsPayloadDeserializer
		extends JsonDeserializer<ControlComponentExtractedVerificationCardsPayload> {

	@Override
	public ControlComponentExtractedVerificationCardsPayload deserialize(final JsonParser parser, final DeserializationContext deserializationContext)
			throws IOException {

		final ObjectMapper mapper = (ObjectMapper) parser.getCodec();
		final JsonNode node = validate(mapper.readTree(parser), CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_SCHEMA);

		final JsonNode encryptionGroupNode = node.get("encryptionGroup");
		final GqGroup encryptionGroup = EncryptionGroupUtils.getEncryptionGroup(mapper, encryptionGroupNode);

		final String electionEventId = node.get("electionEventId").asText();

		final int nodeId = mapper.readValue(node.get("nodeId").toString(), Integer.class);

		final ImmutableList<ExtractedVerificationCard> extractedVerificationCards = ImmutableList.of(mapper.reader()
				.withAttribute("group", encryptionGroup)
				.readValue(node.get("extractedVerificationCards").toString(), ExtractedVerificationCard[].class));

		final CryptoPrimitivesSignature signature = mapper.reader()
				.readValue(node.get("signature").toString(), CryptoPrimitivesSignature.class);

		return new ControlComponentExtractedVerificationCardsPayload(encryptionGroup, electionEventId, nodeId, extractedVerificationCards,
				signature);
	}
}
