/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.transaction.TransactionDefinition.ISOLATION_SERIALIZABLE;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

import java.security.SecureRandom;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.base.Throwables;

import ch.post.it.evoting.controlcomponent.commandmessaging.CommandId;
import ch.post.it.evoting.controlcomponent.commandmessaging.CommandService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestKeyStoreInitializer.class)
class CommandMessagingIT {

	private static final SecureRandom secureRandom = new SecureRandom();

	@Autowired
	private CommandService commandService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	void saveConcurrentlySameContextId() {
		final CommandId commandId1 = CommandId.builder()
				.contextId("context-id-concurrent-1")
				.context("context-concurrent-1")
				.correlationId("correlation-id-1")
				.nodeId(1)
				.build();
		final CommandId commandId2 = CommandId.builder()
				.contextId("context-id-concurrent-1")
				.context("context-concurrent-1")
				.correlationId("correlation-id-2")
				.nodeId(1)
				.build();

		final byte[] requestPayloadHashBytes = new byte[10];
		secureRandom.nextBytes(requestPayloadHashBytes);
		final Instant requestTimestamp = Instant.now();
		final byte[] responsePayloadHashBytes = new byte[10];
		secureRandom.nextBytes(responsePayloadHashBytes);
		final byte[] responsePayloadBytes = new byte[10];
		secureRandom.nextBytes(responsePayloadBytes);

		final ImmutableByteArray requestPayloadHash = new ImmutableByteArray(requestPayloadHashBytes);
		final ImmutableByteArray responsePayloadHash = new ImmutableByteArray(responsePayloadHashBytes);
		final ImmutableByteArray responsePayload = new ImmutableByteArray(responsePayloadBytes);

		final Instant responseTimestamp = Instant.now();

		final TransactionTemplate outerTransactionTemplate = new TransactionTemplate(transactionManager);
		outerTransactionTemplate.setIsolationLevel(ISOLATION_SERIALIZABLE);

		final TransactionTemplate innerTransactionTemplate = new TransactionTemplate(transactionManager);
		innerTransactionTemplate.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
		innerTransactionTemplate.setIsolationLevel(ISOLATION_SERIALIZABLE);

		final DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
				() -> outerTransactionTemplate.executeWithoutResult(outerStatus -> {
					commandService.save(commandId1, requestPayloadHash, requestTimestamp, responsePayloadHash, responsePayload, responseTimestamp);

					innerTransactionTemplate.executeWithoutResult(innerStatus ->
							commandService.save(commandId2, requestPayloadHash, requestTimestamp, responsePayloadHash, responsePayload,
									responseTimestamp));
				}));

		assertTrue(Throwables.getRootCause(exception).getMessage().contains("ERROR: duplicate key value violates unique constraint \"command_uk\""));
	}

}
