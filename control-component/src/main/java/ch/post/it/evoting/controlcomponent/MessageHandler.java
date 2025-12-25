/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

import static ch.post.it.evoting.domain.SharedQueue.CONTROL_COMPONENT_QUEUE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_MESSAGE_TYPE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_TENANT_ID;
import static ch.post.it.evoting.domain.SharedQueue.VOTING_SERVER_ADDRESS;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.google.common.collect.MoreCollectors;

import ch.post.it.evoting.controlcomponent.commandmessaging.Context;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;

@Component
public class MessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandler.class);

	private final List<Configuration<?, ?>> configurations;
	private final ExactlyOnceCommandExecutor exactlyOnceCommandExecutor;
	private final JmsTemplate jmsTemplate;
	private final ContextHolder contextHolder;
	private final int defaultTransactionTimeout;
	private final Hash hash;

	@Value("${nodeID}")
	private int nodeId;

	MessageHandler(
			final List<Configuration<?, ?>> configurations,
			final ExactlyOnceCommandExecutor exactlyOnceCommandExecutor,
			final JmsTemplate jmsTemplate,
			final ContextHolder contextHolder,
			@Value("${spring.transaction.default-timeout}")
			final int defaultTransactionTimeout,
			final Hash hash) {
		this.configurations = configurations;
		this.exactlyOnceCommandExecutor = exactlyOnceCommandExecutor;
		this.jmsTemplate = jmsTemplate;
		this.defaultTransactionTimeout = defaultTransactionTimeout;
		this.contextHolder = contextHolder;
		this.hash = hash;
	}

	@JmsListener(
			destination = CONTROL_COMPONENT_QUEUE + "${nodeID}",
			concurrency = "${jms.listener.concurrency}",
			containerFactory = "customFactory"
	)
	public <T extends Hashable, U extends Hashable> void onMessage(final Message message) throws JMSException {
		try {
			final ImmutableByteArray messageBody = new ImmutableByteArray(checkNotNull(message).getBody(byte[].class));
			contextHolder.setTenantId(checkNotNull(message.getStringProperty(MESSAGE_HEADER_TENANT_ID)));
			final String requestMessageType = checkNotNull(message.getStringProperty(MESSAGE_HEADER_MESSAGE_TYPE));
			final String correlationId = checkNotNull(message.getJMSCorrelationID());

			LOGGER.info("Received new request. [requestMessageType: {}, correlationId: {}, nodeId: {}]", requestMessageType, correlationId, nodeId);

			@SuppressWarnings("unchecked")
			final Configuration<T, U> configuration = (Configuration<T, U>) getConfiguration(requestMessageType);

			final T requestPayload = configuration.requestDeserializer().apply(messageBody);
			final ImmutableByteArray requestPayloadHash = hash.recursiveHash(requestPayload);

			checkState(configuration.signatureValidator().apply(requestPayload),
					"The signature is not valid. [requestMessageType: %s, correlationId: %s, nodeId: %s]", requestMessageType, correlationId, nodeId);

			final String contextId = configuration.contextIdExtractor().apply(requestPayload);
			final Context context = configuration.context();
			final Consumer<T> preValidationTask = configuration.preValidationTask();
			final Function<T, U> exactlyOnceTask = configuration.exactlyOnceTask();
			final Function<T, U> replayTask = configuration.replayTask();
			final Function<U, ImmutableByteArray> responseSerializer = configuration.responseSerializer();
			final int transactionTimeout = configuration.transactionTimeout().orElse(defaultTransactionTimeout);

			// Exactly once process the request payload.
			final ExactlyOnceCommand<U> exactlyOnceCommand = new ExactlyOnceCommand.Builder<U>()
					.setCorrelationId(correlationId)
					.setContextId(contextId)
					.setContext(context.toString())
					.setPreValidationTask(() -> preValidationTask.accept(requestPayload))
					.setTask(() -> exactlyOnceTask.apply(requestPayload))
					.setReplayTask(() -> replayTask.apply(requestPayload))
					.setSerializer(responseSerializer)
					.setRequestPayloadHash(requestPayloadHash)
					.setTransactionTimeout(transactionTimeout)
					.build();

			final ImmutableByteArray responsePayload = exactlyOnceCommandExecutor.process(exactlyOnceCommand);
			final String responseMessageTypeName = configuration.responseType.getName();

			jmsTemplate.convertAndSend(VOTING_SERVER_ADDRESS, responsePayload.elements(), jmsMessage -> {
				jmsMessage.setJMSCorrelationID(correlationId);
				jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, responseMessageTypeName);
				jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
				return jmsMessage;
			});

			LOGGER.info("Response sent. [requestMessageType: {}, responseMessageType: {}, correlationId: {}, nodeId: {}]", requestMessageType,
					responseMessageTypeName, correlationId, nodeId);
		} finally {
			contextHolder.clear();
		}
	}

	private Configuration<?, ?> getConfiguration(final String requestMessageType) {
		final Class<?> messageClass;
		try {
			messageClass = Class.forName(requestMessageType);
		} catch (final ClassNotFoundException e) {
			throw new IllegalArgumentException(
					String.format("The given request message type is unknown. [requestMessageType: %s]", requestMessageType), e);
		}

		return configurations.stream()
				.filter(c -> messageClass.equals(c.requestType()))
				.collect(MoreCollectors.onlyElement());
	}

	public record Configuration<T, U>(
			Class<T> requestType,
			Function<T, Boolean> signatureValidator,
			Context context,
			Consumer<T> preValidationTask,
			Function<T, U> exactlyOnceTask,
			Function<T, U> replayTask,
			Function<T, String> contextIdExtractor,
			Function<ImmutableByteArray, T> requestDeserializer,
			Class<U> responseType,
			Function<U, ImmutableByteArray> responseSerializer,
			Optional<Integer> transactionTimeout) {

		public Configuration(final Class<T> requestType, final Function<T, Boolean> signatureValidator, final Context context,
				final Function<T, U> exactlyOnceTask, final Function<T, U> replayTask, final Function<T, String> contextIdExtractor,
				final Function<ImmutableByteArray, T> requestDeserializer, final Class<U> responseType,
				final Function<U, ImmutableByteArray> responseSerializer) {
			this(requestType, signatureValidator, context, ignored -> {}, exactlyOnceTask, replayTask, contextIdExtractor,
					requestDeserializer, responseType, responseSerializer, Optional.empty());
		}
	}

}


