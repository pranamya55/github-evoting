/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.hasNoDuplicates;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

/**
 * Represents the list of {@link SetupComponentCMTablePayload} sorted by chunk id per election event id and verification card set id.
 *
 * @param payloads the list of {@link SetupComponentCMTablePayload}.
 *                 <li>It must not be null.</li>
 *                 <li>It must not contain null values.</li>
 *                 <li>The election event id must be consistent throughout the payloads.</li>
 *                 <li>The verification card set id must be consistent throughout the payloads.</li>
 *                 <li>The chunk ids must be consistent.</li>
 */
public record SetupComponentCMTablePayloadChunks(ImmutableList<SetupComponentCMTablePayload> payloads) {

	public SetupComponentCMTablePayloadChunks(final ImmutableList<SetupComponentCMTablePayload> payloads) {
		checkNotNull(payloads);

		final ImmutableList<SetupComponentCMTablePayload> sortedPayloads = payloads.stream().parallel()
				.sorted(Comparator.comparingInt(SetupComponentCMTablePayload::getChunkId))
				.collect(toImmutableList());
		checkArgument(!sortedPayloads.isEmpty(), "There must be at least one SetupComponentCMTablePayload.");

		checkArgument(allEqual(sortedPayloads.stream().parallel(), SetupComponentCMTablePayload::getElectionEventId),
				"All SetupComponentCMTablePayloads must have the same election event id.");
		checkArgument(allEqual(sortedPayloads.stream().parallel(), SetupComponentCMTablePayload::getVerificationCardSetId),
				"All SetupComponentCMTablePayloads must have the same verification card set id.");

		final ImmutableList<Integer> chunkIds = sortedPayloads.stream().map(SetupComponentCMTablePayload::getChunkId).collect(toImmutableList());
		checkArgument(hasNoDuplicates(chunkIds) && chunkIds.get(0) == 0 && chunkIds.get(chunkIds.size() - 1) == chunkIds.size() - 1,
				"The SetupComponentCMTablePayloads' chunk ids are not consistent.");

		this.payloads = sortedPayloads;
	}

	public int getChunkCount() {
		return payloads.size();
	}

	public String getElectionEventId() {
		return payloads.get(0).getElectionEventId();
	}

	public String getVerificationCardSetId() {
		return payloads.get(0).getVerificationCardSetId();
	}

}
