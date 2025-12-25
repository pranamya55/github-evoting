/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.requestcckeys;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.votingserver.messaging.MessageHandler;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionCompletableFuture;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionService;
import ch.post.it.evoting.votingserver.messaging.Serializer;

@Service
public class RequestCcKeysService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestCcKeysService.class);

	private final Serializer serializer;
	private final MessageHandler messageHandler;
	private final ResponseCompletionService responseCompletionService;

	public RequestCcKeysService(
			final Serializer serializer,
			final MessageHandler messageHandler,
			final ResponseCompletionService responseCompletionService) {
		this.serializer = serializer;
		this.messageHandler = messageHandler;
		this.responseCompletionService = responseCompletionService;
	}

	/**
	 * Uploads the election event context to the Control Components to generate the Control Component public keys.
	 *
	 * @param electionEventContextPayload the request payload. Must be non null.
	 * @return the correlation id of the request.
	 * @throws NullPointerException if {@code electionEventContextPayload} is null.
	 */
	public String onRequest(final ElectionEventContextPayload electionEventContextPayload) {
		checkNotNull(electionEventContextPayload);

		final String correlationId = messageHandler.sendMessage(electionEventContextPayload);

		final String electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();
		LOGGER.info("Election event context sent to the control components. [electionEventId: {}, correlationId: {}]", electionEventId,
				correlationId);

		return correlationId;
	}

	/**
	 * Waits for the response from the Control Components to ensure the election event context has been successfully processed.
	 *
	 * @param correlationId the correlation id on which to wait. Must be non-null.
	 * @return the list of control component public keys.
	 * @throws NullPointerException if {@code correlationId} is null.
	 */
	public ImmutableList<ControlComponentPublicKeysPayload> waitForResponse(final String correlationId) {
		checkNotNull(correlationId);

		final ResponseCompletionCompletableFuture<ImmutableList<ControlComponentPublicKeysPayload>> completableFuture = responseCompletionService.registerForResponseCompletion(
				correlationId, new TypeReference<>() {});

		return completableFuture.get();
	}

	/**
	 * Defines the behavior when the response from the Control Components is received.
	 *
	 * @param correlationId                      the correlation id of the request, to unblock it. Must be non-null.
	 * @param controlComponentPublicKeysPayloads the response from the Control Components. Must be non-null.
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalArgumentException if the size of the list of control component public keys is not the size of
	 *                                  {@link ControlComponentNode#ids()}.
	 */
	public void onResponse(final String correlationId, final ImmutableList<ControlComponentPublicKeysPayload> controlComponentPublicKeysPayloads) {
		checkNotNull(correlationId);
		checkNotNull(controlComponentPublicKeysPayloads);

		checkArgument(controlComponentPublicKeysPayloads.size() == ControlComponentNode.ids().size());

		responseCompletionService.notifyResponseCompleted(correlationId, controlComponentPublicKeysPayloads);

		final String electionEventId = controlComponentPublicKeysPayloads.get(0).getElectionEventId();
		LOGGER.info("Successfully uploaded election event context and requested CC keys. [electionEventId: {}, correlationId: {}]",
				electionEventId, correlationId);
	}

	/**
	 * Defines how to extract the node id from the response payload.
	 *
	 * @param controlComponentPublicKeysPayload the payload from which to extract the node id. Must be non-null.
	 * @return the extracted node id.
	 * @throws NullPointerException if {@code controlComponentPublicKeysPayload} is null.
	 */
	public int extractNodeId(final ControlComponentPublicKeysPayload controlComponentPublicKeysPayload) {
		checkNotNull(controlComponentPublicKeysPayload);
		return controlComponentPublicKeysPayload.getControlComponentPublicKeys().nodeId();
	}

	/**
	 * Defines how to deserialize the response payload.
	 *
	 * @param messageBytes the bytes to deserialize. Must be non-null.
	 * @return the deserialized payload.
	 * @throws NullPointerException if {@code messageBytes} is null.
	 */
	public ControlComponentPublicKeysPayload deserialize(final ImmutableByteArray messageBytes) {
		checkNotNull(messageBytes);
		return serializer.deserialize(messageBytes, ControlComponentPublicKeysPayload.class);
	}
}
