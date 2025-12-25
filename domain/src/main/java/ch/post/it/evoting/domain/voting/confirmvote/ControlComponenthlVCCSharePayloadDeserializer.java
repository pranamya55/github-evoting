/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.confirmvote;

import static ch.post.it.evoting.domain.voting.JsonSchemaConstants.CONTROL_COMPONENT_HLVCC_SHARE_PAYLOAD_SCHEMA;
import static ch.post.it.evoting.evotinglibraries.domain.validations.JsonSchemaValidation.validate;

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

public class ControlComponenthlVCCSharePayloadDeserializer extends JsonDeserializer<ControlComponenthlVCCSharePayload> {

	@Override
	public ControlComponenthlVCCSharePayload deserialize(final JsonParser parser, final DeserializationContext context)
			throws IOException {
		final ObjectMapper mapper = DomainObjectMapper.getNewInstance();

		final JsonNode node = validate(mapper.readTree(parser), CONTROL_COMPONENT_HLVCC_SHARE_PAYLOAD_SCHEMA);

		final JsonNode encryptionGroupNode = node.get("encryptionGroup");
		final GqGroup encryptionGroup = EncryptionGroupUtils.getEncryptionGroup(mapper, encryptionGroupNode);

		final String hashLongVoteCastReturnCodeShare = mapper.readValue(node.get("hashLongVoteCastReturnCodeShare").toString(), String.class);

		final ConfirmationKey confirmationKey = mapper.reader().withAttribute("group", encryptionGroup)
				.readValue(node.get("confirmationKey"), ConfirmationKey.class);

		final int confirmationAttemptId = mapper.readValue(node.get("confirmationAttemptId").toString(), Integer.class);

		final int nodeId = mapper.readValue(node.get("nodeId").toString(), Integer.class);

		final CryptoPrimitivesSignature signature = mapper.readValue(node.get("signature").toString(), CryptoPrimitivesSignature.class);

		return new ControlComponenthlVCCSharePayload(encryptionGroup, nodeId, hashLongVoteCastReturnCodeShare, confirmationKey, confirmationAttemptId,
				signature);
	}
}
