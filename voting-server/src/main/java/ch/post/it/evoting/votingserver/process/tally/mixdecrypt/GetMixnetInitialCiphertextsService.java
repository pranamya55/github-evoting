/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.tally.mixdecrypt;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.ControlComponentVotesHashPayloadValidation.validate;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.tally.GetMixnetInitialCiphertextsRequestPayload;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentVotesHashPayload;
import ch.post.it.evoting.votingserver.messaging.MessageHandler;
import ch.post.it.evoting.votingserver.messaging.Serializer;

@Service
public class GetMixnetInitialCiphertextsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GetMixnetInitialCiphertextsService.class);

	private final Serializer serializer;
	private final MessageHandler messageHandler;
	private final MixDecryptOnlinePayloadService mixDecryptOnlinePayloadService;
	private final MixDecryptService mixDecryptService;

	public GetMixnetInitialCiphertextsService(
			final Serializer serializer,
			final MessageHandler messageHandler,
			final MixDecryptOnlinePayloadService mixDecryptOnlinePayloadService,
			final MixDecryptService mixDecryptService) {
		this.serializer = serializer;
		this.messageHandler = messageHandler;
		this.mixDecryptOnlinePayloadService = mixDecryptOnlinePayloadService;
		this.mixDecryptService = mixDecryptService;
	}

	public void onRequest(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		LOGGER.info("Starting GetMixnetInitialCiphertexts requests. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		messageHandler.sendMessage(new GetMixnetInitialCiphertextsRequestPayload(electionEventId, ballotBoxId));
	}

	@Transactional
	public void onResponse(final String correlationId, final ImmutableList<ControlComponentVotesHashPayload> controlComponentVotesHashPayloads) {
		checkNotNull(correlationId);
		checkNotNull(controlComponentVotesHashPayloads);

		checkArgument(controlComponentVotesHashPayloads.size() == ControlComponentNode.ids().size(),
				"The Control Component Votes Hash Payloads must have %s elements.",
				ControlComponentNode.ids().size());

		final ControlComponentVotesHashPayload controlComponentVotesHashPayload = controlComponentVotesHashPayloads.get(0);
		final String electionEventId = controlComponentVotesHashPayload.getElectionEventId();
		final String ballotBoxId = controlComponentVotesHashPayload.getBallotBoxId();

		validate(electionEventId, ballotBoxId, controlComponentVotesHashPayloads);

		final ImmutableList<ControlComponentVotesHashPayload> savedControlComponentVotesHashPayloads = mixDecryptOnlinePayloadService.getControlComponentVotesHashPayloads(
				electionEventId, ballotBoxId);
		final ImmutableList<ControlComponentVotesHashPayload> unsavedControlComponentVotesHashPayloads = controlComponentVotesHashPayloads.stream()
				.filter(payload -> !savedControlComponentVotesHashPayloads.contains(payload))
				.collect(toImmutableList());

		mixDecryptOnlinePayloadService.saveControlComponentVotesHashPayloads(unsavedControlComponentVotesHashPayloads);

		mixDecryptService.onRequest(electionEventId, ballotBoxId, controlComponentVotesHashPayloads);
	}

	public int extractNodeId(final ControlComponentVotesHashPayload controlComponentVotesHashPayload) {
		checkNotNull(controlComponentVotesHashPayload);
		return controlComponentVotesHashPayload.getNodeId();
	}

	public ControlComponentVotesHashPayload deserialize(final ImmutableByteArray messageBytes) {
		checkNotNull(messageBytes);
		return serializer.deserialize(messageBytes, ControlComponentVotesHashPayload.class);
	}

}
