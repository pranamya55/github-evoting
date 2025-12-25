/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.compute;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.MoreCollectors;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.configuration.setupvoting.ComputingStatus;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceContext;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceService;
import ch.post.it.evoting.votingserver.messaging.MessageHandler;
import ch.post.it.evoting.votingserver.messaging.Serializer;
import ch.post.it.evoting.votingserver.process.configuration.EncLongCodeShareEntity;
import ch.post.it.evoting.votingserver.process.configuration.EncLongCodeShareRepository;

@Service
public class ComputeEncryptedLongReturnCodeSharesService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ComputeEncryptedLongReturnCodeSharesService.class);

	private final MessageHandler messageHandler;
	private final IdempotenceService<IdempotenceContext> idempotenceService;
	private final EncLongCodeShareRepository encLongCodeShareRepository;
	private final QueuedComputeChunkIdsService queuedComputeChunkIdsService;
	private final Serializer serializer;

	public ComputeEncryptedLongReturnCodeSharesService(
			final MessageHandler messageHandler,
			final IdempotenceService<IdempotenceContext> idempotenceService,
			final EncLongCodeShareRepository encLongCodeShareRepository,
			final QueuedComputeChunkIdsService queuedComputeChunkIdsService,
			final Serializer serializer) {
		this.messageHandler = messageHandler;
		this.idempotenceService = idempotenceService;
		this.encLongCodeShareRepository = encLongCodeShareRepository;
		this.queuedComputeChunkIdsService = queuedComputeChunkIdsService;
		this.serializer = serializer;
	}

	public void onRequest(final SetupComponentVerificationDataPayload setupComponentVerificationDataPayload) {
		checkNotNull(setupComponentVerificationDataPayload);

		final String electionEventId = setupComponentVerificationDataPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentVerificationDataPayload.getVerificationCardSetId();
		final int chunkId = setupComponentVerificationDataPayload.getChunkId();

		idempotenceService.execute(IdempotenceContext.COMPUTE_CHUNK, String.format("%s-%s-%s", electionEventId, verificationCardSetId, chunkId),
				setupComponentVerificationDataPayload,
				() -> {
					queuedComputeChunkIdsService.saveQueuedComputeChunkId(electionEventId, verificationCardSetId, chunkId);
					// Request the generation of the EncryptedLongReturnCodeShares to the control components.
					messageHandler.sendMessage(setupComponentVerificationDataPayload);
				});
	}

	public ComputingStatus getComputingStatus(final String electionEventId, final String verificationCardSetId,
			final int chunkCount) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkArgument(chunkCount >= 0);

		final int expectedCount = ControlComponentNode.ids().size() * chunkCount;
		final long actualCount = encLongCodeShareRepository.countByVerificationCardSetId(verificationCardSetId);

		LOGGER.debug("Asked for computing status. [electionEventId: {}, verificationCardSetId: {}, expectedCount: {}, actualCount: {}]",
				electionEventId, verificationCardSetId, expectedCount, actualCount);

		if (actualCount < expectedCount) {
			return ComputingStatus.COMPUTING;
		} else if (actualCount == expectedCount) {
			return ComputingStatus.COMPUTED;
		} else {
			return ComputingStatus.COMPUTING_ERROR;
		}
	}

	public int extractNodeId(final ControlComponentCodeSharesPayload controlComponentCodeSharesPayload) {
		checkNotNull(controlComponentCodeSharesPayload);

		return controlComponentCodeSharesPayload.getNodeId();
	}

	public ControlComponentCodeSharesPayload deserialize(final ImmutableByteArray messageBytes) {
		checkNotNull(messageBytes);

		return serializer.deserialize(messageBytes, ControlComponentCodeSharesPayload.class);
	}

	public void onResponse(final String correlationId, final ImmutableList<ControlComponentCodeSharesPayload> controlComponentCodeSharesPayloads) {
		checkNotNull(correlationId);
		checkNotNull(controlComponentCodeSharesPayloads);
		final ControlComponentCodeSharesPayload controlComponentCodeSharesPayload = controlComponentCodeSharesPayloads.stream()
				.collect(MoreCollectors.onlyElement());

		saveControlComponentCodeSharesPayload(controlComponentCodeSharesPayload);

		final String contextId = getContextId(controlComponentCodeSharesPayload);

		LOGGER.info("Received and saved EncLongCodeShares calculation response. [contextId: {}, correlationId: {}]", contextId, correlationId);
	}

	public void saveControlComponentCodeSharesPayload(final ControlComponentCodeSharesPayload controlComponentCodeSharesPayload) {
		checkNotNull(controlComponentCodeSharesPayload);

		final String electionEventId = controlComponentCodeSharesPayload.getElectionEventId();
		final String verificationCardSetId = controlComponentCodeSharesPayload.getVerificationCardSetId();
		final int chunkId = controlComponentCodeSharesPayload.getChunkId();
		final int nodeId = controlComponentCodeSharesPayload.getNodeId();

		final ImmutableByteArray encLongCodeShare = serializer.serialize(controlComponentCodeSharesPayload);

		final EncLongCodeShareEntity encLongCodeShareEntity = new EncLongCodeShareEntity(verificationCardSetId, chunkId, nodeId, encLongCodeShare);
		encLongCodeShareRepository.save(encLongCodeShareEntity);

		LOGGER.debug("Saved enc long code share entity. [electionEventId: {}, verificationCardSetId: {}, chunkId: {}, nodeId: {}]",
				electionEventId, verificationCardSetId, chunkId, nodeId);
	}

	private String getContextId(final ControlComponentCodeSharesPayload controlComponentCodeSharesPayload) {
		checkNotNull(controlComponentCodeSharesPayload);

		final String electionEventId = controlComponentCodeSharesPayload.getElectionEventId();
		final String verificationCardSetId = controlComponentCodeSharesPayload.getVerificationCardSetId();
		final int chunkId = controlComponentCodeSharesPayload.getChunkId();

		return String.join("-", ImmutableList.of(electionEventId, verificationCardSetId, String.valueOf(chunkId)));
	}
}
