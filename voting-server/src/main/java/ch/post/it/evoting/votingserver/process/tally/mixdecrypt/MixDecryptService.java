/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.tally.mixdecrypt;

import static ch.post.it.evoting.evotinglibraries.domain.validations.ControlComponentVotesHashPayloadValidation.validate;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.MoreCollectors;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineRequestPayload;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineResponsePayload;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentVotesHashPayload;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;
import ch.post.it.evoting.votingserver.messaging.MessageHandler;
import ch.post.it.evoting.votingserver.messaging.Serializer;

@Service
public class MixDecryptService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixDecryptService.class);
	private final Serializer serializer;
	private final MessageHandler messageHandler;
	private final MixDecryptOnlinePayloadService mixDecryptOnlinePayloadService;

	public MixDecryptService(
			final Serializer serializer,
			final MessageHandler messageHandler,
			final MixDecryptOnlinePayloadService mixDecryptOnlinePayloadService) {
		this.serializer = serializer;
		this.messageHandler = messageHandler;
		this.mixDecryptOnlinePayloadService = mixDecryptOnlinePayloadService;
	}

	public void onRequest(final String electionEventId, final String ballotBoxId,
			final ImmutableList<ControlComponentVotesHashPayload> controlComponentVotesHashPayloads) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		validate(electionEventId, ballotBoxId, controlComponentVotesHashPayloads);

		final int initialNodeId = 1;
		final ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads = ImmutableList.emptyList();

		final MixDecryptOnlineRequestPayload mixDecryptOnlineRequestPayload = new MixDecryptOnlineRequestPayload(electionEventId, ballotBoxId,
				initialNodeId, controlComponentVotesHashPayloads, controlComponentShufflePayloads);

		LOGGER.info("Starting mixing. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		messageHandler.sendMessage(mixDecryptOnlineRequestPayload, initialNodeId);
	}

	@Transactional
	public void onResponse(final String correlationId, final ImmutableList<MixDecryptOnlineResponsePayload> mixDecryptOnlineResponsePayloads) {
		checkNotNull(correlationId);
		checkNotNull(mixDecryptOnlineResponsePayloads);
		final MixDecryptOnlineResponsePayload mixDecryptOnlineResponsePayload = mixDecryptOnlineResponsePayloads.stream()
				.collect(MoreCollectors.onlyElement());

		final String electionEventId = mixDecryptOnlineResponsePayload.controlComponentBallotBoxPayload().getElectionEventId();
		final String ballotBoxId = mixDecryptOnlineResponsePayload.controlComponentBallotBoxPayload().getBallotBoxId();
		final int nodeId = mixDecryptOnlineResponsePayload.controlComponentBallotBoxPayload().getNodeId();

		// Save payloads
		final ControlComponentBallotBoxPayload controlComponentBallotBoxPayload = mixDecryptOnlineResponsePayload.controlComponentBallotBoxPayload();
		mixDecryptOnlinePayloadService.saveControlComponentBallotBoxPayload(controlComponentBallotBoxPayload);

		final ControlComponentShufflePayload controlComponentShufflePayload = mixDecryptOnlineResponsePayload.controlComponentShufflePayload();
		mixDecryptOnlinePayloadService.saveControlComponentShufflePayload(controlComponentShufflePayload);

		LOGGER.info(
				"Control component ballot box and shuffle payloads are successfully saved for node {}. [electionEventId:{}, ballotBoxId:{}, correlationId:{}]",
				nodeId, electionEventId, ballotBoxId, correlationId);

		// Check mixing progress
		if (nodeId < ControlComponentNode.ids().size()) {

			// Prepare request for the next node
			final ImmutableList<ControlComponentShufflePayload> shufflePayloads = mixDecryptOnlinePayloadService.getControlComponentShufflePayloadsOrderByNodeId(
					electionEventId, ballotBoxId);
			final ImmutableList<ControlComponentVotesHashPayload> controlComponentVotesHashPayloads = mixDecryptOnlinePayloadService.getControlComponentVotesHashPayloads(
					electionEventId, ballotBoxId);

			final int nextNodeId = nodeId + 1;
			final MixDecryptOnlineRequestPayload mixDecryptOnlineRequestPayload = new MixDecryptOnlineRequestPayload(electionEventId, ballotBoxId,
					nextNodeId, controlComponentVotesHashPayloads, shufflePayloads);

			messageHandler.sendMessage(mixDecryptOnlineRequestPayload, correlationId, nextNodeId);
			LOGGER.info("Sent next mixing request to node {} [electionEventId:{}, ballotBoxId:{}, correlationId:{}]", nextNodeId, electionEventId,
					ballotBoxId, correlationId);

		} else {

			// All nodes have sent a response, process is completed
			LOGGER.info("Successfully mixed the ballot box. [electionEventId:{}, ballotBoxId:{}, correlationId:{}]", electionEventId, ballotBoxId,
					correlationId);

		}
	}

	public int extractNodeId(final MixDecryptOnlineResponsePayload mixDecryptOnlineResponsePayload) {
		checkNotNull(mixDecryptOnlineResponsePayload);

		return mixDecryptOnlineResponsePayload.controlComponentBallotBoxPayload().getNodeId();
	}

	public MixDecryptOnlineResponsePayload deserialize(final ImmutableByteArray messageBytes) {
		checkNotNull(messageBytes);
		return serializer.deserialize(messageBytes, MixDecryptOnlineResponsePayload.class);
	}
}
