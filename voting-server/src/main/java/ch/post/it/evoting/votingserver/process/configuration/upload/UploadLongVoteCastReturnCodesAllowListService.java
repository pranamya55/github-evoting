/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.upload;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.ContextIdExtractor;
import ch.post.it.evoting.domain.configuration.setupvoting.LongVoteCastReturnCodesAllowListResponsePayload;
import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.votingserver.messaging.MessageHandler;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionService;
import ch.post.it.evoting.votingserver.messaging.Serializer;

@Service
public class UploadLongVoteCastReturnCodesAllowListService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadLongVoteCastReturnCodesAllowListService.class);

	private final Serializer serializer;
	private final MessageHandler messageHandler;
	private final ResponseCompletionService responseCompletionService;

	public UploadLongVoteCastReturnCodesAllowListService(
			final Serializer serializer,
			final MessageHandler messageHandler,
			final ResponseCompletionService responseCompletionService) {
		this.serializer = serializer;
		this.messageHandler = messageHandler;
		this.responseCompletionService = responseCompletionService;
	}

	public String onRequest(final String electionEventId,
			final String verificationCardSetId,
			final SetupComponentLVCCAllowListPayload setupComponentLVCCAllowListPayload) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkNotNull(setupComponentLVCCAllowListPayload);
		checkArgument(electionEventId.equals(setupComponentLVCCAllowListPayload.getElectionEventId()), "Election event id mismatch.");
		checkArgument(verificationCardSetId.equals(setupComponentLVCCAllowListPayload.getVerificationCardSetId()),
				"Verification card set id mismatch.");

		final String contextId = ContextIdExtractor.extract(setupComponentLVCCAllowListPayload);

		final String correlationId = messageHandler.sendMessage(setupComponentLVCCAllowListPayload);

		LOGGER.info("Sent the long vote cast return codes allow list for upload. [contextId: {}, correlationId: {}]", contextId, correlationId);

		return correlationId;
	}

	public void waitForResponse(final String correlationId) {
		checkNotNull(correlationId);
		responseCompletionService.registerForCompletion(correlationId).get();
	}

	public void onResponse(final String correlationId,
			final ImmutableList<LongVoteCastReturnCodesAllowListResponsePayload> longVoteCastReturnCodesAllowListResponsePayloads) {
		checkNotNull(correlationId);
		checkNotNull(longVoteCastReturnCodesAllowListResponsePayloads);
		checkArgument(longVoteCastReturnCodesAllowListResponsePayloads.size() == ControlComponentNode.ids().size());

		final String contextId = ContextIdExtractor.extract(longVoteCastReturnCodesAllowListResponsePayloads.get(0));

		LOGGER.info("Successfully uploaded long vote cast return codes allow list. [contextId: {}, correlationId: {}]", contextId, correlationId);

		responseCompletionService.notifyCompleted(correlationId);
	}

	public int extractNodeId(
			final LongVoteCastReturnCodesAllowListResponsePayload longVoteCastReturnCodesAllowListResponsePayload) {
		checkNotNull(longVoteCastReturnCodesAllowListResponsePayload);

		return longVoteCastReturnCodesAllowListResponsePayload.nodeId();
	}

	public LongVoteCastReturnCodesAllowListResponsePayload deserializePayload(final ImmutableByteArray messageBytes) {
		checkNotNull(messageBytes);
		return serializer.deserialize(messageBytes, LongVoteCastReturnCodesAllowListResponsePayload.class);
	}
}
