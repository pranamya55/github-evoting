/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;

@JsonPropertyOrder({ "electionEventId", "config", "favicon", "logo" })
public record VoterPortalConfigPayload(String electionEventId, ImmutableByteArray config, ImmutableByteArray favicon, ImmutableByteArray logo) {
}
