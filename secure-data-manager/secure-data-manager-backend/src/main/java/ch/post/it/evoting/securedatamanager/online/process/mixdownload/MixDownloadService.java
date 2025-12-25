/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.mixdownload;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStatus.COMPLETE;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.DOWNLOAD_BALLOT_BOX;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.MIX_BALLOT_BOX;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.MIX_DOWNLOAD;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.tally.BallotBoxStatus;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionHandler;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowService;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowState;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class MixDownloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixDownloadService.class);

	private final DownloadBallotBoxService downloadBallotBoxService;
	private final ExecutorService executorService;
	private final WorkflowService workflowService;
	private final BallotBoxService ballotBoxService;
	private final MixDecryptService mixDecryptService;
	private final WorkflowExceptionHandler workflowExceptionHandler;

	public MixDownloadService(
			final DownloadBallotBoxService downloadBallotBoxService,
			final WorkflowService workflowService,
			final BallotBoxService ballotBoxService,
			final MixDecryptService mixDecryptService,
			final ExecutorService fixedThreadExecutorService,
			final WorkflowExceptionHandler workflowExceptionHandler) {
		this.downloadBallotBoxService = downloadBallotBoxService;
		this.workflowService = workflowService;
		this.ballotBoxService = ballotBoxService;
		this.mixDecryptService = mixDecryptService;
		this.executorService = fixedThreadExecutorService;
		this.workflowExceptionHandler = workflowExceptionHandler;
	}

	public void mixAndDownload(final String electionEventId, final ImmutableList<String> ballotBoxIds) {
		validateUUID(electionEventId);
		checkNotNull(ballotBoxIds);

		workflowService.notifyInProgress(MIX_DOWNLOAD);
		final ImmutableList<WorkflowState> workflowStateList = workflowService.getWorkflowStateList();

		final ImmutableList<CompletableFuture<Void>> futures = ballotBoxIds.stream()
				.map(ballotBoxId -> {

					final Runnable mixDownloadTask = () -> {

						final boolean mixingNeeded = workflowStateList.stream()
								.anyMatch(workflowState -> workflowState.contextId().equals(ballotBoxId)
										&& workflowState.step().equals(MIX_BALLOT_BOX)
										&& !workflowState.status().equals(COMPLETE));

						if (mixingNeeded) {
							// Start mixing
							workflowService.notifyInProgress(MIX_BALLOT_BOX, ballotBoxId);
							mixDecryptService.mix(electionEventId, ballotBoxId);

							// Polling mixing status
							pollBallotBoxStatus(electionEventId, ballotBoxId);
							workflowService.notifyComplete(MIX_BALLOT_BOX, ballotBoxId);
						}

						// Download
						workflowService.notifyInProgress(DOWNLOAD_BALLOT_BOX, ballotBoxId);
						downloadBallotBoxService.download(electionEventId, ballotBoxId);
						workflowService.notifyComplete(DOWNLOAD_BALLOT_BOX, ballotBoxId);
					};

					return CompletableFuture.runAsync(mixDownloadTask, executorService)
							.whenComplete((unused, throwable) -> {
								if (throwable != null) {
									final BallotBoxStatus ballotBoxStatus = ballotBoxService.getBallotBoxStatus(ballotBoxId);
									// If the ballot box is in MIXED status, then the download step has failed.
									if (BallotBoxStatus.MIXED.equals(ballotBoxStatus)) {
										final WorkflowExceptionCode exceptionCode = workflowExceptionHandler.handleException(DOWNLOAD_BALLOT_BOX,
												throwable);
										workflowService.notifyError(DOWNLOAD_BALLOT_BOX, ballotBoxId, exceptionCode);
									}
									// Else, the mix step has failed.
									else {
										final WorkflowExceptionCode exceptionCode = workflowExceptionHandler.handleException(MIX_BALLOT_BOX,
												throwable);
										workflowService.notifyError(MIX_BALLOT_BOX, ballotBoxId, exceptionCode);
									}
								}
							});
				})
				.collect(toImmutableList());

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[] {}))
				.whenComplete((unused, throwable) -> {
					if (throwable != null) {
						final WorkflowExceptionCode exceptionCode = workflowExceptionHandler.handleException(MIX_DOWNLOAD, throwable);
						workflowService.notifyError(MIX_DOWNLOAD, exceptionCode);
						LOGGER.error("The workflow step [{}] has failed.", MIX_DOWNLOAD.name(), throwable);
					} else {
						// Check children states to determine if the workflow step is complete.
						final boolean allComplete = this.workflowService.getWorkflowStateList().stream()
								.filter(workflowState -> workflowState.step().equals(DOWNLOAD_BALLOT_BOX))
								.allMatch(workflowState -> COMPLETE.equals(workflowState.status()));

						if (allComplete) {
							this.workflowService.notifyComplete(MIX_DOWNLOAD);
						} else {
							this.workflowService.notifyReady(MIX_DOWNLOAD);
						}

						LOGGER.info("The workflow step [{}] has been successfully processed.", MIX_DOWNLOAD.name());
					}

				});
	}

	private void pollBallotBoxStatus(final String electionEventId, final String ballotBoxId) {

		BallotBoxStatus mixingStatus;
		try (final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {

			mixingStatus = BallotBoxStatus.MIXING;
			while (BallotBoxStatus.MIXING.equals(mixingStatus) || BallotBoxStatus.MIXING_NOT_STARTED.equals(mixingStatus)) {
				try {
					LOGGER.info("Wait before polling mix status, [ballotBoxId: {}]", ballotBoxId);
					final ScheduledFuture<?> sleep = scheduler.schedule(() -> {
					}, 10, TimeUnit.SECONDS);
					sleep.get();
				} catch (final InterruptedException | ExecutionException e) {
					Thread.currentThread().interrupt();
					final String errorMessage = String.format("Error while polling ballot box status. [electionEvenId: %s, ballotBoxId: %s]",
							electionEventId,
							ballotBoxId);
					throw new IllegalStateException(errorMessage, e);
				}

				LOGGER.info("Poll mix status, [verificationCardSetId: {}]", ballotBoxId);
				mixingStatus = mixDecryptService.getMixingStatus(electionEventId, ballotBoxId);
				LOGGER.info("Mix status, [ballotBoxId: {}, status: {}]", ballotBoxId, mixingStatus);
			}
		}

		// Update status
		ballotBoxService.updateStatus(ballotBoxId, mixingStatus);

		// Check if mixing failed
		if (BallotBoxStatus.MIXING_ERROR.equals(mixingStatus)) {
			LOGGER.error("Mixing failed. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);
			throw new IllegalStateException(
					String.format("Mixing failed. [electionEventId: %s, ballotBoxId:%s]", electionEventId, ballotBoxId));
		}
	}

}
