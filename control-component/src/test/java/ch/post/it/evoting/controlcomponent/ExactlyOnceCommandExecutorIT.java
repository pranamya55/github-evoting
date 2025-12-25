/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

import static ch.post.it.evoting.domain.multitenancy.TenantConstants.TEST_TENANT_ID;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.ID_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import ch.post.it.evoting.controlcomponent.commandmessaging.CommandId;
import ch.post.it.evoting.controlcomponent.commandmessaging.CommandService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base32Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;

@SpringBootTest
@ContextConfiguration(initializers = TestKeyStoreInitializer.class)
@ActiveProfiles("test")
@DisplayName("ExactlyOnceCommandExecutor calling")
class ExactlyOnceCommandExecutorIT {

	private static final String BAD_INPUT_ID = "Bad input";
	private static final Hash hash = HashFactory.createHash();
	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base32Alphabet = Base32Alphabet.getInstance();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final Runnable PRE_VALIDATION_NO_OP_TASK = () -> {};
	private static final Callable<ImmutableByteArray> THROW_IF_CALLED_TASK = () -> {
		throw new NullPointerException("This should not be thrown.");
	};
	private static final int TRANSACTION_TIMEOUT = 60;
	@Value("${nodeID}")
	private int nodeId;
	@MockitoSpyBean
	private CommandService commandService;
	@MockitoSpyBean
	private ObjectMapper objectMapper;
	@MockitoSpyBean
	private ExactlyOnceCommandExecutor processor;
	@Autowired
	private ContextHolder contextHolder;
	private String correlationId;
	private String contextId;
	private String context;

	@BeforeEach
	void setup() {
		correlationId = random.genRandomString(ID_LENGTH, base64Alphabet);

		contextId = random.genRandomString(ID_LENGTH, base32Alphabet);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		context = uuidGenerator.generate();

		contextHolder.setTenantId(TEST_TENANT_ID);
	}

