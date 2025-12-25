/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.compute;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueuedComputeChunkIdsRepository extends CrudRepository<QueuedComputeChunkIdsEntity, QueuedComputeChunkIdsEntityId> {

	List<QueuedComputeChunkIdsEntity> findAllByElectionEventIdAndVerificationCardSetIdOrderByChunkId(final String electionEventId,
			final String verificationCardSetId);
}
