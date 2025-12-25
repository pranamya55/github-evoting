/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.compute;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("QueuedComputeChunkIdsService calling")
class QueuedComputeChunkIdsServiceTest {

	private static QueuedComputeChunkIdsRepository queuedComputeChunkIdsRepository;
	private static QueuedComputeChunkIdsService queuedComputeChunkIdsService;

	private String electionEventId;
	private String verificationCardSetId;

	@BeforeAll
	static void setupAll() {
		queuedComputeChunkIdsRepository = mock(QueuedComputeChunkIdsRepository.class);
		queuedComputeChunkIdsService = new QueuedComputeChunkIdsService(queuedComputeChunkIdsRepository);
	}

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();
	}

	@DisplayName("getQueuedComputeChunksIds with null arguments throws a NullPointerException")
	@Test
	void getQueuedComputeChunksIdsWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> queuedComputeChunkIdsService.getQueuedComputeChunkIds(null, verificationCardSetId));
		assertThrows(NullPointerException.class, () -> queuedComputeChunkIdsService.getQueuedComputeChunkIds(electionEventId, null));
	}

	@DisplayName("getQueuedComputeChunksIds with non UUIDs throws a FailedValidationException")
	@Test
	void getQueuedComputeChunksIdsWithNonUuidsThrows() {
		assertThrows(FailedValidationException.class, () -> queuedComputeChunkIdsService.getQueuedComputeChunkIds("nonUUID", verificationCardSetId));
		assertThrows(FailedValidationException.class, () -> queuedComputeChunkIdsService.getQueuedComputeChunkIds(electionEventId, "nonUUID"));
	}

	@DisplayName("getQueuedComputeChunksIds with valid input returns list of chunk ids")
	@Test
	void getQueuedComputeChunksIdsWithValidArgumentsReturns() {
		doReturn(IntStream.range(0, 20).mapToObj(i -> new QueuedComputeChunkIdsEntity(electionEventId, verificationCardSetId, i)).toList())
				.when(queuedComputeChunkIdsRepository)
				.findAllByElectionEventIdAndVerificationCardSetIdOrderByChunkId(electionEventId, verificationCardSetId);
		final ImmutableList<Integer> chunkIds = IntStream.range(0, 20).boxed().collect(toImmutableList());
		assertEquals(chunkIds, queuedComputeChunkIdsService.getQueuedComputeChunkIds(electionEventId, verificationCardSetId));
	}
}
