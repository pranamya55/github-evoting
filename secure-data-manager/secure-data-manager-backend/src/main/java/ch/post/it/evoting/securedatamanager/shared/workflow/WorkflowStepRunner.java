/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.workflow;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

@Service
public class WorkflowStepRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowStepRunner.class);

	private static final Runnable EMPTY_ACTION = () -> {
	};

	private final WorkflowService workflowService;
	private final ExecutorService fixedThreadExecutorService;
	private final ExecutorService singleThreadExecutorService;
	private final WorkflowExceptionHandler workflowExceptionHandler;

	public WorkflowStepRunner(
			final WorkflowService workflowService,
			final ExecutorService fixedThreadExecutorService,
			final ExecutorService singleThreadExecutorService,
			final WorkflowExceptionHandler workflowExceptionHandler) {
		this.workflowService = workflowService;
		this.fixedThreadExecutorService = fixedThreadExecutorService;
		this.singleThreadExecutorService = singleThreadExecutorService;
		this.workflowExceptionHandler = workflowExceptionHandler;
	}

	/**
	 * <p>Runs a {@code workflowStep} composed of a single {@code workflowTask}. Default actions are executed on completion of the task.</p>
	 *
	 * <p>See {@link #run(WorkflowStep, PreWorkflowTask, ImmutableList, Runnable, Runnable, Runnable, ExecutorService)}.
	 *
	 * @param workflowStep the workflow step to execute. Must be non-null.
	 * @param workflowTask the associated task to execute. Must be non-null.
	 */
	public void run(final WorkflowStep workflowStep, final WorkflowTask workflowTask) {
		checkNotNull(workflowStep);
		checkNotNull(workflowTask);

		run(workflowStep, null, ImmutableList.of(workflowTask), EMPTY_ACTION, EMPTY_ACTION, EMPTY_ACTION, fixedThreadExecutorService);
	}

	/**
	 * <p>Runs a {@code workflowStep} composed of a list of {@code workflowTasks}. The {@code workflowTasks} are run in parallel. Default actions are
	 * executed on completion of the tasks.</p>
	 *
	 * <p>See {@link #run(WorkflowStep, PreWorkflowTask, ImmutableList, Runnable, Runnable, Runnable, ExecutorService)}.
	 *
	 * @param workflowStep  the workflow step to execute. Must be non-null.
	 * @param workflowTasks the associated tasks list to execute. Must be non-null.
	 */
	public void run(final WorkflowStep workflowStep, final ImmutableList<WorkflowTask> workflowTasks) {
		checkNotNull(workflowStep);
		checkNotNull(workflowTasks);

		run(workflowStep, null, workflowTasks, EMPTY_ACTION, EMPTY_ACTION, EMPTY_ACTION, fixedThreadExecutorService);
	}

	/**
	 * <p>Runs a {@code workflowStep} composed of a {@code preWorkflowTask} and a list of {@code workflowTasks}. The pre-workflow task is run first
	 * and if it succeeds then the {@code workflowTasks} are run in parallel. Default actions are executed on completion of the tasks.</p>
	 *
	 * <p>See {@link #run(WorkflowStep, PreWorkflowTask, ImmutableList, Runnable, Runnable, Runnable, ExecutorService)}.
	 *
	 * @param workflowStep    the workflow step to execute. Must be non-null.
	 * @param preWorkflowTask a task to be executed before the workflow tasks.
	 * @param workflowTasks   the associated tasks to execute. Must be non-null.
	 */
	public void run(final WorkflowStep workflowStep, final PreWorkflowTask<?> preWorkflowTask, final ImmutableList<WorkflowTask> workflowTasks) {
		checkNotNull(workflowStep);
		checkNotNull(preWorkflowTask);
		checkNotNull(workflowTasks);

		run(workflowStep, preWorkflowTask, workflowTasks, EMPTY_ACTION, EMPTY_ACTION, EMPTY_ACTION, fixedThreadExecutorService);
	}

	/**
	 * <p>Runs a {@code workflowStep} composed of a list of {@code workflowTasks}. The tasks are run sequentially. Default actions are executed on
	 * completion of the tasks.</p>
	 *
	 * <p>See {@link #run(WorkflowStep, PreWorkflowTask, ImmutableList, Runnable, Runnable, Runnable, ExecutorService)}.
	 *
	 * @param workflowStep  the workflow step to execute. Must be non-null.
	 * @param workflowTasks the associated tasks list to execute. Must be non-null.
	 */
	public void runSequential(final WorkflowStep workflowStep, final ImmutableList<WorkflowTask> workflowTasks) {
		checkNotNull(workflowStep);
		checkNotNull(workflowTasks);

		run(workflowStep, null, workflowTasks, EMPTY_ACTION, EMPTY_ACTION, EMPTY_ACTION, singleThreadExecutorService);
	}

	/**
	 * <p>Runs a {@code workflowStep} composed of a single {@code workflowTask}. Default actions are executed in case of success, failure and
	 * completion.</p>
	 *
	 * <p>See {@link #run(WorkflowStep, PreWorkflowTask, ImmutableList, Runnable, Runnable, Runnable, ExecutorService)}.
	 *
	 * @param workflowStep the workflow step to execute. Must be non-null.
	 * @param workflowTask the associated task to execute. Must be non-null.
	 */
	public void run(final WorkflowStep workflowStep, final PreWorkflowTask<?> preWorkflowTask, final WorkflowTask workflowTask) {
		checkNotNull(workflowStep);
		checkNotNull(workflowTask);

		run(workflowStep, preWorkflowTask, ImmutableList.of(workflowTask), EMPTY_ACTION, EMPTY_ACTION, EMPTY_ACTION, fixedThreadExecutorService);
	}

	/**
	 * <p>Runs a {@code workflowStep} composed of a single {@code workflowTask}. Default actions are executed in case of success and failure, while
	 * {@code completeAction} is executed on completion.</p>
	 *
	 * <p>See {@link #run(WorkflowStep, PreWorkflowTask, ImmutableList, Runnable, Runnable, Runnable, ExecutorService)}.
	 *
	 * @param workflowStep   the workflow step to execute. Must be non-null.
	 * @param workflowTask   the associated task to execute. Must be non-null.
	 * @param completeAction the action to be executed on completion of the workflow step.
	 */
	public void run(final WorkflowStep workflowStep, final PreWorkflowTask<?> preWorkflowTask, final WorkflowTask workflowTask,
			final Runnable completeAction) {
		checkNotNull(workflowStep);
		checkNotNull(workflowTask);
		checkNotNull(completeAction);

		run(workflowStep, preWorkflowTask, ImmutableList.of(workflowTask), EMPTY_ACTION, EMPTY_ACTION, completeAction, fixedThreadExecutorService);
	}

	/**
	 * <p>Runs a {@code workflowStep} composed of a a list of {@code workflowTasks}. Default actions are executed in case of success and failure,
	 * while {@code completeAction} is executed on completion.</p>
	 *
	 * <p>See {@link #run(WorkflowStep, PreWorkflowTask, ImmutableList, Runnable, Runnable, Runnable, ExecutorService)}.
	 *
	 * @param workflowStep  the workflow step to execute. Must be non-null.
	 * @param workflowTasks the associated tasks to execute. Must be non-null.
	 */
	public void run(final WorkflowStep workflowStep, final ImmutableList<WorkflowTask> workflowTasks, final Runnable completeAction) {
		checkNotNull(workflowStep);
		checkNotNull(workflowTasks);

		run(workflowStep, null, workflowTasks, EMPTY_ACTION, EMPTY_ACTION, completeAction, fixedThreadExecutorService);
	}

	/**
	 * <p>Runs a {@code workflowStep} composed of a a list of {@code workflowTasks}. {@code successAction} is executed on successful completion of
	 * the tasks, {@code failureAction} on failed completion of the tasks and a default action on completion regardless of result.</p>
	 *
	 * <p>See {@link #run(WorkflowStep, PreWorkflowTask, ImmutableList, Runnable, Runnable, Runnable, ExecutorService)}.
	 *
	 * @param workflowStep  the workflow step to execute. Must be non-null.
	 * @param workflowTasks the associated tasks to execute. Must be non-null.
	 * @param successAction the action to be executed in case of success of all tasks. Must be non-null.
	 * @param failureAction the action to be executed in case after failure of any task. Must be non-null.
	 */
	public void run(final WorkflowStep workflowStep, final ImmutableList<WorkflowTask> workflowTasks, final Runnable successAction,
			final Runnable failureAction) {

		checkNotNull(workflowStep);
		checkNotNull(workflowTasks);

		run(workflowStep, null, workflowTasks, successAction, failureAction, EMPTY_ACTION, fixedThreadExecutorService);
	}

	/**
	 * <p>
	 * Runs the supplied {@code workflowTasks} as part of the {@code workflowStep}. The tasks may be executed in parallel or sequentially, depending
	 * on the given {@code executorService}. Once all tasks are completed, optional actions are executed depending on the outcome of the tasks. If
	 * provided, the {@code completeAction} is always executed first on completion of all tasks regardless of the result (sucess or failure).
	 * </p>
	 * <p>
	 * The provided {@code successAction}, {@code failureAction} and {@code completeAction} are supplementary to the default actions and do not
	 * replace them. The default actions are as follows:
	 * </p>
	 * <ul>
	 *     <li>success: notify the completion of the workflow step and log.</li>
	 *     <li>failure: notify the error of the workflow step, handle the exception via the {@link WorkflowExceptionHandler} and log.</li>
	 *     <li>complete: no action.</li>
	 * </ul>
	 *
	 * @param workflowStep    the workflow step to start progress and complete or error.
	 * @param workflowTasks   the tasks to be executed.
	 * @param successAction   the action to be executed in case of success of all tasks.
	 * @param failureAction   the action to be executed in case after failure of any task.
	 * @param completeAction  the action to be executed on completion of all tasks.
	 * @param executorService the executor service to run the tasks.
	 */
	private void run(final WorkflowStep workflowStep, final PreWorkflowTask<?> preWorkflowTask, final ImmutableList<WorkflowTask> workflowTasks,
			final Runnable successAction, final Runnable failureAction, final Runnable completeAction, final ExecutorService executorService) {

		LOGGER.debug("Starting the workflow step [{}]...", workflowStep.name());

		// Notify the workflow that the step is in progress.
		workflowService.notifyInProgress(workflowStep);

		// Prepare the pre-workflow future to be executed in the initiating thread. May do nothing if no task is provided.
		final CompletableFuture<?> preWorkflowFuture = createPreWorkflowFuture(workflowStep, preWorkflowTask, Runnable::run);

		// Once the pre-task is completed successfully, run the rest of the workflow tasks. If it fails, do nothing.
		preWorkflowFuture.thenRun(() -> {
			// Prepare and run the workflow tasks.
			final CompletableFuture<Void>[] workflowFutures = workflowTasks.stream()
					.map(workflowTask -> CompletableFuture.runAsync(workflowTask.runnableTask(), executorService)
							.whenComplete(((unused, throwable) -> {
								if (throwable == null) {
									workflowTask.successAction().run();
								} else {
									workflowTask.failureAction().accept(throwable);
								}
							})))
					.<CompletableFuture<Void>>toArray(CompletableFuture[]::new);

			// Once all workflow tasks are completed, execute the action appropriately.
			CompletableFuture.allOf(workflowFutures)
					.whenComplete(((unused, throwable) -> {
						completeAction.run();

						if (throwable == null) {
							successAction.run();
							workflowService.notifyComplete(workflowStep);
							LOGGER.info("The workflow step [{}] has been successfully processed.", workflowStep.name());
						} else {
							failureAction.run();
							final WorkflowExceptionCode exceptionCode = workflowExceptionHandler.handleException(workflowStep, throwable);
							workflowService.notifyError(workflowStep, exceptionCode);
							LOGGER.error("The workflow step [{}] has failed.", workflowStep.name(), throwable);
						}
					}));
		});
	}

	private CompletableFuture<?> createPreWorkflowFuture(final WorkflowStep workflowStep, final PreWorkflowTask<?> preWorkflowTask,
			final Executor executor) {

		final CompletableFuture<?> preWorkflowFuture;
		if (preWorkflowTask == null) {
			// No pre-workflow task to run, complete immediately.
			preWorkflowFuture = CompletableFuture.completedFuture(null);
		} else {
			// Prepare and run the pre-workflow task first.
			preWorkflowFuture = CompletableFuture.runAsync(preWorkflowTask::run, executor)
					.whenComplete(((unused, throwable) -> {
						if (throwable == null) {
							// Do not notify in case of success, because there are still the workflow tasks to run.
							LOGGER.info("Pre-workflow task executed successfully.");
						} else {
							final WorkflowExceptionCode exceptionCode = workflowExceptionHandler.handleException(workflowStep, throwable);
							workflowService.notifyError(workflowStep, exceptionCode);
							LOGGER.error("The workflow step [{}] has failed.", workflowStep.name(), throwable);
						}
					}));
		}

		return preWorkflowFuture;
	}

}
