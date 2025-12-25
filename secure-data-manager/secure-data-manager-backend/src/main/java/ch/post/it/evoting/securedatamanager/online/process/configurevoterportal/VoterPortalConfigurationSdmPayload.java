/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.configurevoterportal;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import ch.post.it.evoting.domain.configuration.VoterPortalConfigPayload;

@JsonPropertyOrder({ "sourcePath", "payload", "state" })
public record VoterPortalConfigurationSdmPayload(
		String sourcePath,
		String url,
		VoterPortalConfigPayload payload,
		VoterPortalConfigurationPayloadState state) {
}
