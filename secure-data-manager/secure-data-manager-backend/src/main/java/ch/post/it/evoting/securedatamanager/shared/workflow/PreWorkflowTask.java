/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.workflow;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.function.Supplier;

/**
 * Represents a pre-task to be executed by the {@link WorkflowStepRunner}. The pre-task's goal is to perform preparation work before starting to
 * execute the {@link WorkflowTask}(s). The pre-task optionally returns a result of type {@code T}, which is typically used by the subsequent tasks.
 *
 * @param <T> The type of the optional result of the pre-task.
 */
public final class PreWorkflowTask<T> {

	private final Supplier<T> preTask;

	private T result;

	/**
	 * Constructs a pre-preTask with the given supplier.
	 *
	 * @param preTask the preTask to execute.
	 * @throws NullPointerException if the preTask is null.
	 */
	public PreWorkflowTask(final Supplier<T> preTask) {
		checkNotNull(preTask);
		this.preTask = preTask;
	}

	/**
	 * Executes the pre-preTask and stores the result.
	 */
	public void run() {
		result = preTask.get();
	}

	/**
	 * Gets the result of the pre-preTask. Can be {@code null} if the pre-preTask does not return a result. Note that this method should only be
	 * called after the #run method has been executed.
	 *
	 * @return the result of the pre-preTask.
	 * @throws IllegalStateException if the pre-preTask has not been executed yet or is not configured to have a result.
	 */
	public T get() {
		checkState(result != null, "Pre-preTask has not been executed yet or is not configured to have a result.");
		return result;
	}

}
