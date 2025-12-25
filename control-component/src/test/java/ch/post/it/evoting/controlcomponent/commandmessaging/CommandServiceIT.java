/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.commandmessaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestKeyStoreInitializer.class)
class CommandServiceIT {

	private static final String CONTEXT_KEY_GENERATION = "key-generation";
	private static final String CONTEXT_ID_ONE = "context-id-1";
	private static final String CORRELATION_ID_ONE = "1";
	private static final int NODE_ID_ONE = 1;
	private static final int NODE_ID_2 = 2;

	@Autowired
	private CommandService commandService;

	@BeforeAll
	static void bootstrap(
			@Autowired
			final CommandService commandService) {

		final CommandId commandIdOneNodeOne = CommandId.builder()
				.contextId(CONTEXT_ID_ONE)
				.context(CONTEXT_KEY_GENERATION)
				.correlationId(CORRELATION_ID_ONE)
				.nodeId(NODE_ID_ONE)
				.build();
		final CommandId commandIdOneNodeTwo = CommandId.builder()
				.contextId(CONTEXT_ID_ONE)
				.context(CONTEXT_KEY_GENERATION)
				.correlationId(CORRELATION_ID_ONE)
				.nodeId(NODE_ID_2)
				.build();

		final SecureRandom secureRandom = new SecureRandom();
		final byte[] requestPayloadHashBytes = new byte[10];
		secureRandom.nextBytes(requestPayloadHashBytes);
		final byte[] responsePayloadHashBytes = new byte[10];
		secureRandom.nextBytes(responsePayloadHashBytes);
		final byte[] responsePayloadBytes = new byte[10];
		secureRandom.nextBytes(responsePayloadBytes);

		final ImmutableByteArray requestPayloadHash = new ImmutableByteArray(requestPayloadHashBytes);
		final ImmutableByteArray responsePayloadHash = new ImmutableByteArray(responsePayloadHashBytes);
		final ImmutableByteArray responsePayload = new ImmutableByteArray(responsePayloadBytes);

		commandService.save(commandIdOneNodeOne, requestPayloadHash, Instant.now(), responsePayloadHash, responsePayload, Instant.now());
		commandService.save(commandIdOneNodeTwo, requestPayloadHash, Instant.now(), responsePayloadHash, responsePayload, Instant.now());
	}

	@Test
	void findIdenticalCommand() {
		final CommandId commandIdOneNodeOne = CommandId.builder()
				.contextId(CONTEXT_ID_ONE)
				.context(CONTEXT_KEY_GENERATION)
				.correlationId(CORRELATION_ID_ONE)
				.nodeId(NODE_ID_ONE)
				.build();

		final Optional<CommandEntity> identicalCommand = commandService.findIdenticalCommand(commandIdOneNodeOne);
		assertTrue(identicalCommand.isPresent());
	}

	@Test
	void findSemanticallyIdenticalCommand() {
		final CommandId commandIdOneNodeTwo = CommandId.builder()
				.contextId(CONTEXT_ID_ONE)
				.context(CONTEXT_KEY_GENERATION)
				.correlationId("2")
				.nodeId(NODE_ID_ONE)
				.build();

		final Optional<CommandEntity> identicalCommand = commandService.findIdenticalCommand(commandIdOneNodeTwo);
		assertFalse(identicalCommand.isPresent());

		final ImmutableList<CommandEntity> semanticallyIdenticalCommandEntity = commandService.findSemanticallyIdenticalCommand(commandIdOneNodeTwo);
		assertEquals(1, semanticallyIdenticalCommandEntity.size());
	}

	@Test
	void failToFindCommand() {
		final CommandId commandIdTwoNodeTwo = CommandId.builder()
				.contextId("unique-id-2")
				.context(CONTEXT_KEY_GENERATION)
				.correlationId(CORRELATION_ID_ONE)
				.nodeId(NODE_ID_ONE)
				.build();

		final Optional<CommandEntity> identicalCommand = commandService.findIdenticalCommand(commandIdTwoNodeTwo);
		assertFalse(identicalCommand.isPresent());

		final ImmutableList<CommandEntity> semanticallyIdenticalCommandEntity = commandService.findSemanticallyIdenticalCommand(commandIdTwoNodeTwo);
		assertEquals(0, semanticallyIdenticalCommandEntity.size());
	}

	@Test
	void findAllMessagesWithCorrelationId() {
		final ImmutableList<CommandEntity> allMessagesWithCorrelationId = commandService.findAllCommandsWithCorrelationId(CORRELATION_ID_ONE);
		assertEquals(NODE_ID_2, allMessagesWithCorrelationId.size());
	}
}
