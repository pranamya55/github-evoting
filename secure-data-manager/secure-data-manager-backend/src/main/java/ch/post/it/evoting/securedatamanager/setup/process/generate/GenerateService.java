/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generate;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.process.Status.VCS_DOWNLOADED;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.GENERATE;
import static com.google.common.base.Preconditions.checkState;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.securedatamanager.shared.process.Status;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStepRunner;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowTask;

@Service
@ConditionalOnProperty("role.isSetup")
public class GenerateService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateService.class);

	private final WorkflowStepRunner workflowStepRunner;
	private final VerificationCardSetService verificationCardSetService;
	private final ReturnCodesPayloadsGenerateService returnCodesPayloadsGenerateService;

	public GenerateService(
			final WorkflowStepRunner workflowStepRunner,
			final VerificationCardSetService verificationCardSetService,
			final ReturnCodesPayloadsGenerateService returnCodesPayloadsGenerateService) {
		this.workflowStepRunner = workflowStepRunner;
		this.verificationCardSetService = verificationCardSetService;
		this.returnCodesPayloadsGenerateService = returnCodesPayloadsGenerateService;
	}

	/**
	 * Generates the verification card set data. The generation contains 3 steps: generate the ballot box data, generate the ballot file and finally
	 * the Return Codes payloads.
	 *
	 * @param electionEventId The id of the election event.
	 */
	public void generate(final String electionEventId) {
		validateUUID(electionEventId);

		LOGGER.info("Generating the verification card sets. [electionEventId: {}]", electionEventId);

		// Retrieve all verification card sets not yet generated.
		final ImmutableList<String> downloadedVerificationCardSetIds = verificationCardSetService.getVerificationCardSetIds(VCS_DOWNLOADED);

		// If there is no verification card set to generate, do nothing.
		if (!downloadedVerificationCardSetIds.isEmpty()) {
			LOGGER.info("Found verification card sets to generate. [electionEventId: {}, amount: {}]", electionEventId,
					downloadedVerificationCardSetIds.size());
		} else {
			LOGGER.info("No verification card set to generate. [electionEventId: {}}", electionEventId);
			return;
		}

		// Prepare generate tasks.
		final ImmutableList<WorkflowTask> generateWorkflowTasks = downloadedVerificationCardSetIds.stream()
				.map(verificationCardSetId -> {
					final Runnable generateTask = () ->
							// Generate the Return Codes payloads.
							returnCodesPayloadsGenerateService.generate(electionEventId, verificationCardSetId);

					final Runnable successAction = () -> LOGGER.info(
							"Generation of verification card set successful. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
							verificationCardSetId);
					final Consumer<Throwable> failureAction = throwable -> LOGGER.error(
							"Generation of verification card set failed. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
							verificationCardSetId, throwable);

					return new WorkflowTask(generateTask, successAction, failureAction);
				})
				.collect(toImmutableList());

		final Runnable completeAction = () -> {
			final ImmutableList<String> generatedVerificationCardSetIds = verificationCardSetService.getVerificationCardSetIds(Status.GENERATED);
			final ImmutableList<String> allVerificationCardSetIds = verificationCardSetService.getVerificationCardSetIds(electionEventId);
			checkState(allVerificationCardSetIds.size() == generatedVerificationCardSetIds.size(),
					"The verification card sets are not all generated. [electionEventId: %s, verificationCardSetIds: %s]", electionEventId,
					generatedVerificationCardSetIds);
		};

		workflowStepRunner.run(GENERATE, generateWorkflowTasks, completeAction);
	}

}