	@Test
	@DisplayName("processExactlyOnce with null argument does not save")
	void testProcessExactlyOnceWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> processor.process(null));
	}

	@Test
	@DisplayName("processExactlyOnce with process throwing an exception does roll back correctly")
	void testProcessExactlyOnceWithProcessThrowingExceptionRollsBackCorrectly() {

		final CommandId commandId = CommandId.builder()
				.contextId(contextId)
				.context(context)
				.correlationId(correlationId)
				.nodeId(nodeId)
				.build();

		assertFalse(commandService.findIdenticalCommand(commandId).isPresent());

		final TestPayload payload = new TestPayload(BAD_INPUT_ID);
		final ImmutableByteArray testPayloadHash = hash.recursiveHash(payload);
		final Callable<ImmutableByteArray> callable = () -> getTestPayloadBytes(payload);
		final ExactlyOnceCommand<ImmutableByteArray> processingInput = new ExactlyOnceCommand.Builder<ImmutableByteArray>()
				.setCorrelationId(correlationId)
				.setContextId(contextId)
				.setContext(context)
				.setPreValidationTask(PRE_VALIDATION_NO_OP_TASK)
				.setTask(callable)
				.setReplayTask(THROW_IF_CALLED_TASK)
				.setRequestPayloadHash(testPayloadHash)
				.setSerializer(Function.identity())
				.setTransactionTimeout(TRANSACTION_TIMEOUT)
				.build();

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> processor.process(processingInput));
		assertEquals("Failed to execute exactly once command.", exception.getMessage());
		assertEquals(BAD_INPUT_ID, Throwables.getRootCause(exception).getMessage());

		assertFalse(commandService.findIdenticalCommand(commandId).isPresent());
	}

	@Test
	@DisplayName("processExactlyOnce with processing function returning a payload saves request and response")
	void testProcessExactlyOnceWithCallableReturningPayloadSavesResponseCorrectly() throws IOException {
		final TestPayload testPayload = new TestPayload("PayloadToBeSaved1");
		final ImmutableByteArray testPayloadHash = hash.recursiveHash(testPayload);
		final Callable<ImmutableByteArray> callable = () -> getTestPayloadBytes(testPayload);
		final ExactlyOnceCommand<ImmutableByteArray> processingInput = new ExactlyOnceCommand.Builder<ImmutableByteArray>()
				.setCorrelationId(correlationId)
				.setContextId(contextId)
				.setContext(context)
				.setPreValidationTask(PRE_VALIDATION_NO_OP_TASK)
				.setTask(callable)
				.setReplayTask(THROW_IF_CALLED_TASK)
				.setRequestPayloadHash(testPayloadHash)
				.setSerializer(Function.identity())
				.setTransactionTimeout(TRANSACTION_TIMEOUT)
				.build();

		final CommandId commandId = CommandId.builder()
				.contextId(contextId)
				.context(context)
				.correlationId(correlationId)
				.nodeId(nodeId)
				.build();

		assertFalse(commandService.findIdenticalCommand(commandId).isPresent());

		final ImmutableByteArray payloadBytes = assertDoesNotThrow(() -> processor.process(processingInput));

		assertTrue(commandService.findIdenticalCommand(commandId).isPresent());
		assertEquals(objectMapper.writeValueAsString(testPayload), new String(payloadBytes.elements()));
	}

	@Test
	@DisplayName("processExactlyOnce twice with exactly the same message, saves request and response only once")
	void testProcessExactlyOnceWithMessageAlreadySavedDoesNotCallAgain() throws JsonProcessingException {
		final TestPayload testPayload = new TestPayload("PayloadToBeSaved2");
		final ImmutableByteArray testPayloadHash = hash.recursiveHash(testPayload);
		final Callable<ImmutableByteArray> callable = () -> getTestPayloadBytes(testPayload);

		final CommandId commandId = CommandId.builder()
				.contextId(contextId)
				.context(context)
				.correlationId(correlationId)
				.nodeId(nodeId)
				.build();

		assertFalse(commandService.findIdenticalCommand(commandId).isPresent());

		// 1. Call
		final ExactlyOnceCommand<ImmutableByteArray> task = new ExactlyOnceCommand.Builder<ImmutableByteArray>()
				.setCorrelationId(correlationId)
				.setContextId(contextId)
				.setContext(context)
				.setPreValidationTask(PRE_VALIDATION_NO_OP_TASK)
				.setTask(callable)
				.setReplayTask(THROW_IF_CALLED_TASK)
				.setRequestPayloadHash(testPayloadHash)
				.setSerializer(Function.identity())
				.setTransactionTimeout(TRANSACTION_TIMEOUT)
				.build();
		final ImmutableByteArray responseBytes1 = assertDoesNotThrow(() -> processor.process(task));

		verify(commandService, times(1)).save(any(), any(), any(), any(), any(), any());
		assertTrue(commandService.findIdenticalCommand(commandId).isPresent());
		assertEquals(objectMapper.writeValueAsString(testPayload), new String(responseBytes1.elements()));

		// 2.call
		final ExactlyOnceCommand<ImmutableByteArray> throwingTask = new ExactlyOnceCommand.Builder<ImmutableByteArray>()
				.setCorrelationId(correlationId)
				.setContextId(contextId)
				.setContext(context)
				.setPreValidationTask(PRE_VALIDATION_NO_OP_TASK)
				.setTask(THROW_IF_CALLED_TASK)
				.setReplayTask(() -> responseBytes1)
				.setRequestPayloadHash(testPayloadHash)
				.setSerializer(Function.identity())
				.setTransactionTimeout(TRANSACTION_TIMEOUT)
				.build();

		final ImmutableByteArray responseBytes2 = assertDoesNotThrow(() -> processor.process(throwingTask));

		assertTrue(commandService.findIdenticalCommand(commandId).isPresent());
		assertEquals(objectMapper.writeValueAsString(testPayload), new String(responseBytes2.elements()));

	}

	@Test
	@DisplayName("processExactlyOnce twice with the same message but different correlation id throws for one")
	void twiceWithSameMessageButDifferentCorrelationIds() {
		final TestPayload testPayload = new TestPayload("PayloadToBeSavedDifferentCorrelationId");
		final ImmutableByteArray testPayloadHash = hash.recursiveHash(testPayload);

		final CommandId commandId = CommandId.builder()
				.contextId(contextId)
				.context(context)
				.correlationId(correlationId)
				.nodeId(nodeId)
				.build();

		final String differentCorrelationId = random.genRandomString(ID_LENGTH, base64Alphabet);
		final CommandId differentCommandId = CommandId.builder()
				.contextId(contextId)
				.context(context)
				.correlationId(differentCorrelationId)
				.nodeId(nodeId)
				.build();

		assertFalse(commandService.findIdenticalCommand(commandId).isPresent());
		assertFalse(commandService.findIdenticalCommand(differentCommandId).isPresent());

		// Use a latch to synchronize threads, ensuring both are concurrently trying to save their command.
		final CountDownLatch countDownLatch = new CountDownLatch(2);

		// 1. Call
		final Callable<ImmutableByteArray> firstTask = () -> {
			countDownLatch.countDown();
			countDownLatch.await();
			return getTestPayloadBytes(testPayload);
		};
		final ExactlyOnceCommand<ImmutableByteArray> firstExactlyOnceCommand = new ExactlyOnceCommand.Builder<ImmutableByteArray>()
				.setCorrelationId(correlationId)
				.setContextId(contextId)
				.setContext(context)
				.setPreValidationTask(PRE_VALIDATION_NO_OP_TASK)
				.setTask(firstTask)
				.setReplayTask(THROW_IF_CALLED_TASK)
				.setRequestPayloadHash(testPayloadHash)
				.setSerializer(Function.identity())
				.setTransactionTimeout(TRANSACTION_TIMEOUT)
				.build();
		final Runnable firstProcess = () -> {
			contextHolder.setTenantId(TEST_TENANT_ID);
			processor.process(firstExactlyOnceCommand);
		};

		// 2. Call
		final Callable<ImmutableByteArray> secondTask = () -> {
			countDownLatch.countDown();
			countDownLatch.await();
			return getTestPayloadBytes(testPayload);
		};
		final ExactlyOnceCommand<ImmutableByteArray> secondExactlyOnceCommand = new ExactlyOnceCommand.Builder<ImmutableByteArray>()
				.setCorrelationId(differentCorrelationId)
				.setContextId(contextId)
				.setContext(context)
				.setPreValidationTask(PRE_VALIDATION_NO_OP_TASK)
				.setTask(secondTask)
				.setReplayTask(THROW_IF_CALLED_TASK)
				.setRequestPayloadHash(testPayloadHash)
				.setSerializer(Function.identity())
				.setTransactionTimeout(TRANSACTION_TIMEOUT)
				.build();
		final Runnable secondProcess = () -> {
			contextHolder.setTenantId(TEST_TENANT_ID);
			processor.process(secondExactlyOnceCommand);
		};

		// Execute the two process asynchronously.
		final ExecutorService executorService = Executors.newFixedThreadPool(2);
		final CompletableFuture<Void> firstCompletableFuture = CompletableFuture.runAsync(firstProcess, executorService);
		final CompletableFuture<Void> secondCompletableFuture = CompletableFuture.runAsync(secondProcess, executorService);

		// Block until both are completed. One should fail and complete exceptionally.
		final CompletableFuture<Void> allCompletableFuture = CompletableFuture.allOf(firstCompletableFuture, secondCompletableFuture);
		final CompletionException completionException = assertThrows(CompletionException.class, allCompletableFuture::join);

		// Assert the constraint violation happens because they have same (CONTEXT_ID, CONTEXT, NODE_ID).
		assertInstanceOf(DataIntegrityViolationException.class, completionException.getCause());
		assertTrue(Throwables.getRootCause(completionException).getMessage()
				.contains("ERROR: duplicate key value violates unique constraint \"command_uk\""));

		// Verify save was indeed called for both commands.
		verify(commandService).save(eq(commandId), any(), any(), any(), any(), any());
		verify(commandService).save(eq(differentCommandId), any(), any(), any(), any(), any());

		// Exactly one command must be present.
		final boolean commandPresent = commandService.findIdenticalCommand(commandId).isPresent();
		final boolean differentCommandPresent = commandService.findIdenticalCommand(differentCommandId).isPresent();

		assertTrue(commandPresent || differentCommandPresent);
		assertFalse(commandPresent && differentCommandPresent);
	}

	@Test
	@DisplayName("processExactlyOnce with same message twice but with different message content throws an exception")
	void testProcessExactlyOnceWithMessageAlreadySavedButContentDifferentThrows() {
		final TestPayload payload = new TestPayload("PayloadToBeSaved3");
		final ImmutableByteArray testPayloadHash = hash.recursiveHash(payload);
		final Callable<ImmutableByteArray> callable = () -> getTestPayloadBytes(payload);
		final ExactlyOnceCommand<ImmutableByteArray> processingInput = new ExactlyOnceCommand.Builder<ImmutableByteArray>()
				.setCorrelationId(correlationId)
				.setContextId(contextId)
				.setContext(context)
				.setPreValidationTask(PRE_VALIDATION_NO_OP_TASK)
				.setTask(callable)
				.setReplayTask(THROW_IF_CALLED_TASK)
				.setRequestPayloadHash(testPayloadHash)
				.setSerializer(Function.identity())
				.setTransactionTimeout(TRANSACTION_TIMEOUT)
				.build();

		final CommandId commandId = CommandId.builder()
				.contextId(contextId)
				.context(context)
				.correlationId(correlationId)
				.nodeId(nodeId)
				.build();

		assertFalse(commandService.findIdenticalCommand(commandId).isPresent());

		// 1. Call
		assertDoesNotThrow(() -> processor.process(processingInput));
		assertTrue(commandService.findIdenticalCommand(commandId).isPresent());

		// 2.call
		final ImmutableByteArray differentMessageBytes = ImmutableByteArray.of((byte) 0b0000101);
		final Callable<ImmutableByteArray> differentCallable = () -> differentMessageBytes;
		final ExactlyOnceCommand<ImmutableByteArray> differentProcessingInput = new ExactlyOnceCommand.Builder<ImmutableByteArray>()
				.setCorrelationId(correlationId)
				.setContextId(contextId)
				.setContext(context)
				.setPreValidationTask(PRE_VALIDATION_NO_OP_TASK)
				.setTask(differentCallable)
				.setReplayTask(THROW_IF_CALLED_TASK)
				.setRequestPayloadHash(differentMessageBytes)
				.setSerializer(Function.identity())
				.setTransactionTimeout(TRANSACTION_TIMEOUT)
				.build();

		final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> processor.process(differentProcessingInput));
		final String expectedErrorMessage = String.format(
				"Similar request previously treated but for different request payload. [correlationId: %s, contextId: %s, context: %s, nodeId: %s]",
				correlationId, contextId, context, nodeId);
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception).getMessage());
		assertTrue(commandService.findIdenticalCommand(commandId).isPresent());

	}

	private ImmutableByteArray getTestPayloadBytes(final TestPayload testPayload) throws JsonProcessingException {
		if (testPayload.id().equals(BAD_INPUT_ID)) {
			throw new IllegalStateException(BAD_INPUT_ID);
		}
		return new ImmutableByteArray(objectMapper.writeValueAsBytes(testPayload));
	}

	record TestPayload(@JsonProperty String id) implements HashableList {

		@JsonCreator
		TestPayload(
				@JsonProperty("id")
				final String id) {
			this.id = id;
		}

		@Override
		public String id() {
			return this.id;
		}

		@Override
		public ImmutableList<Hashable> toHashableForm() {
			return ImmutableList.of(HashableString.from(id));
		}
	}
}
