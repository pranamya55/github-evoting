/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.configurevoterportal;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "config", "favicon", "logo" })
public record VoterPortalConfigurationPayloadState(
		VoterPortalConfigurationPayloadStatus config,
		VoterPortalConfigurationPayloadStatus favicon,
		VoterPortalConfigurationPayloadStatus logo
) {}
