/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.confirmvote;

import static ch.post.it.evoting.domain.voting.JsonSchemaConstants.CONTROL_COMPONENT_LVCC_SHARE_PAYLOAD_SCHEMA;
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

public class ControlComponentlVCCSharePayloadDeserializer extends JsonDeserializer<ControlComponentlVCCSharePayload> {

	@Override
	public ControlComponentlVCCSharePayload deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
		final ObjectMapper mapper = DomainObjectMapper.getNewInstance();

		final JsonNode node = validate(mapper.readTree(parser), CONTROL_COMPONENT_LVCC_SHARE_PAYLOAD_SCHEMA);

		final String electionEventId = mapper.readValue(node.get("electionEventId").toString(), String.class);
		final String verificationCardSetId = mapper.readValue(node.get("verificationCardSetId").toString(), String.class);
		final String verificationCardId = mapper.readValue(node.get("verificationCardId").toString(), String.class);
		final int nodeId = mapper.readValue(node.get("nodeId").toString(), Integer.class);

		final JsonNode encryptionGroupNode = node.get("encryptionGroup");
		final GqGroup encryptionGroup = EncryptionGroupUtils.getEncryptionGroup(mapper, encryptionGroupNode);

		final ConfirmationKey confirmationKey = mapper.reader()
				.withAttribute("group", encryptionGroup)
				.readValue(node.get("confirmationKey"), ConfirmationKey.class);

		final boolean hasSignature = node.get("signature") != null;

		final CryptoPrimitivesSignature signature = hasSignature ?
				mapper.readValue(node.get("signature").toString(), CryptoPrimitivesSignature.class) : null;

		final boolean isVerified = node.get("isVerified").asBoolean();
		final LongVoteCastReturnCodeShare share = isVerified ?
				mapper.reader().withAttribute("group", encryptionGroup)
						.readValue(node.get("longVoteCastReturnCodeShare"), LongVoteCastReturnCodeShare.class) :
				null;

		if (hasSignature) {
			return new ControlComponentlVCCSharePayload(electionEventId, verificationCardSetId,
					verificationCardId, nodeId, encryptionGroup, share, confirmationKey, isVerified, signature);
		} else {
			return new ControlComponentlVCCSharePayload(electionEventId, verificationCardSetId,
					verificationCardId, nodeId, encryptionGroup, share, confirmationKey, isVerified);
		}
	}
}
