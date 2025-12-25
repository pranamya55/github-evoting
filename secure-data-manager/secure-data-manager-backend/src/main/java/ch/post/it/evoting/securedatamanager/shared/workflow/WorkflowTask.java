/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.workflow;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Consumer;

/**
 * Represents a task to be executed by the {@link WorkflowStepRunner}. The task is composed of the actual task to be run, an action to be executed in
 * case of success ({@code successAction}) and an action to be executed in case of failure ({@code failureAction}).
 *
 * @param runnableTask  the actual work to be performed.
 * @param successAction the action to be executed in case of success.
 * @param failureAction the action to be executed in case of failure.
 */
public record WorkflowTask(Runnable runnableTask, Runnable successAction, Consumer<Throwable> failureAction) {

	/**
	 * Constructs a new workflow task.
	 *
	 * @param runnableTask  the actual work to be performed. Must be non-null.
	 * @param successAction the action to be executed in case of success. Must be non-null.
	 * @param failureAction the action to be executed in case of failure. Must be non-null.
	 * @throws NullPointerException if any of the parameters is null.
	 */
	public WorkflowTask {
		checkNotNull(runnableTask);
		checkNotNull(successAction);
		checkNotNull(failureAction);
	}

}
