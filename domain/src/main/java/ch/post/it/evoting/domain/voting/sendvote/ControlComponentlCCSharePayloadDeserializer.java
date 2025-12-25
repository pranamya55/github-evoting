/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.sendvote;

import static ch.post.it.evoting.domain.voting.JsonSchemaConstants.CONTROL_COMPONENT_LCC_SHARE_PAYLOAD_SCHEMA;
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

public class ControlComponentlCCSharePayloadDeserializer extends JsonDeserializer<ControlComponentlCCSharePayload> {

	@Override
	public ControlComponentlCCSharePayload deserialize(final JsonParser parser, final DeserializationContext context)
			throws IOException {
		final ObjectMapper mapper = DomainObjectMapper.getNewInstance();
		final JsonNode node = validate(mapper.readTree(parser), CONTROL_COMPONENT_LCC_SHARE_PAYLOAD_SCHEMA);

		final JsonNode encryptionGroupNode = node.get("encryptionGroup");
		final GqGroup encryptionGroup = EncryptionGroupUtils.getEncryptionGroup(mapper, encryptionGroupNode);

		final LongChoiceReturnCodeShare payload = mapper.reader().withAttribute("group", encryptionGroup)
				.readValue(node.get("longChoiceReturnCodeShare"), LongChoiceReturnCodeShare.class);

		final CryptoPrimitivesSignature signature = mapper.readValue(node.get("signature").toString(), CryptoPrimitivesSignature.class);

		return new ControlComponentlCCSharePayload(encryptionGroup, payload, signature);
	}
}
