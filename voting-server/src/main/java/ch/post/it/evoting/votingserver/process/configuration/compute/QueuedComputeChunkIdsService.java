/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.compute;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

@Service
public class QueuedComputeChunkIdsService {

	private final QueuedComputeChunkIdsRepository queuedComputeChunkIdsRepository;

	public QueuedComputeChunkIdsService(final QueuedComputeChunkIdsRepository queuedComputeChunkIdsRepository) {
		this.queuedComputeChunkIdsRepository = queuedComputeChunkIdsRepository;
	}

	public ImmutableList<Integer> getQueuedComputeChunkIds(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		return queuedComputeChunkIdsRepository.findAllByElectionEventIdAndVerificationCardSetIdOrderByChunkId(electionEventId, verificationCardSetId)
				.stream()
				.map(QueuedComputeChunkIdsEntity::getChunkId)
				.collect(toImmutableList());
	}

	public void saveQueuedComputeChunkId(final String electionEventId, final String verificationCardSetId, final int chunkId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkArgument(chunkId >= 0);

		final QueuedComputeChunkIdsEntity queuedComputeChunkIdsEntity = new QueuedComputeChunkIdsEntity(electionEventId, verificationCardSetId,
				chunkId);
		queuedComputeChunkIdsRepository.save(queuedComputeChunkIdsEntity);
	}
}
