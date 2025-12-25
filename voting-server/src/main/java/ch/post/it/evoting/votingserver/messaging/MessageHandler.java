/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.messaging;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.domain.SharedQueue.CONTROL_COMPONENTS_ADDRESS;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_MESSAGE_TYPE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_NODE_ID;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_TENANT_ID;
import static ch.post.it.evoting.domain.SharedQueue.VOTING_SERVER_ADDRESS;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.activemq.artemis.api.core.Message.HDR_DUPLICATE_DETECTION_ID;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.MoreCollectors;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;

@Component
public class MessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandler.class);

	private final List<Configuration<?, ?>> configurations;
	private final InProgressMessageService inProgressMessageService;
	private final EntityManager entityManager;
	private final MessageHandler self;
	private final ContextHolder contextHolder;
	private final JmsTemplate mutlicastJmsTemplate;
	private final Serializer serializer;

	public MessageHandler(
			@Qualifier("multicastJmsTemplate")
			final JmsTemplate multicastJmsTemplate,
			final Serializer serializer,
			@Lazy
			final List<Configuration<?, ?>> configurations,
			final InProgressMessageService inProgressMessageService,
			final EntityManager entityManager,
			@Lazy
			final MessageHandler messageHandler,
			final ContextHolder contextHolder) {
		this.mutlicastJmsTemplate = multicastJmsTemplate;
		this.serializer = serializer;
		this.configurations = configurations;
		this.inProgressMessageService = inProgressMessageService;
		this.entityManager = entityManager;
		this.self = messageHandler;
		this.contextHolder = contextHolder;
	}

	public String generateCorrelationId() {
		return UUID.randomUUID().toString();
	}

	/**
	 * As this method is transactional but contains a non-transactional operation (send to queue), it must be the last operation done in the current
	 * transaction. The transaction is flushed before the message is sent.
	 */
	@Transactional
	public <T> String sendMessage(final T msg, final String correlationId) {
		checkNotNull(correlationId);
		return sendMessage(msg, correlationId, ControlComponentNode.ids());
	}

	/**
	 * As this method is transactional but contains a non-transactional operation (send to queue), it must be the last operation done in the current
	 * transaction. The transaction is flushed before the message is sent.
	 */
	@Transactional
	public <T> String sendMessage(final T msg) {
		return sendMessage(msg, generateCorrelationId(), ControlComponentNode.ids());
	}

	/**
	 * As this method is transactional but contains a non-transactional operation (send to queue), it must be the last operation done in the current
	 * transaction. The transaction is flushed before the message is sent.
	 */
	@Transactional
	public <T> String sendMessage(final T msg, final Integer nodeId) {
		return sendMessage(msg, generateCorrelationId(), ImmutableSet.of(nodeId));
	}

	/**
	 * As this method is transactional but contains a non-transactional operation (send to queue), it must be the last operation done in the current
	 * transaction. The transaction is flushed before the message is sent.
	 */
	@Transactional
	public <T> String sendMessage(final T msg, final String correlationId, final Integer nodeId) {
		return sendMessage(msg, correlationId, ImmutableSet.of(nodeId));
	}

	@SuppressWarnings("unchecked")
	protected <T, U> String sendMessage(final T msg, final String correlationId, final ImmutableSet<Integer> nodes) {
		checkNotNull(msg);
		checkNotNull(nodes);

		final Configuration<T, U> configuration = (Configuration<T, U>) configurations.stream()
				.filter(config -> config.requestMessageType().equals(msg.getClass()))
				.collect(MoreCollectors.onlyElement());

		if (configuration.broadcastRequest()) {
			checkState(nodes.containsAll(ControlComponentNode.ids()) && nodes.size() == ControlComponentNode.ids().size());
		} else {
			checkState(nodes.size() == 1);
		}

		final String requestMessageType = msg.getClass().getName();
		final String contextId = configuration.contextIdExtractor().apply(msg);

		final Optional<String> optionalInProgressCorrelationId = inProgressMessageService.getCorrelationId(requestMessageType, contextId);
		if (optionalInProgressCorrelationId.isPresent()) {
			// There is already a request in progress with the same type and contextId.
			// Do not send the message again to the control components, just return the correlationId of the existing in progress request.
			final String inProgressCorrelationId = optionalInProgressCorrelationId.get();
			LOGGER.warn(
					"Request already in progress, not sending a duplicate request, returning in progress correlationId. [requestMessageType: {}, contextId: {}, correlationId: {}]",
					requestMessageType, contextId, inProgressCorrelationId);
			return inProgressCorrelationId;
		}

		inProgressMessageService.storeRequests(correlationId, nodes, requestMessageType, contextId);

		final byte[] payload = serializer.serialize(msg).elements();

		// Ensure the DB is up-to-date before executing non-transactional task (i.e. sending the message)
		entityManager.flush();
		// Ensure that no unintentional changes to already flushed entities will be persisted later.
		entityManager.clear();

		// As this call is not transactional, it must be the last operation done in the current transaction.
		mutlicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, payload, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, msg.getClass().getName());
			jmsMessage.setStringProperty(HDR_DUPLICATE_DETECTION_ID.toString(), UUID.randomUUID().toString());
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			// Mixing case.
			if (!configuration.broadcastRequest()) {
				jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, String.valueOf(nodes.iterator().next()));
			}
			return jmsMessage;
		});

		return correlationId;
	}

	@JmsListener(destination = VOTING_SERVER_ADDRESS, containerFactory = "customFactory")
	public <T, U> void onMessage(final Message message) throws JMSException {
		try {
			checkNotNull(message);
			contextHolder.setTenantId(checkNotNull(message.getStringProperty(MESSAGE_HEADER_TENANT_ID)));
			final String messageType = checkNotNull(message.getStringProperty(MESSAGE_HEADER_MESSAGE_TYPE));
			final String correlationId = checkNotNull(message.getJMSCorrelationID());

			LOGGER.info("Received new response. [messageType: {}, correlationId: {}]", messageType, correlationId);

			final ImmutableByteArray messageBody = new ImmutableByteArray(message.getBody(byte[].class));

			@SuppressWarnings("unchecked")
			final Configuration<T, U> configuration = (Configuration<T, U>) getConfiguration(messageType);

			final U payload = configuration.responseDeserializer().apply(messageBody);
			final int nodeId = configuration.nodeIdExtractor().apply(payload);
			final BiConsumer<String, ImmutableList<U>> responseHandler = configuration.ResponseHandler();
			final boolean aggregateResults = configuration.aggregateResponseResults();

			self.processMessage(aggregateResults, correlationId, nodeId, messageBody, messageType, responseHandler, payload);
		} finally {
			contextHolder.clear();
		}
	}

	@Transactional
	public <U> void processMessage(final boolean aggregateResults, final String correlationId, final int nodeId, final ImmutableByteArray messageBody,
			final String messageType, final BiConsumer<String, ImmutableList<U>> responseHandler, final U payload) {
		if (aggregateResults) {
			//Lock all inProgressMessages with the same correlationId to avoid concurrent modifications and having an incorrect count
			inProgressMessageService.storeResponse(correlationId, nodeId, messageBody, messageType);
			final int count = inProgressMessageService.countAllInProgressMessagesWithResponsePayload(correlationId);
			if (count == ControlComponentNode.ids().size()) {
				LOGGER.info("All nodes have returned their contributions. [correlationId: {}]", correlationId);
				handleResponseForAggregatedMessages(correlationId, messageType);
			} else {
				LOGGER.info("Not all nodes have returned their contributions. [correlationId: {}, count: {}]", correlationId, count);
			}
		} else {
			LOGGER.info("Response received. [correlationId: {}, nodeId: {}]", correlationId, nodeId);
			inProgressMessageService.removeInProgressMessage(correlationId, nodeId);
			responseHandler.accept(correlationId, ImmutableList.of(payload));
		}
	}

	private <T, U> void handleResponseForAggregatedMessages(final String correlationId, final String messageType) {
		checkNotNull(correlationId);
		checkNotNull(messageType);

		@SuppressWarnings("unchecked")
		final Configuration<T, U> configuration = (Configuration<T, U>) getConfiguration(messageType);
		final BiConsumer<String, ImmutableList<U>> responseHandler = configuration.ResponseHandler();

		final ImmutableList<InProgressMessage> allNodesResponses = inProgressMessageService.getAllNodesResponses(correlationId);

		final ImmutableList<U> payloads = allNodesResponses.stream()
				.map(InProgressMessage::getResponsePayload)
				.map(configuration.responseDeserializer)
				.sorted(Comparator.comparingInt(configuration.nodeIdExtractor::apply))
				.collect(toImmutableList());

		checkState(payloads.size() == ControlComponentNode.ids().size(),
				"The number of payloads does not correspond to the number of control component nodes");

		inProgressMessageService.removeInProgressMessages(correlationId);
		responseHandler.accept(correlationId, payloads);

		LOGGER.info("Response for aggregated messages handled. [correlationId: {}]", correlationId);
	}

	private Configuration<?, ?> getConfiguration(final String responseMessageType) {
		final Class<?> messageClass;
		try {
			messageClass = Class.forName(responseMessageType);
		} catch (final ClassNotFoundException e) {
			throw new IllegalArgumentException(
					String.format("The given request message type is unknown. [responseMessageType: %s]", responseMessageType), e);
		}

		return configurations.stream()
				.filter(c -> messageClass.equals(c.responseMessageType()))
				.collect(MoreCollectors.onlyElement());
	}

	public record Configuration<T, U>(
			Class<T> requestMessageType,
			boolean broadcastRequest,
			Function<T, String> contextIdExtractor,
			Class<U> responseMessageType,
			Function<U, Integer> nodeIdExtractor,
			Function<ImmutableByteArray, U> responseDeserializer, BiConsumer<String, ImmutableList<U>> ResponseHandler,
			boolean aggregateResponseResults) {
	}
}


