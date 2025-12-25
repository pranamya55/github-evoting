/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.compute;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Objects;

public class QueuedComputeChunkIdsEntityId implements Serializable {
	private String electionEventId;

	private String verificationCardSetId;

	private int chunkId;

	public QueuedComputeChunkIdsEntityId() {
		//Intentionally left blank
	}

	public QueuedComputeChunkIdsEntityId(final String electionEventId, final String verificationCardSetId, final int chunkId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkArgument(chunkId >= 0);

		this.electionEventId = electionEventId;
		this.verificationCardSetId = verificationCardSetId;
		this.chunkId = chunkId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(electionEventId, verificationCardSetId, chunkId);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final QueuedComputeChunkIdsEntityId queuedComputeChunkIdsEntityId = (QueuedComputeChunkIdsEntityId) o;
		return Objects.equals(electionEventId, queuedComputeChunkIdsEntityId.electionEventId) &&
				Objects.equals(verificationCardSetId, queuedComputeChunkIdsEntityId.verificationCardSetId) &&
				Objects.equals(chunkId, queuedComputeChunkIdsEntityId.chunkId);
	}

}
