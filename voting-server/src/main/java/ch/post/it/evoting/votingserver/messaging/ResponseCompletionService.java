/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.messaging;

import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_TENANT_ID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;

@Service
public class ResponseCompletionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResponseCompletionService.class);
	private static final String BROADCAST_TOPIC_EXCHANGE = "broadcast-topic-exchange";
	private static final String HEADER_MESSAGE_TYPE = "message_type";

	private final EntityManager entityManager;
	private final Serializer serializer;
	private final Cache<String, CachedElement<?>> cache;
	private final ContextHolder contextHolder;
	private final JmsTemplate multicastJmsTemplate;

	@Value("${responseCompletion.defaultTimeout:120}")
	private long defaultCompletionTimout;

	public ResponseCompletionService(
			final EntityManager entityManager,
			final Serializer serializer,
			@Qualifier("multicastJmsTemplate")
			final JmsTemplate multicastJmsTemplate,
			@Value("${responseCompletionService.cache-timeout:10}")
			final long cacheTimeout,
			final ContextHolder contextHolder) {
		this.entityManager = entityManager;
		this.serializer = serializer;
		this.multicastJmsTemplate = multicastJmsTemplate;
		this.cache = Caffeine.newBuilder()
				.expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
				.removalListener((correlationId, value, cause) -> {
					if (cause.wasEvicted()) {
						LOGGER.debug("Response completion evicted from cache. [correlationId : {}]", correlationId);
					}
				})
				.build();
		this.contextHolder = contextHolder;
	}

	public void notifyCompleted(final String correlationId) {
		checkNotNull(correlationId);
		notifyResponseCompleted(correlationId, new Empty());
	}

	public <T> void notifyResponseCompleted(final String correlationId, final T response) {
		notify(correlationId, response);
	}

	public void notifyException(final String correlationId, final RuntimeException exception) {
		notify(correlationId, exception);
	}

	private <T> void notify(final String correlationId, final T value) {
		checkNotNull(correlationId);
		checkNotNull(value);

		@SuppressWarnings("unchecked")
		final CachedElement<T> cachedElement = (CachedElement<T>) cache.getIfPresent(correlationId);

		if (cachedElement != null) {
			if (cachedElement.completableFuture().isRunning()) {
				if (value instanceof final RuntimeException runtimeException) {
					cachedElement.completableFuture().completeExceptionally(runtimeException);
				} else {
					cachedElement.completableFuture().complete(value);
				}
			}
			cache.invalidate(correlationId);
		} else {
			produceMessageToBroadcastQueue(correlationId, value);
		}
	}

	public <T> ResponseCompletionCompletableFuture<T> registerForResponseCompletion(final String correlationId, final Class<T> clazz) {
		checkNotNull(correlationId);
		checkNotNull(clazz);

		return registerForResponseCompletion(correlationId,
				() -> new CachedElement<>(new ResponseCompletionCompletableFuture<>(defaultCompletionTimout), clazz, null));
	}

	public <T> ResponseCompletionCompletableFuture<T> registerForResponseCompletion(final String correlationId,
			final TypeReference<T> typeReference) {
		checkNotNull(correlationId);
		checkNotNull(typeReference);

		return registerForResponseCompletion(correlationId,
				() -> new CachedElement<>(new ResponseCompletionCompletableFuture<>(defaultCompletionTimout), null, typeReference));
	}

	private <T> ResponseCompletionCompletableFuture<T> registerForResponseCompletion(final String correlationId,
			final Supplier<CachedElement<T>> cachedElementSupplier) {

		checkNotNull(correlationId);
		checkNotNull(cachedElementSupplier);

		@SuppressWarnings("unchecked")
		final CachedElement<T> cachedElement = (CachedElement<T>) cache.getIfPresent(correlationId);

		if (cachedElement != null) {
			if (cachedElement.completableFuture().isRunning()) {
				LOGGER.warn(
						"A registration for the given correlationId already exists and is still running. Returning the existing one. [correlationId: {}]",
						correlationId);
				return cachedElement.completableFuture();
			}

			LOGGER.warn("A registration for the given correlationId already exists and will be replaced. [correlationId: {}]", correlationId);
			cache.invalidate(correlationId);
		}

		@SuppressWarnings("unchecked")
		final ResponseCompletionCompletableFuture<T> responseCompletionCompletableFuture = (ResponseCompletionCompletableFuture<T>) cache.get(
				correlationId, string -> cachedElementSupplier.get()).completableFuture();

		return responseCompletionCompletableFuture;
	}

	public ResponseCompletionCompletableFuture<Empty> registerForCompletion(final String correlationId) {
		checkNotNull(correlationId);
		return registerForResponseCompletion(correlationId, Empty.class);
	}

	private <T> void produceMessageToBroadcastQueue(final String correlationId, final T message) {
		checkNotNull(correlationId);
		checkNotNull(message);

		final byte[] payload = serializer.serialize(message).elements();

		// Ensure the DB is up-to-date before executing non-transactional task (i.e. sending the message)
		entityManager.flush();
		// Ensure that no unintentional changes to already flushed entities will be persisted later.
		entityManager.clear();

		// As this call is not transactional, it must be the last operation done in the current transaction.
		multicastJmsTemplate.convertAndSend(BROADCAST_TOPIC_EXCHANGE, payload, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(HEADER_MESSAGE_TYPE, message.getClass().getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});

		LOGGER.info("Response has been broadcasted to other instances. [correlationId: {}]", correlationId);
	}

	@JmsListener(destination = BROADCAST_TOPIC_EXCHANGE, containerFactory = "multicastConnectionFactory")
	private <T> void consumeMessageFromBroadcastQueue(final Message message) throws JMSException {
		try {
			checkNotNull(message);
			contextHolder.setTenantId(checkNotNull(message.getStringProperty(MESSAGE_HEADER_TENANT_ID)));
			final String correlationId = checkNotNull(message.getJMSCorrelationID());
			final String messageType = checkNotNull(message.getStringProperty(HEADER_MESSAGE_TYPE));

			@SuppressWarnings("unchecked")
			final CachedElement<T> cachedElement = (CachedElement<T>) cache.getIfPresent(correlationId);

			if (cachedElement != null) {
				if (!messageType.contains("Exception")) {
					final T o;
					final ImmutableByteArray messageBytes = new ImmutableByteArray(message.getBody(byte[].class));
					if (cachedElement.clazz != null) {
						o = serializer.deserialize(messageBytes, cachedElement.clazz);
					} else {
						o = serializer.deserialize(messageBytes, cachedElement.typeReference);
					}
					cachedElement.completableFuture().complete(o);
					cache.invalidate(correlationId);
					LOGGER.info("Response has been handled after been received from broadcast message. [correlationId: {}]", correlationId);
				} else {
					final ImmutableByteArray messageBytes = new ImmutableByteArray(message.getBody(byte[].class));
					final RuntimeException o = (RuntimeException) serializer.deserialize(messageBytes, getExceptionType(messageType));

					cachedElement.completableFuture().completeExceptionally(o);
					cache.invalidate(correlationId);
					LOGGER.info("Response (exceptionally) has been handled after been received from broadcast message. [correlationId: {}]",
							correlationId);
				}
			} else {
				LOGGER.info("Response has been received from broadcast message but no one is registered for in this instance. [correlationId: {}]",
						correlationId);
			}
		} finally {
			contextHolder.clear();
		}
	}

	private Class<?> getExceptionType(final String messageType) {
		try {
			final Class<?> aClass = Class.forName(messageType);
			if (RuntimeException.class.isAssignableFrom(aClass)) {
				return aClass;
			} else {
				throw new IllegalArgumentException("Exception is not a descendant of RuntimeException");
			}
		} catch (final ClassNotFoundException e) {
			throw new IllegalArgumentException("Class not found.", e);
		}
	}

	private record CachedElement<T>(ResponseCompletionCompletableFuture<T> completableFuture, Class<T> clazz, TypeReference<T> typeReference) {
		public CachedElement {
			checkNotNull(completableFuture);
			checkState(clazz != null ^ typeReference != null,
					"Either clazz or typeReference (xor) should by specified to deserialize the T element.");
		}
	}

	@SuppressWarnings("Java:S2094")
	public record Empty() {
	}
}
