/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.compute;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

@Entity
@IdClass(QueuedComputeChunkIdsEntityId.class)
public class QueuedComputeChunkIdsEntity {
	@Id
	private String electionEventId;

	@Id
	private String verificationCardSetId;

	@Id
	private int chunkId;

	public QueuedComputeChunkIdsEntity() {
		//Intentionally left blank
	}

	public QueuedComputeChunkIdsEntity(final String electionEventId, final String verificationCardSetId, final int chunkId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkArgument(chunkId >= 0);

		this.electionEventId = electionEventId;
		this.verificationCardSetId = verificationCardSetId;
		this.chunkId = chunkId;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public int getChunkId() {
		return chunkId;
	}
}
