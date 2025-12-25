/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver;

import static ch.post.it.evoting.domain.SharedQueue.CONTROL_COMPONENT_QUEUE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_MESSAGE_TYPE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_TENANT_ID;
import static ch.post.it.evoting.domain.SharedQueue.VOTING_SERVER_ADDRESS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.domain.multitenancy.TenantConstants;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;
import ch.post.it.evoting.votingserver.messaging.InProgressMessageRepository;
import ch.post.it.evoting.votingserver.messaging.InProgressMessageService;

/**
 * Helper class to test broadcasting to CCs.
 */
@Service
public class BroadcastIntegrationTestService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastIntegrationTestService.class);

	private final LinkedBlockingQueue<String> correlationQueue = new LinkedBlockingQueue<>(20);

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private InProgressMessageRepository inProgressMessageRepository;

	@Autowired
	private ContextHolder contextHolder;

	@Bean
	public InProgressMessageService inProgressMessageService() {
		final InProgressMessageService spy = spy(new InProgressMessageService(inProgressMessageRepository));
		doAnswer(invocation -> {
			final Object[] args = invocation.getArguments();
			final String correlationId = (String) args[0];
			final ImmutableSet<Integer> nodes = (ImmutableSet) args[1];

			invocation.callRealMethod();
			nodes.forEach(nodeId -> notifyRequestEvent(correlationId, nodeId));
			return null;
		}).when(spy).storeRequests(Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString());
		return spy;
	}

	private void notifyRequestEvent(final String correlationId, final int nodeId) {
		checkNotNull(correlationId);
		checkArgument(ControlComponentNode.ids().contains(nodeId));

		if (!correlationQueue.contains(correlationId)) {
			correlationQueue.offer(correlationId);
		}
	}

	/**
	 * Simulate CCs message consumer. First consumes the message on all control-components and then responds to the voting-server address with the
	 * {@code responsePayload}.
	 *
	 * @param responsePayload function of nodeId to response payload to create the response to be sent on the response queue.
	 */
	public void respondWith(final Function<Integer, Object> responsePayload) {
		ControlComponentNode.ids().forEach(nodeId -> respondWithForGivenNodeId(CONTROL_COMPONENT_QUEUE + nodeId, responsePayload, nodeId));
	}

	private void respondWithForGivenNodeId(final String requestQueue, final Function<Integer, Object> responsePayload, final Integer nodeId) {
		final Message requestMessage = jmsTemplate.receive(requestQueue);
		assert requestMessage != null;
		LOGGER.info("Received request [queue: {}, nodeId: {}]", requestQueue, nodeId);

		final Object payload = responsePayload.apply(nodeId);

		try {
			final byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);
			final String payloadClass = payload.getClass().getName();
			final String correlationId = requestMessage.getJMSCorrelationID();

			jmsTemplate.convertAndSend(VOTING_SERVER_ADDRESS, payloadBytes, jmsMessage -> {
				jmsMessage.setJMSCorrelationID(correlationId);
				jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, payloadClass);
				jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
				return jmsMessage;
			});

			LOGGER.info("Sent response [nodeId: {}, messageType : {}, correlationId : {}]", nodeId, payloadClass, correlationId);
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(e);
		} catch (final JMSException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Wait with timeout for broadcast request to have been saved
	 */
	public void awaitBroadcastRequestsSaved(final long timeout, final TimeUnit unit) throws InterruptedException {
		final String correlationId = correlationQueue.poll(120, TimeUnit.SECONDS);

		final CompletableFuture<Boolean> future = new CompletableFuture<>();
		try (final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {
			final ScheduledFuture<?> poller = scheduler.scheduleAtFixedRate(() -> {
				contextHolder.setTenantId(TenantConstants.TEST_TENANT_ID);
				final long count = StreamSupport.stream(inProgressMessageRepository.findAll().spliterator(), false)
						.filter(ipm -> ipm.getCorrelationId().equals(correlationId))
						.count();
				if (count == ControlComponentNode.ids().size()) {
					future.complete(true);
				}
			}, 0, 100, TimeUnit.MILLISECONDS);

			try {
				future.orTimeout(timeout, unit).join();
			} finally {
				poller.cancel(true);
				scheduler.shutdown();
			}
		}
	}
}
