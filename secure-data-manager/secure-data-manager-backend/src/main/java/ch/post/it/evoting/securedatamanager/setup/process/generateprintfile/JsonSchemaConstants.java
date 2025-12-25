/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generateprintfile;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;

public final class JsonSchemaConstants {

	private static final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder ->
			builder.schemaMappers(schemaMappers -> schemaMappers.mapPrefix("https://evoting.ch/securedatamanager/",
					"classpath:json-schema/ch/post/it/evoting/securedatamanager/setup/process/generateprintfile/"))
	);

	private static final SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

	public static final JsonSchema BALLOT_BOXES_REPORT_SCHEMA = factory.getSchema(
			SchemaLocation.of("https://evoting.ch/securedatamanager/BallotBoxesReport.schema.json"), config);

	private JsonSchemaConstants() {
		// Intentionally left blank.
	}
}