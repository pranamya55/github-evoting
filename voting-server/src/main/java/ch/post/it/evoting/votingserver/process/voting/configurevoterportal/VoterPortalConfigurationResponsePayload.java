/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.configurevoterportal;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;

@JsonPropertyOrder({ "electionEventId", "config", "favicon", "logo", "votingServerTime" })
public record VoterPortalConfigurationResponsePayload(String electionEventId, ImmutableByteArray config, ImmutableByteArray favicon,
													  ImmutableByteArray logo, Long votingServerTime) {
}
