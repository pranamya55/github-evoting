/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.workflow;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.NONE;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode.valueOf;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStatus.COMPLETE;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStatus.ERROR;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStatus.IDLE;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStatus.IN_PROGRESS;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStatus.READY;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.DECRYPT_BALLOT_BOX;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.DOWNLOAD_BALLOT_BOX;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.MIX_BALLOT_BOX;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

public class WorkflowService {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowService.class);

	private final BallotBoxService ballotBoxService;
	private final WorkflowLogService workflowLogService;
	private final ElectionEventService electionEventService;
	private final ServerMode serverMode;

	private SseEmitter emitter;

	public WorkflowService(
			final BallotBoxService ballotBoxService,
			final WorkflowLogService workflowLogService,
			final ElectionEventService electionEventService,
			final ServerMode serverMode) {
		this.ballotBoxService = ballotBoxService;
		this.workflowLogService = workflowLogService;
		this.electionEventService = electionEventService;
		this.serverMode = serverMode;
	}

	public SseEmitter getEmitter() {
		if (emitter != null) {
			LOGGER.info("Return already existing emitter.");
			return emitter;
		}
		emitter = new SseEmitter(Long.MAX_VALUE);
		emitter.onCompletion(() -> LOGGER.info("Emitter complete"));
		emitter.onTimeout(() -> {
					LOGGER.info("Emitter timeout");
					emitter.complete();
				}
		);
		LOGGER.info("Created a new emitter.");
		return emitter;
	}

	/**
	 * Provides a state list for all the workflow steps of the current server mode.
	 *
	 * @return workflow state list.
	 */
	public ImmutableList<WorkflowState> getWorkflowStateList() {
		final ImmutableList<WorkflowLog> workflowLogs = workflowLogService.findLogs();

		// To get first step at READY state, initial value is true.
		final AtomicBoolean readiness = new AtomicBoolean(true);

		// Loop on the current server mode steps.
		return Arrays.stream(WorkflowStep.values())
				.filter(workflowStep -> serverMode.equals(workflowStep.getMode()))
				.flatMap(workflowStep -> {
					// Special cases for children states linked to ballotBoxIds
					if (MIX_BALLOT_BOX.equals(workflowStep)
							|| DOWNLOAD_BALLOT_BOX.equals(workflowStep)
							|| DECRYPT_BALLOT_BOX.equals(workflowStep)) {
						final String electionEventId = electionEventService.findElectionEventId();
						if (electionEventId != null) {
							return ballotBoxService.getBallotBoxes(electionEventId).stream()
									.map(ballotBox -> {
										readiness.set(getChildStepReadiness(workflowStep, ballotBox.id(), workflowLogs));
										return buildState(workflowStep, ballotBox.id(), workflowLogs, readiness);
									});
						} else {
							return Stream.of(buildState(workflowStep, "", workflowLogs, readiness));
						}
					} else {
						return Stream.of(buildState(workflowStep, "", workflowLogs, readiness));
					}
				}).collect(toImmutableList());
	}

	private WorkflowState buildState(final WorkflowStep workflowStep, final String contextId, final ImmutableList<WorkflowLog> workflowLogs,
			final AtomicBoolean readiness) {

		// Look up for start log
		final WorkflowLog startLog = workflowLogs.stream()
				.filter(workflowLog -> workflowLog.getWorkflowStep().equals(workflowStep.name()))
				.filter(workflowLog -> workflowLog.getContextId().equals(contextId))
				.filter(workflowLog -> IN_PROGRESS.name().equals(workflowLog.getStatus()))
				.max(Comparator.comparing(WorkflowLog::getTimestamp))
				.orElse(null);

		if (startLog == null) {
			// No start log for current step, decides if state is READY or IDLE based on readiness.
			if (readiness.get()) {
				// Optional steps should not interfere with the readiness of the next step.
				// In case of optional step we forward the readiness.
				readiness.set(workflowStep.isOptional());
				return new WorkflowState(workflowStep, null, null, READY, contextId, NONE, workflowStep.isOptional());
			} else {
				return new WorkflowState(workflowStep, null, null, IDLE, contextId, NONE, workflowStep.isOptional());
			}
		}

		// Start log exists, look up for end log
		final WorkflowLog endLog = workflowLogs.stream()
				.filter(workflowLog -> workflowLog.getWorkflowStep().equals(workflowStep.name()))
				.filter(workflowLog -> workflowLog.getContextId().equals(contextId))
				.filter(workflowLog -> COMPLETE.name().equals(workflowLog.getStatus())
						|| ERROR.name().equals(workflowLog.getStatus())
						|| (READY.name().equals(workflowLog.getStatus()) && workflowStep.isFractionable()))
				.max(Comparator.comparing(WorkflowLog::getTimestamp))
				.orElse(null);

		if (endLog != null && !endLog.getTimestamp().isBefore(startLog.getTimestamp())) {
			// End log exists and after start log (or at the same time), returns COMPLETE, ERROR or READY state.
			// READY state is when forcing parent to go IN_PROGRESS->READY when not all children are COMPLETE.

			// ERROR.
			if (ERROR.name().equals(endLog.getStatus())) {
				return new WorkflowState(workflowStep, startLog.getTimestamp(), endLog.getTimestamp(), ERROR, startLog.getContextId(),
						valueOf(endLog.getExceptionCode()), workflowStep.isOptional());
			}
			// Next step will be READY.
			if (COMPLETE.name().equals(endLog.getStatus())) {
				readiness.set(true);
			}
			// COMPLETE or READY.
			return new WorkflowState(workflowStep, startLog.getTimestamp(), endLog.getTimestamp(), WorkflowStatus.valueOf(endLog.getStatus()),
					startLog.getContextId(), NONE, workflowStep.isOptional());
		} else {
			// End log not exists, returns IN_PROGRESS state
			readiness.set(false);
			return new WorkflowState(workflowStep, startLog.getTimestamp(), null, IN_PROGRESS, startLog.getContextId(), NONE,
					workflowStep.isOptional());
		}
	}

	private void notify(final WorkflowStep step, final WorkflowStatus status, final String contextId, final WorkflowExceptionCode exceptionCode) {
		workflowLogService.saveLog(step, status, contextId, exceptionCode);

		if (ERROR.equals(status)) {
			LOGGER.error("Workflow failed. [step: {}, status: {}, contextId: {}, exceptionCode: {}] has failed.", step, status, contextId,
					exceptionCode.name());
		} else {
			LOGGER.info("Workflow notification. [step: {}, status: {}, contextId: {}].", step, status, contextId);
		}

		// Notify subscribers.
		final WorkflowState workflowState = this.getState(step, contextId);
		checkState(Objects.nonNull(workflowState), "Invalid workflow state. [step: %s, status: %s, contextId: %s]", step, status, contextId);
		checkState(workflowState.status().equals(status), "Invalid workflow status. [step: %s, status: %s, workflowState.status: %s, contextId: %s]",
				step, status, workflowState.status(), contextId);
		this.sendMessage(workflowState);
	}

	public void notifyReady(final WorkflowStep step) {
		notify(step, READY, "", NONE);
	}

	public void notifyInProgress(final WorkflowStep step, final String contextId) {
		notify(step, IN_PROGRESS, contextId, NONE);
	}

	public void notifyInProgress(final WorkflowStep step) {
		notifyInProgress(step, "");
	}

	public void notifyComplete(final WorkflowStep step, final String contextId) {
		notify(step, COMPLETE, contextId, NONE);
	}

	public void notifyComplete(final WorkflowStep step) {
		notifyComplete(step, "");
	}

	public void notifyError(final WorkflowStep step, final String contextId, final WorkflowExceptionCode exceptionCode) {
		notify(step, ERROR, contextId, exceptionCode);
	}

	public void notifyError(final WorkflowStep step, final WorkflowExceptionCode exceptionCode) {
		notifyError(step, "", exceptionCode);
	}

	public boolean isStepComplete(final WorkflowStep step) {
		return stepHasStatus(step, WorkflowStatus.COMPLETE);
	}

	public boolean isStepInProgress(final WorkflowStep step) {
		return stepHasStatus(step, IN_PROGRESS);
	}

	private boolean stepHasStatus(final WorkflowStep step, final WorkflowStatus workflowStatus) {
		final WorkflowState workflowState = getState(step, "");
		if (workflowState != null) {
			return workflowStatus.equals(workflowState.status());
		}
		return false;
	}

	private WorkflowState getState(final WorkflowStep step, final String contextId) {
		checkNotNull(step);

		return this.getWorkflowStateList().stream()
				.filter(workflowState -> workflowState.step().equals(step))
				.filter(workflowState -> workflowState.contextId().equals(contextId))
				.findFirst()
				.orElse(null);
	}

	private void sendMessage(final WorkflowState state) {
		if (emitter == null) {
			LOGGER.warn("No emitter available");
			return;
		}
		try {
			emitter.send(state);
			LOGGER.info("Message sent. [step: {}, status: {}, contextId: {}]", state.step(), state.status(), state.contextId());
		} catch (final IOException e) {
			emitter.complete();
		}
	}

	private boolean getChildStepReadiness(final WorkflowStep workflowStep, final String contextId, final ImmutableList<WorkflowLog> workflowLogs) {
		return switch (workflowStep) {
			case MIX_BALLOT_BOX -> isBallotBoxMixable(contextId);
			case DOWNLOAD_BALLOT_BOX -> hasCompleteLog(MIX_BALLOT_BOX, contextId, workflowLogs);
			case DECRYPT_BALLOT_BOX -> hasCompleteLog(DOWNLOAD_BALLOT_BOX, contextId, workflowLogs);
			default -> false;
		};
	}

	private boolean isBallotBoxMixable(final String ballotBoxId) {
		final String electionEventId = electionEventService.findElectionEventId();
		return ballotBoxService.getBallotBoxes(electionEventId).stream()
				.filter(bb -> bb.id().equals(ballotBoxId))
				.findFirst()
				.map(ballotBox -> {
					final boolean isTestBallotBox = ballotBox.test();
					final LocalDateTime ballotBoxFinishDate = ballotBox.finishTime();
					final int gracePeriod = ballotBox.gracePeriod();
					final boolean afterGracePeriod = LocalDateTimeUtils.now().isAfter(ballotBoxFinishDate.plusSeconds(gracePeriod));

					return isTestBallotBox || afterGracePeriod;
				})
				.orElse(false);
	}

	private boolean hasCompleteLog(final WorkflowStep workflowStep, final String contextId, final ImmutableList<WorkflowLog> workflowLogs) {
		return workflowLogs.stream()
				.filter(workflowLog -> workflowStep.name().equals(workflowLog.getWorkflowStep()))
				.filter(workflowLog -> COMPLETE.name().equals(workflowLog.getStatus()))
				.anyMatch(workflowLog -> workflowLog.getContextId().equals(contextId));
	}

}
