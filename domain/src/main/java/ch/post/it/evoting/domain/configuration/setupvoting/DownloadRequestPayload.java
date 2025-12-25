/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

public record DownloadRequestPayload(String electionEventId, String verificationCardSetId, ImmutableList<Integer> chunkIds) {

	public DownloadRequestPayload {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkNotNull(chunkIds);
		checkArgument(!chunkIds.isEmpty(), "The list of chunk ids must not be empty.");
		checkArgument(chunkIds.stream().allMatch(chunkId -> chunkId >= 0), "The chunk ids must be positive.");
	}
}
