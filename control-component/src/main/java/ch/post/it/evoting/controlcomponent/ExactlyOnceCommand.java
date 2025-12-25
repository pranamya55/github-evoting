/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.Callable;
import java.util.function.Function;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;

public class ExactlyOnceCommand<U> {

	private final String correlationId;
	private final String contextId;
	private final String context;
	private final Runnable preTask;
	private final Callable<U> task;
	private final Callable<U> replayTask;
	private final Function<U, ImmutableByteArray> serializer;
	private final ImmutableByteArray requestPayloadHash;
	private final int transactionTimeout;

	private ExactlyOnceCommand(final String correlationId, final String contextId, final String context, final Runnable preTask,
			final Callable<U> task, final Callable<U> replayTask, final Function<U, ImmutableByteArray> serializer,
			final ImmutableByteArray requestPayloadHash,
			final int transactionTimeout) {
		this.correlationId = correlationId;
		this.contextId = contextId;
		this.context = context;
		this.preTask = preTask;
		this.task = task;
		this.replayTask = replayTask;
		this.serializer = serializer;
		this.requestPayloadHash = requestPayloadHash;
		this.transactionTimeout = transactionTimeout;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public String getContextId() {
		return contextId;
	}

	public String getContext() {
		return context;
	}

	public Callable<U> getTask() {
		return task;
	}

	public Callable<U> getReplayTask() {
		return replayTask;
	}

	public ImmutableByteArray getRequestPayloadHash() {
		return requestPayloadHash;
	}

	public Function<U, ImmutableByteArray> getSerializer() {
		return serializer;
	}

	public Runnable getPreTask() {
		return preTask;
	}

	public int getTransactionTimeout() {
		return transactionTimeout;
	}

	public static class Builder<U> {
		private String correlationId;
		private String contextId;
		private String context;
		private Runnable preValidationTask;
		private Callable<U> task;
		private Callable<U> replayTask;
		private Function<U, ImmutableByteArray> serializer;
		private ImmutableByteArray requestPayloadHash;

		private int transactionTimeout;

		/**
		 * Sets the correlation id which is identical for all subtask of a business operation execution.
		 * <p>
		 * The combination of correlation id, context id and context uniquely identify this task.
		 * </p>
		 *
		 * @param correlationId The correlation id to be used for building the ExactlyOnceCommand.
		 * @return this Builder with the correlationId set.
		 */
		public Builder<U> setCorrelationId(final String correlationId) {
			this.correlationId = correlationId;
			return this;
		}

		/**
		 * Sets the context id which uniquely identifies the resource being operated on.
		 * <p>
		 * The combination of correlation id, context id and context uniquely identify this task.
		 * </p>
		 *
		 * @param contextId The context id to be used for building the ExactlyOnceCommand.
		 * @return this Builder with the contextId set.
		 */
		public Builder<U> setContextId(final String contextId) {
			this.contextId = contextId;
			return this;
		}

		/**
		 * Sets the context which identifies the business operation being executed.
		 * <p>
		 * The combination of correlation id, context id and context uniquely identify this task.
		 * </p>
		 *
		 * @param context The context to be used for building the ExactlyOnceCommand.
		 * @return this Builder with the context set.
		 */
		public Builder<U> setContext(final String context) {
			this.context = context;
			return this;
		}

		/**
		 * Sets the preValidationTask that should be executed before the task to validation it's input.
		 * <p>
		 * The preValidationTask is executed before the task and before the creation of the transaction. If the preValidationTask fails, the task will
		 * not be executed.
		 * </p>
		 *
		 * @param preValidationTask The task to be executed in the ExactlyOnceCommand.
		 * @return the Builder with the task set.
		 */
		public Builder<U> setPreValidationTask(final Runnable preValidationTask) {
			this.preValidationTask = preValidationTask;
			return this;
		}

		/**
		 * Sets the task that is guaranteed to be successfully processed exactly once. If the task fails, it is guaranteed not to be persisted.
		 *
		 * @param task The task to be executed in the ExactlyOnceCommand.
		 * @return the Builder with the task set.
		 */
		public Builder<U> setTask(final Callable<U> task) {
			this.task = task;
			return this;
		}

		/**
		 * Sets the on-replay task, which is only called in case of replays.
		 *
		 * @param replayTask The task to be executed in the ExactlyOnceCommand in case of replays.
		 * @return the Builder with the task set.
		 */
		public Builder<U> setReplayTask(final Callable<U> replayTask) {
			this.replayTask = replayTask;
			return this;
		}

		/**
		 * Sets the request payload hash, which is the hash of the input to the process.
		 *
		 * @param requestPayloadHash The hash of the message to be processed in the ExactlyOnceCommand.
		 * @return the Builder with the request payload hash set.
		 */
		public Builder<U> setRequestPayloadHash(final ImmutableByteArray requestPayloadHash) {
			this.requestPayloadHash = requestPayloadHash;
			return this;
		}

		public Builder<U> setSerializer(final Function<U, ImmutableByteArray> serializer) {
			this.serializer = serializer;
			return this;
		}

		public Builder<U> setTransactionTimeout(final int transactionTimeout) {
			this.transactionTimeout = transactionTimeout;
			return this;
		}

		/**
		 * Instantiates an ExactlyOnceCommand with the fields set according to the Builder's fields.
		 *
		 * @return an ExactlyOnceCommand.
		 * @throws NullPointerException if any of the Builder's fields is null
		 */
		public ExactlyOnceCommand<U> build() {
			checkNotNull(correlationId);
			checkNotNull(contextId);
			checkNotNull(context);
			checkNotNull(preValidationTask);
			checkNotNull(task);
			checkNotNull(replayTask);
			checkNotNull(requestPayloadHash);
			checkNotNull(serializer);
			checkState(transactionTimeout >= -1, "Transaction Timeout must be a non-negative integer or TIMEOUT_DEFAULT");

			return new ExactlyOnceCommand<>(correlationId, contextId, context, preValidationTask, task, replayTask, serializer,
					requestPayloadHash, transactionTimeout);
		}
	}
}
