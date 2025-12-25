/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;

public final class JsonSchemaConstants {

	private static final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder ->
			builder.schemaMappers(schemaMappers -> schemaMappers.mapPrefix("https://evoting.ch/votingserver/",
							"classpath:json-schema/ch/post/it/evoting/votingserver/process/voting/"))
					.schemaMappers(schemaMappers -> schemaMappers.mapPrefix("https://evoting.ch/evotinglibraries/",
							"classpath:json-schema/ch/post/it/evoting/evotinglibraries/domain/"))
	);

	private static final SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

	public static final JsonSchema AUTHENTICATE_VOTER_PAYLOAD_SCHEMA = factory.getSchema(
			SchemaLocation.of("https://evoting.ch/votingserver/authenticatevoter/AuthenticateVoterPayload.schema.json"), config);
	public static final JsonSchema CONFIRM_VOTE_PAYLOAD_SCHEMA = factory.getSchema(
			SchemaLocation.of("https://evoting.ch/votingserver/confirmvote/ConfirmVotePayload.schema.json"), config);
	public static final JsonSchema SEND_VOTE_PAYLOAD_SCHEMA = factory.getSchema(
			SchemaLocation.of("https://evoting.ch/votingserver/sendvote/SendVotePayload.schema.json"), config);

	private JsonSchemaConstants() {
		// Intentionally left blank.
	}
}
