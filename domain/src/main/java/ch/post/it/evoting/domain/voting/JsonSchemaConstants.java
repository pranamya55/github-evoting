/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;

public final class JsonSchemaConstants {

	private static final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder ->
			builder.schemaMappers(schemaMappers -> schemaMappers.mapPrefix("https://evoting.ch/domain/",
							"classpath:json-schema/ch/post/it/evoting/domain/"))
					.schemaMappers(schemaMappers -> schemaMappers.mapPrefix("https://evoting.ch/evotinglibraries/",
							"classpath:json-schema/ch/post/it/evoting/evotinglibraries/domain/"))
	);

	private static final SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

	public static final JsonSchema CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_SCHEMA = factory.getSchema(
			SchemaLocation.of("https://evoting.ch/domain/tally/disputeresolver/ControlComponentExtractedElectionEventPayload.schema.json"), config);
	public static final JsonSchema CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_SCHEMA = factory.getSchema(
			SchemaLocation.of(
					"https://evoting.ch/domain/tally/disputeresolver/ControlComponentExtractedVerificationCardsPayload.schema.json"),
			config);
	public static final JsonSchema DISPUTE_RESOLVER_RESOLVED_CONFIRMED_VOTES_PAYLOAD_SCHEMA = factory.getSchema(
			SchemaLocation.of("https://evoting.ch/domain/tally/disputeresolver/DisputeResolverResolvedConfirmedVotesPayload.schema.json"), config);
	public static final JsonSchema CONTROL_COMPONENT_HLVCC_SHARE_PAYLOAD_SCHEMA = factory.getSchema(
			SchemaLocation.of("https://evoting.ch/domain/voting/confirmvote/ControlComponenthlVCCSharePayload.schema.json"), config);
	public static final JsonSchema CONTROL_COMPONENT_LVCC_SHARE_PAYLOAD_SCHEMA = factory.getSchema(
			SchemaLocation.of("https://evoting.ch/domain/voting/confirmvote/ControlComponentlVCCSharePayload.schema.json"), config);
	public static final JsonSchema CONTROL_COMPONENT_LCC_SHARE_PAYLOAD_SCHEMA = factory.getSchema(
			SchemaLocation.of("https://evoting.ch/domain/voting/sendvote/ControlComponentlCCSharePayload.schema.json"), config);
	public static final JsonSchema VOTER_PORTAL_CONFIG_SCHEMA = factory.getSchema(
			SchemaLocation.of("https://evoting.ch/domain/setup/voter-portal-config.schema.json"), config);

	private JsonSchemaConstants() {
		// Intentionally left blank.
	}
}
