/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generateprintfile;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.GENERATE_PRINT_FILE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStepRunner;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowTask;

@Service
@ConditionalOnProperty("role.isSetup")
public class GeneratePrintFileService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GeneratePrintFileService.class);

	private final WorkflowStepRunner workflowStepRunner;
	private final EvotingPrintService evotingPrintService;
	private final BallotBoxesReportService ballotBoxesReportService;

	public GeneratePrintFileService(
			final WorkflowStepRunner workflowStepRunner,
			final EvotingPrintService evotingPrintService,
			final BallotBoxesReportService ballotBoxesReportService) {
		this.workflowStepRunner = workflowStepRunner;
		this.evotingPrintService = evotingPrintService;
		this.ballotBoxesReportService = ballotBoxesReportService;
	}

	/**
	 * Asynchronously generates the evoting-print and ballot boxes report files. If either one fails, the whole step fails.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if {@code electionEventId} is null.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 */
	public void generate(final String electionEventId) {
		validateUUID(electionEventId);

		// Evoting print.
		final WorkflowTask printFileWorkflowTask = new WorkflowTask(
				() -> evotingPrintService.generate(electionEventId),
				() -> LOGGER.info("Generation of evoting print file successful. [electionEventId: {}]", electionEventId),
				throwable -> LOGGER.error("Generation of evoting print file failed. [electionEventId: {}]", electionEventId, throwable)
		);

		// Ballot boxes report.
		final WorkflowTask ballotBoxesWorkflowTask = new WorkflowTask(
				() -> ballotBoxesReportService.generate(electionEventId),
				() -> LOGGER.info("Generation of ballot box report file successful. [electionEventId: {}]", electionEventId),
				throwable -> LOGGER.error("Generation of ballot box report file failed. [electionEventId: {}]", electionEventId, throwable)
		);

		workflowStepRunner.run(GENERATE_PRINT_FILE, ImmutableList.of(printFileWorkflowTask, ballotBoxesWorkflowTask));
	}

	/**
	 * Retrieves the print information.
	 *
	 * @return the print information.
	 */
	public PrintInfo getPrintInfo() {
		return evotingPrintService.getPrintInfo();
	}

}
