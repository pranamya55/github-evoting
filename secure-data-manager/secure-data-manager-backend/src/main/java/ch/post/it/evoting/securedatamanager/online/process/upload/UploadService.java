/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.upload;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStepRunner;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowTask;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class UploadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadService.class);

	private final WorkflowStepRunner workflowStepRunner;
	private final UploadElectoralBoardService uploadElectoralBoardService;
	private final UploadVerificationCardSetService uploadVerificationCardSetService;

	public UploadService(
			final WorkflowStepRunner workflowStepRunner,
			final UploadElectoralBoardService uploadElectoralBoardService,
			final UploadVerificationCardSetService uploadVerificationCardSetService) {
		this.workflowStepRunner = workflowStepRunner;
		this.uploadElectoralBoardService = uploadElectoralBoardService;
		this.uploadVerificationCardSetService = uploadVerificationCardSetService;
	}

	public void upload(final String electionEventId, final WorkflowStep uploadConfigurationStep) {
		validateUUID(electionEventId);
		checkNotNull(uploadConfigurationStep);

		LOGGER.debug("Uploading the election configuration... [electionEventId: {}]", electionEventId);

		final WorkflowTask workflowTask = new WorkflowTask(
				() -> performUpload(electionEventId, uploadConfigurationStep),
				() -> LOGGER.info("Upload of election configuration successful. [electionEventId: {}]", electionEventId),
				throwable -> LOGGER.error("Upload of election configuration failed. [electionEventId: {}]", electionEventId, throwable)
		);

		workflowStepRunner.run(uploadConfigurationStep, workflowTask);
	}

	private void performUpload(final String electionEventId, final WorkflowStep uploadConfigurationStep) {
		switch (uploadConfigurationStep) {
		case UPLOAD_CONFIGURATION_1 -> uploadVerificationCardSetService.upload(electionEventId);
		case UPLOAD_CONFIGURATION_2 -> uploadElectoralBoardService.upload(electionEventId);
		default -> throw new IllegalArgumentException(
				String.format("Unsupported upload configuration step. [WorkflowStep: %s]", uploadConfigurationStep));
		}
	}

}
