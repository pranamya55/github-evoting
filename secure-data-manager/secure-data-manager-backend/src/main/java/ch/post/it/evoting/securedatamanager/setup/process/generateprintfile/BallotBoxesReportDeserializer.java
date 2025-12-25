/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generateprintfile;

import static ch.post.it.evoting.evotinglibraries.domain.validations.JsonSchemaValidation.validate;
import static ch.post.it.evoting.securedatamanager.setup.process.generateprintfile.JsonSchemaConstants.BALLOT_BOXES_REPORT_SCHEMA;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

public class BallotBoxesReportDeserializer extends JsonDeserializer<BallotBoxesReport> {

	@Override
	public BallotBoxesReport deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
			throws IOException {

		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		final JsonNode node = validate(objectMapper.readTree(jsonParser), BALLOT_BOXES_REPORT_SCHEMA);

		final String electionEventId = objectMapper.readValue(node.get("electionEventId").toString(), String.class);

		final ImmutableList<BallotBoxInformation> ballotBoxesInformation = ImmutableList.of(objectMapper.reader()
				.readValue(node.get("ballotBoxesInformation"), BallotBoxInformation[].class)
		);

		return new BallotBoxesReport(electionEventId, ballotBoxesInformation);
	}

}