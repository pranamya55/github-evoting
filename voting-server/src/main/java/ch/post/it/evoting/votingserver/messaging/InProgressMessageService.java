/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.messaging;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.google.common.collect.MoreCollectors;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

@Service
public class InProgressMessageService {
	private final InProgressMessageRepository inProgressMessageRepository;

	public InProgressMessageService(final InProgressMessageRepository inProgressMessageRepository) {
		this.inProgressMessageRepository = inProgressMessageRepository;
	}

	public void storeRequests(final String correlationId, final ImmutableSet<Integer> nodes, final String requestMessageType,
			final String contextId) {
		checkNotNull(correlationId);
		checkArgument(ControlComponentNode.ids().containsAll(nodes));
		checkNotNull(requestMessageType);
		checkNotNull(contextId);

		final ImmutableList<InProgressMessage> inProgressMessages = nodes.stream()
				.map(nodeId -> new InProgressMessage(correlationId, nodeId, requestMessageType, contextId))
				.collect(toImmutableList());

		inProgressMessageRepository.saveAll(inProgressMessages);
	}

	public void storeResponse(final String correlationId, final int nodeId, final ImmutableByteArray payload, final String responseMessageType) {
		checkNotNull(correlationId);
		checkArgument(ControlComponentNode.ids().contains(nodeId));
		checkNotNull(payload);
		checkNotNull(responseMessageType);

		//Lock all for lines with the same correlationId to avoid concurrent modifications within the same correlationId
		final List<InProgressMessage> inProgressMessages = inProgressMessageRepository.findAllByCorrelationIdOrderByNodeId(correlationId);

		final InProgressMessage inProgressMessage = inProgressMessages.stream().filter(ipm -> ipm.getNodeId() == nodeId)
				.collect(MoreCollectors.onlyElement());

		inProgressMessage.setResponsePayload(payload);
		inProgressMessage.setResponseMessageType(responseMessageType);
		inProgressMessageRepository.save(inProgressMessage);
	}

	/**
	 * Get the correlationId for a given requestMessageType and contextId, if it exists.
	 *
	 * @param requestMessageType the request message type. Must not be null.
	 * @param contextId          the context id. Must not be null.
	 * @return the correlationId, if it exists.
	 * @throws NullPointerException if any of the parameters is null.
	 */
	public Optional<String> getCorrelationId(final String requestMessageType, final String contextId) {
		checkNotNull(requestMessageType);
		checkNotNull(contextId);

		return inProgressMessageRepository.findFirstByRequestMessageTypeAndContextId(requestMessageType, contextId)
				.map(InProgressMessage::getCorrelationId);
	}

	public int countAllInProgressMessagesWithResponsePayload(final String correlationId) {
		checkNotNull(correlationId);
		return inProgressMessageRepository.countByCorrelationIdAndResponsePayloadIsNotNull(correlationId);
	}

	public ImmutableList<InProgressMessage> getAllNodesResponses(final String correlationId) {
		checkNotNull(correlationId);
		return ImmutableList.from(inProgressMessageRepository.findAllByCorrelationIdAndResponsePayloadIsNotNull(correlationId));
	}

	public void removeInProgressMessage(final String correlationId, final int nodeId) {
		checkNotNull(correlationId);
		checkArgument(ControlComponentNode.ids().contains(nodeId));

		final InProgressMessageId inProgressMessageId = new InProgressMessageId(correlationId, nodeId);
		inProgressMessageRepository.deleteById(inProgressMessageId);
	}

	public void removeInProgressMessages(final String correlationId) {
		checkNotNull(correlationId);
		inProgressMessageRepository.deleteAllByCorrelationId(correlationId);
	}
}
