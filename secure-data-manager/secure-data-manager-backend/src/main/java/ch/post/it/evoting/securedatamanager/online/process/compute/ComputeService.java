/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.compute;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.GET_STATUS_UNSUCCESSFUL_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.COMPUTE;
import static com.google.common.base.Preconditions.checkState;

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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.configuration.setupvoting.ComputingStatus;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationDataPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.Status;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionHandler;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowService;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class ComputeService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ComputeService.class);

	private final ExecutorService executorService;
	private final WorkflowService workflowService;
	private final WebClientFactory webClientFactory;
	private final WorkflowExceptionHandler workflowExceptionHandler;
	private final VerificationCardSetService verificationCardSetService;
	private final ComputeVerificationCardSetService computeVerificationCardSetService;
	private final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository;

	public ComputeService(
			final WorkflowService workflowService,
			final WebClientFactory webClientFactory,
			final ExecutorService fixedThreadExecutorService,
			final WorkflowExceptionHandler workflowExceptionHandler,
			final VerificationCardSetService verificationCardSetService,
			final ComputeVerificationCardSetService computeVerificationCardSetService,
			final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository
	) {
		this.workflowService = workflowService;
		this.webClientFactory = webClientFactory;
		this.executorService = fixedThreadExecutorService;
		this.workflowExceptionHandler = workflowExceptionHandler;
		this.verificationCardSetService = verificationCardSetService;
		this.computeVerificationCardSetService = computeVerificationCardSetService;
		this.setupComponentVerificationDataPayloadFileRepository = setupComponentVerificationDataPayloadFileRepository;
	}

	public void compute(final String electionEventId) {
		validateUUID(electionEventId);

		LOGGER.info("Computing the verification card sets. [electionEventId: {}]", electionEventId);

		final ImmutableList<String> verificationCardSetIds = verificationCardSetService.getVerificationCardSetIds(electionEventId);
		final ImmutableList<String> precomputedVerificationCardSetIds = verificationCardSetService.getVerificationCardSetIds(Status.PRECOMPUTED);
		final ImmutableList<String> computingVerificationCardSetIds = verificationCardSetService.getVerificationCardSetIds(Status.COMPUTING);

		// Notify the workflow that the computation is in progress.
		workflowService.notifyInProgress(COMPUTE);

		// Run the computations asynchronously.
		final ImmutableList<CompletableFuture<Void>> computationFutures = verificationCardSetIds.stream()
				// Prepare the pre-computation tasks.
				.map(verificationCardSetId -> {
					final Runnable computationTask = () -> {
						if (precomputedVerificationCardSetIds.contains(verificationCardSetId)) {
							// Start computation.
							computeVerificationCardSetService.compute(electionEventId, verificationCardSetId);

							// Polling status.
							pollVerificationCardSetStatus(electionEventId, verificationCardSetId);
						}
						// If the verification card set is already computing, poll the status.
						if (computingVerificationCardSetIds.contains(verificationCardSetId)) {
							pollVerificationCardSetStatus(electionEventId, verificationCardSetId);
						}
					};

					return CompletableFuture.runAsync(computationTask, executorService)
							.exceptionally(throwable -> {
								LOGGER.error("Computation of verification card set failed. [electionEvenId: {}, verificationCardSetId: {}]",
										electionEventId,
										verificationCardSetId, throwable);
								return null;
							});
				})
				.collect(toImmutableList());

		CompletableFuture.allOf(computationFutures.toArray(new CompletableFuture[] {}))
				.thenRun(() -> {
					final ImmutableList<String> computedVerificationCardSetIds = verificationCardSetService.getVerificationCardSetIds(
							Status.COMPUTED);
					if (computedVerificationCardSetIds.size() == verificationCardSetIds.size()) {
						workflowService.notifyComplete(COMPUTE);
						LOGGER.info("The verification card sets has been successfully computed. [electionEventId: {}]", electionEventId);
					} else {
						workflowService.notifyError(COMPUTE, WorkflowExceptionCode.DEFAULT);
						LOGGER.error("Not all verification card sets were computed. [electionEventId: {}]", electionEventId);
					}
				})
				.exceptionally(throwable -> {
					final WorkflowExceptionCode exceptionCode = workflowExceptionHandler.handleException(COMPUTE, throwable);
					workflowService.notifyError(COMPUTE, exceptionCode);
					LOGGER.error("Some of the computations failed. [electionEventId: {}]", electionEventId);
					return null;
				});
	}

	/**
	 * Check if the control components finished the generation of the encrypted long Return Code Shares and update the status of the verification card
	 * set they belong to. The SDM checks if the number of generated encrypted long Return Code Shares (number of chunks) in the voting server
	 * corresponds to the number of Setup Component verification data (number of chunks) in the SDM.
	 */
	private void pollVerificationCardSetStatus(final String electionEventId, final String verificationCardSetId) {
		final int chunkCount = setupComponentVerificationDataPayloadFileRepository.getCount(electionEventId, verificationCardSetId);

		ComputingStatus computingStatus = ComputingStatus.COMPUTING;
		try (final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {

			while (ComputingStatus.COMPUTING.equals(computingStatus)) {
				try {
					LOGGER.info("Wait before polling compute status. [verificationCardSetId: {}]", verificationCardSetId);
					final ScheduledFuture<?> sleep = scheduler.schedule(() -> {
					}, 10, TimeUnit.SECONDS);
					sleep.get();
				} catch (final InterruptedException | ExecutionException e) {
					Thread.currentThread().interrupt();
					final String errorMessage = String.format(
							"Error while polling verification card set status. [electionEvenId: %s, verificationCardSetId: %s]", electionEventId,
							verificationCardSetId);
					throw new IllegalStateException(errorMessage, e);
				}

				LOGGER.info("Poll compute status. [verificationCardSetId: {}]", verificationCardSetId);
				computingStatus = webClientFactory.getWebClient(
								String.format(GET_STATUS_UNSUCCESSFUL_MESSAGE + "[electionEventId: %s, verificationCardSetId: %s, chunkCount: %s]",
										electionEventId, verificationCardSetId, chunkCount))
						.get()
						.uri(uriBuilder -> uriBuilder.path(
										"api/v1/configuration/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}/chunkcount/{chunkCount}/status")
								.build(electionEventId, verificationCardSetId, chunkCount))
						.accept(MediaType.APPLICATION_JSON)
						.retrieve()
						.bodyToMono(ComputingStatus.class)
						.block();
				LOGGER.info("Compute status. [verificationCardSetId: {}, status: {}]", verificationCardSetId, computingStatus);
			}
		}

		// Update status
		checkState(computingStatus != null);
		verificationCardSetService.updateStatus(verificationCardSetId, computingStatus);

		if (ComputingStatus.COMPUTING_ERROR.equals(computingStatus)) {
			throw new IllegalStateException(String.format("Computation failed. [electionEventId: %s, verificationCardSetId:%s]", electionEventId,
					verificationCardSetId));
		}
	}

}
