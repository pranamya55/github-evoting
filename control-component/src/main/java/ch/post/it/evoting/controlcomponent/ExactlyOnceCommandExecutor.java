/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import ch.post.it.evoting.controlcomponent.commandmessaging.CommandEntity;
import ch.post.it.evoting.controlcomponent.commandmessaging.CommandId;
import ch.post.it.evoting.controlcomponent.commandmessaging.CommandService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;

/**
 * Class for processing requests exactly once.
 */
@Service
public class ExactlyOnceCommandExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExactlyOnceCommandExecutor.class);

	private final Hash hash;
	private final CommandService commandService;
	private final PlatformTransactionManager platformTransactionManager;

	@Value("${nodeID}")
	private int nodeId;

	public ExactlyOnceCommandExecutor(
			final Hash hash,
			final CommandService commandService,
			final PlatformTransactionManager platformTransactionManager) {
		this.hash = hash;
		this.commandService = commandService;
		this.platformTransactionManager = platformTransactionManager;
	}

	/**
	 * Processes a given task exactly once.
	 * <ul>
	 * <li>If the same request (same ids, same request content) is already stored in the database, the response to the request is taken from the database and returned.</li>
	 * <li>If the same request ids but different request content, an IllegalStateException is thrown.</li>
	 * <li>Otherwise the request is processed and the response is persisted and returned.</li>
	 * </ul>
	 *
	 * @param exactlyOnceCommand The exactlyOnceCommand to be processed.
	 * @return the result of the processing as a byte array.
	 * @throws NullPointerException  if the exactlyOnceCommand is null.
	 * @throws IllegalStateException if the request could not be processed correctly.
	 */
	// The transaction is handled manually in the code below and ensures all processing is atomically executed in its own transaction.
	public <T extends Hashable> ImmutableByteArray process(final ExactlyOnceCommand<T> exactlyOnceCommand) {
		checkNotNull(exactlyOnceCommand);
		final Instant requestTimestamp = Instant.now();
		final String correlationId = exactlyOnceCommand.getCorrelationId();
		final String contextId = exactlyOnceCommand.getContextId();
		final String context = exactlyOnceCommand.getContext();
		final Callable<T> task = exactlyOnceCommand.getTask();
		final Callable<T> replayTask = exactlyOnceCommand.getReplayTask();
		final Function<T, ImmutableByteArray> serializer = exactlyOnceCommand.getSerializer();
		final ImmutableByteArray requestPayloadHash = exactlyOnceCommand.getRequestPayloadHash();
		final Runnable preTask = exactlyOnceCommand.getPreTask();
		final int transactionTimeout = exactlyOnceCommand.getTransactionTimeout();

		final CommandId commandId = CommandId.builder()
				.contextId(contextId)
				.context(context)
				.correlationId(correlationId)
				.nodeId(nodeId)
				.build();
		final ImmutableList<CommandEntity> identicalCommandEntity = commandService.findSemanticallyIdenticalCommand(commandId);
		checkState(identicalCommandEntity.size() <= 1,
				"There was a problem with exactly once processing, multiple semantically identical commands exist.");

		if (identicalCommandEntity.size() == 1) {
			return handleIdenticalCommand(identicalCommandEntity, requestPayloadHash, correlationId, contextId, context, replayTask, serializer);
		} else {
			runPreTask(preTask);
			return runTask(task, requestPayloadHash, serializer, commandId, requestTimestamp, transactionTimeout);
		}
	}

	private <T extends Hashable> ImmutableByteArray handleIdenticalCommand(final ImmutableList<CommandEntity> identicalCommandEntity,
			final ImmutableByteArray requestPayloadHash, final String correlationId, final String contextId, final String context,
			final Callable<T> replayTask, final Function<T, ImmutableByteArray> serializer) {
		final ImmutableByteArray identicalCommandRequestPayloadHash = identicalCommandEntity.get(0).getRequestPayloadHash();
		// Check if the identical command request payload hash is the same as the new request payload hash.
		if (Objects.equals(requestPayloadHash, identicalCommandRequestPayloadHash)) {
			LOGGER.warn("Request already processed, returning previously calculated payload. [correlationId: {}, contextId: {}, context: {}]",
					correlationId, contextId, context);
			try {
				final T responsePayload = replayTask.call();
				final ImmutableByteArray responsePayloadHash = hash.recursiveHash(responsePayload);
				checkState(Objects.equals(identicalCommandEntity.get(0).getResponsePayloadHash(), responsePayloadHash),
						"The identical command response payload hash does not match the new command response payload hash.");
				return serializer.apply(responsePayload);
			} catch (final Exception e) {
				throw new IllegalStateException("Failed to handle identical command.", e);
			}
		} else {
			final String errorMessage = String.format(
					"Similar request previously treated but for different request payload. [correlationId: %s, contextId: %s, context: %s, nodeId: %s]",
					correlationId, contextId, context, nodeId);
			throw new IllegalStateException(errorMessage);
		}
	}

	private void runPreTask(final Runnable preTask) {
		try {
			preTask.run();
		} catch (final Exception e) {
			throw new IllegalStateException("Failed to execute pre-validation task", e);
		}
	}

	private <T extends Hashable> ImmutableByteArray runTask(final Callable<T> task, final ImmutableByteArray requestPayloadHash,
			final Function<T, ImmutableByteArray> serializer, final CommandId commandId, final Instant requestTimestamp,
			final int transactionTimeout) {
		final ImmutableByteArray serializedResponsePayload;

		final DefaultTransactionDefinition definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		definition.setTimeout(transactionTimeout);
		final TransactionStatus transaction = platformTransactionManager.getTransaction(definition);
		try {
			final T responsePayload = task.call();
			final ImmutableByteArray responsePayloadHash = hash.recursiveHash(responsePayload);
			serializedResponsePayload = serializer.apply(responsePayload);
			commandService.save(commandId, requestPayloadHash, requestTimestamp, responsePayloadHash, serializedResponsePayload, Instant.now());
		} catch (final Exception e) {
			platformTransactionManager.rollback(transaction);
			throw new IllegalStateException("Failed to execute exactly once command.", e);
		}
		platformTransactionManager.commit(transaction);
		return serializedResponsePayload;
	}
}
