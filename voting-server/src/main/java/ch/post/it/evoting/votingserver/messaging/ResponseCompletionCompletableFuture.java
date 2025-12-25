/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.messaging;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class ResponseCompletionCompletableFuture<T> {

	private final CompletableFuture<T> completableFuture;
	private final long defaultTimeout;

	public ResponseCompletionCompletableFuture(final long defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
		this.completableFuture = new CompletableFuture<>();
	}

	public <U> CompletableFuture<U> thenApply(final Function<? super T, ? extends U> fn) {
		return completableFuture.thenApply(fn);
	}

	/**
	 * Waits if necessary for at most {@link #defaultTimeout} seconds for this future to complete, and then returns its result, if available.
	 *
	 * @return the result value
	 * @throws IllegalStateException if the execution was cancelled
	 * @throws CompletionException   if this future doesn't complete during the timeout
	 */
	public T get() {
		return get(defaultTimeout, TimeUnit.SECONDS);
	}

	/**
	 * Waits if necessary for at most the given time for this future to complete, and then returns its result, if available.
	 *
	 * @param timeout the maximum time to wait
	 * @param unit    the time unit of the timeout argument
	 * @return the result value
	 * @throws IllegalStateException    if the execution was cancelled
	 * @throws CompletionException      if this future doesn't complete during the timeout
	 * @throws IllegalArgumentException if the timeout is negative
	 * @throws NullPointerException     if the unit is null
	 */
	public T get(final long timeout, final TimeUnit unit) {
		checkArgument(timeout >= 0, "the timeout must be greater or equal to zero. [timeout: %s]", timeout);
		checkNotNull(unit);
		try {
			return completableFuture.get(timeout, unit);
		} catch (final ExecutionException e) {
			if (e.getCause() instanceof final RuntimeException runtimeException) {
				throw runtimeException;
			} else {
				throw new IllegalStateException("This should not happen as the CompletableFuture does not execute any computation.");
			}
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CompletionException("The thread waiting for the CompletableFuture was interrupted.", e);
		} catch (final TimeoutException e) {
			throw new CompletionException(String.format("The CompletableFuture did not complete during the timeout period. [timeout: %s]",
					timeout), e);
		}
	}

	/**
	 * If not already completed, sets the value returned by {@link #get()} and related methods to the given value.
	 *
	 * @param value the result value
	 * @throws NullPointerException if the value is null.
	 */
	public void complete(final T value) {
		checkNotNull(value);
		completableFuture.complete(value);
	}

	public void completeExceptionally(final RuntimeException exception) {
		checkNotNull(exception);
		completableFuture.completeExceptionally(exception);

	}

	public boolean isRunning() {
		return Future.State.RUNNING.equals(completableFuture.state());
	}
}
