/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.download;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.DOWNLOAD;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.securedatamanager.shared.process.Status;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStepRunner;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowTask;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class DownloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadService.class);

	private final boolean sequential;
	private final WorkflowStepRunner workflowStepRunner;
	private final VerificationCardSetService verificationCardSetService;
	private final DownloadVerificationCardSetService downloadVerificationCardSetService;

	public DownloadService(
			@Value("${sdm.process.download.sequential}")
			final boolean sequential,
			final WorkflowStepRunner workflowStepRunner,
			final VerificationCardSetService verificationCardSetService,
			final DownloadVerificationCardSetService downloadVerificationCardSetService) {
		this.sequential = sequential;
		this.workflowStepRunner = workflowStepRunner;
		this.verificationCardSetService = verificationCardSetService;
		this.downloadVerificationCardSetService = downloadVerificationCardSetService;
	}

	public void download(final String electionEventId) {
		validateUUID(electionEventId);

		LOGGER.debug("Downloading verification card sets... [electionEventId: {}]", electionEventId);

		final ImmutableList<String> computedVerificationCardSetIds = verificationCardSetService.getVerificationCardSetIds(Status.COMPUTED);

		final ImmutableList<WorkflowTask> workflowTasks = computedVerificationCardSetIds.stream()
				.map(verificationCardSetId ->
						new WorkflowTask(
								() -> downloadVerificationCardSetService.download(electionEventId, verificationCardSetId),
								() -> LOGGER.info("Download of verification card set successful. [electionEventId: {}, verificationCardSetId: {}]",
										electionEventId, verificationCardSetId),
								throwable -> LOGGER.error(
										"Download of verification card set failed. [electionEventId: {}, verificationCardSetId: {}]",
										electionEventId, verificationCardSetId, throwable)
						)
				)
				.collect(toImmutableList());

		if (sequential) {
			workflowStepRunner.runSequential(DOWNLOAD, workflowTasks);
		} else {
			workflowStepRunner.run(DOWNLOAD, workflowTasks);
		}
	}

}
