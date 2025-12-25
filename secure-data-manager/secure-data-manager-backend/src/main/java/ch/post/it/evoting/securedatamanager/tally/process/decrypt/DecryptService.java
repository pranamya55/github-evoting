/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.decrypt;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStatus.COMPLETE;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.DECRYPT;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.DECRYPT_BALLOT_BOX;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBox;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionCode;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowExceptionHandler;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowService;
import ch.post.it.evoting.securedatamanager.tally.process.VerifyElectoralBoardPasswordService;

@Service
@ConditionalOnProperty("role.isTally")
public class DecryptService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DecryptService.class);

	private final ExecutorService executorService;
	private final WorkflowService workflowService;
	private final BallotBoxService ballotBoxService;
	private final MixOfflineFacade mixOfflineFacade;
	private final WorkflowExceptionHandler workflowExceptionHandler;
	private final VerifyElectoralBoardPasswordService verifyElectoralBoardPasswordService;

	public DecryptService(
			final ExecutorService fixedThreadExecutorService,
			final WorkflowService workflowService,
			final BallotBoxService ballotBoxService,
			final MixOfflineFacade mixOfflineFacade,
			final WorkflowExceptionHandler workflowExceptionHandler,
			final VerifyElectoralBoardPasswordService verifyElectoralBoardPasswordService) {
		this.executorService = fixedThreadExecutorService;
		this.workflowService = workflowService;
		this.ballotBoxService = ballotBoxService;
		this.mixOfflineFacade = mixOfflineFacade;
		this.workflowExceptionHandler = workflowExceptionHandler;
		this.verifyElectoralBoardPasswordService = verifyElectoralBoardPasswordService;
	}

	public void decrypt(final String electionEventId, final ImmutableList<String> ballotBoxIds,
			final ImmutableList<SafePasswordHolder> electoralBoardPasswords) {
		validateUUID(electionEventId);
		checkNotNull(ballotBoxIds).forEach(Validations::validateUUID);
		checkNotNull(electoralBoardPasswords);
		checkArgument(electoralBoardPasswords.size() >= 2, "There must be at least two passwords.");

		// Create a safe copy of the passwords for validations.
		final ImmutableList<SafePasswordHolder> electoralBoardPasswordsCopyForValidation = electoralBoardPasswords.stream()
				.map(SafePasswordHolder::copy)
				.collect(toImmutableList());

		// Create a safe copy of the passwords for decryption.
		final ImmutableList<SafePasswordHolder> electoralBoardPasswordsCopyForDecryption = electoralBoardPasswords.stream()
				.map(SafePasswordHolder::copy)
				.collect(toImmutableList());

		// Wipe the passwords after usage.
		electoralBoardPasswords.forEach(SafePasswordHolder::clear);

		validatePasswords(electionEventId, electoralBoardPasswordsCopyForValidation)
				.thenRun(() -> performDecrypt(electionEventId, ballotBoxIds, electoralBoardPasswordsCopyForDecryption));
	}

	public ImmutableList<BallotBox> getBallotBoxes(final String electionEventId) {
		validateUUID(electionEventId);
		return ballotBoxService.getBallotBoxes(electionEventId);
	}

	/**
	 * Validates the electoral board passwords and returns a future that completes normally when all passwords have been validated or exceptionally if
	 * any validation failed.
	 */
	private CompletableFuture<Void> validatePasswords(final String electionEventId, final ImmutableList<SafePasswordHolder> electoralBoardPasswords) {
		workflowService.notifyInProgress(DECRYPT);
		LOGGER.debug("Validating electoral board passwords... [electionEventId: {}]", electionEventId);

		return CompletableFuture.allOf(IntStream.range(0, electoralBoardPasswords.size())
						.mapToObj(memberIndex -> {
							final Runnable verificationTask = () -> {
								final SafePasswordHolder electoralBoardPasswordCopyForVerification = electoralBoardPasswords.get(memberIndex).copy();

								checkArgument(verifyElectoralBoardPasswordService.verifyElectoralBoardMemberPassword(
										electionEventId, memberIndex, electoralBoardPasswordCopyForVerification), "The password is invalid.");
							};

							return CompletableFuture.runAsync(verificationTask, executorService)
									.whenComplete((verificationResult, throwable) -> {
										if (throwable != null) {
											LOGGER.error(
													"Verification of electoral board password has failed. [electionEventId: {}, memberIndex: {}]",
													electionEventId, memberIndex, throwable);
										} else {
											LOGGER.debug("Verification of electoral board password successful. [electionEventId: {} memberIndex: {}]",
													electionEventId, memberIndex);
										}
									});
						})
						.toArray(CompletableFuture[]::new))
				.whenComplete((unused, throwable) -> {
					// Wipe the passwords after usage.
					electoralBoardPasswords.forEach(SafePasswordHolder::clear);

					if (throwable != null) {
						final WorkflowExceptionCode exceptionCode = workflowExceptionHandler.handleException(DECRYPT, throwable);
						workflowService.notifyError(DECRYPT, exceptionCode);
						LOGGER.error("Verification of electoral board passwords has failed. [electionEventId: {}]", electionEventId);
					} else {
						LOGGER.info("Verification of electoral board passwords successful. [electionEventId: {}]", electionEventId);
					}
				});
	}

	/**
	 * Decrypts the ballot boxes asynchronously.
	 */
	private void performDecrypt(final String electionEventId, final ImmutableList<String> ballotBoxIds,
			final ImmutableList<SafePasswordHolder> electoralBoardPasswords) {
		LOGGER.debug("Decrypting ballot boxes... [electionEventId: {}]", electionEventId);

		final ImmutableList<CompletableFuture<Void>> futures = ballotBoxIds.stream()
				.map(ballotBoxId -> {
					final Runnable decryptTask = () -> {
						final ImmutableList<SafePasswordHolder> electoralBoardPasswordsCopyForBallotBoxDecryption = electoralBoardPasswords.stream()
								.map(SafePasswordHolder::copy)
								.collect(toImmutableList());
						workflowService.notifyInProgress(DECRYPT_BALLOT_BOX, ballotBoxId);
						mixOfflineFacade.mixOffline(electionEventId, ballotBoxId, electoralBoardPasswordsCopyForBallotBoxDecryption);
					};

					return CompletableFuture.runAsync(decryptTask, executorService)
							.whenComplete((unused, throwable) -> {
								if (throwable != null) {
									workflowService.notifyError(DECRYPT_BALLOT_BOX, ballotBoxId, WorkflowExceptionCode.DEFAULT);
									LOGGER.error("Decryption of ballot box failed. [electionEventId: {}, ballotBoxId: {}]", electionEventId,
											ballotBoxId, throwable);
								} else {
									workflowService.notifyComplete(DECRYPT_BALLOT_BOX, ballotBoxId);
									LOGGER.info("Decryption of ballot box successful. [electionEventId: {}, ballotBoxId: {}]", electionEventId,
											ballotBoxId);
								}
							});
				})
				.collect(toImmutableList());

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[] {}))
				.whenComplete((unused, throwable) -> {
					// Wipe the passwords after usage.
					electoralBoardPasswords.forEach(SafePasswordHolder::clear);

					if (throwable != null) {
						final WorkflowExceptionCode exceptionCode = workflowExceptionHandler.handleException(DECRYPT, throwable);
						workflowService.notifyError(DECRYPT, exceptionCode);
						LOGGER.error("The workflow step [{}] has failed.", DECRYPT.name(), throwable);
					} else {
						// Check children states to determine if the workflow step is complete.
						final boolean allComplete = this.workflowService.getWorkflowStateList().stream()
								.filter(workflowState -> workflowState.step().equals(DECRYPT_BALLOT_BOX))
								.allMatch(workflowState -> COMPLETE.equals(workflowState.status()));

						if (allComplete) {
							this.workflowService.notifyComplete(DECRYPT);
						} else {
							this.workflowService.notifyReady(DECRYPT);
						}

						LOGGER.info("The workflow step [{}] has been successfully processed.", DECRYPT.name());
					}
				});
	}

}
