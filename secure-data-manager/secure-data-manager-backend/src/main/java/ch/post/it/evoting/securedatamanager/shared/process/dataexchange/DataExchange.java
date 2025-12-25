/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.dataexchange;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;
import ch.post.it.evoting.securedatamanager.shared.workflow.PreWorkflowTask;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStepRunner;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowTask;

public abstract class DataExchange {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataExchange.class);

	private final PathResolver pathResolver;
	private final WorkflowStepRunner workflowStepRunner;
	private final ImportExportService importExportService;

	protected DataExchange(
			final PathResolver pathResolver,
			final WorkflowStepRunner workflowStepRunner,
			final ImportExportService importExportService) {
		this.pathResolver = pathResolver;
		this.workflowStepRunner = workflowStepRunner;
		this.importExportService = importExportService;
	}

	public void exportSDMData(final String electionEventId, final int exchangeIndex) {
		validateUUID(electionEventId);
		checkArgument(exchangeIndex >= 0, "Exchange index must be positive.");

		LOGGER.debug("Exporting SDM data... [electionEventId: {}, exchangeIndex: {}]", electionEventId, exchangeIndex);

		final WorkflowStep exportWorkflowStep = WorkflowStep.getExportStep(exchangeIndex);

		final WorkflowTask workflowTask = new WorkflowTask(
				() -> importExportService.exportElectionEventData(electionEventId, exchangeIndex),
				() -> LOGGER.info("SDM data exported successfully. [electionEventId: {}, workflowStep: {}]", electionEventId, exportWorkflowStep),
				throwable -> LOGGER.error("SDM data export failed. [electionEventId: {}, workflowStep: {}]", electionEventId, exportWorkflowStep,
						throwable)
		);

		workflowStepRunner.run(exportWorkflowStep, workflowTask);
	}

	public void importSDMData(final int exchangeIndex, final MultipartFile zip) {
		checkArgument(exchangeIndex >= 0, "Exchange index must be positive.");
		checkNotNull(zip);

		final WorkflowStep importWorkflowStep = WorkflowStep.getImportStep(exchangeIndex);
		LOGGER.debug("Importing SDM data... [exchangeIndex: {}, workflowStep: {}]", exchangeIndex, importWorkflowStep);

		final PreWorkflowTask<Path> preWorkflowTask = new PreWorkflowTask<>(() -> performPreWorkflowTask(exchangeIndex, zip, importWorkflowStep));

		final WorkflowTask workflowTask = new WorkflowTask(
				() -> importExportService.importElectionEventData(exchangeIndex, preWorkflowTask.get()),
				() -> LOGGER.info("SDM data imported successfully. [exchangeIndex: {}, workflowStep: {}]", exchangeIndex, importWorkflowStep),
				throwable -> LOGGER.error("SDM data import failed. [exchangeIndex: {}, workflowStep: {}]", exchangeIndex, importWorkflowStep,
						throwable)
		);

		workflowStepRunner.run(importWorkflowStep, preWorkflowTask, workflowTask, () -> deleteTemporaryImport(preWorkflowTask.get()));
	}

	private Path performPreWorkflowTask(final int exchangeIndex, final MultipartFile zip, final WorkflowStep importWorkflowStep) {
		// Save own copy of import file to avoid it being deleted by Spring too early. It is important to do this in the main thread.
		final Path importTempPath = createTemporaryImport(zip);
		LOGGER.debug("Temporary import file created. [exchangeIndex: {}, workflowStep: {}, importTempPath: {}]", exchangeIndex, importWorkflowStep,
				importTempPath);
		return importTempPath;
	}

	public ExportInfo getExportInfo(final int exchangeIndex) {
		return new ExportInfo(pathResolver.resolveOutputPath().toString(), importExportService.getExportFilename(exchangeIndex, false));
	}

	// The temporary file is created, used and immediately deleted after use, minimizing security risks.
	// The code handles exceptions and ensures cleanup of the temporary file.
	@SuppressWarnings("java:S5443")
	private Path createTemporaryImport(final MultipartFile zip) {
		Path importTempPath = null;
		try {
			importTempPath = Files.createTempFile(null, null);
			zip.transferTo(importTempPath);
		} catch (final IOException e) {
			if (importTempPath != null) {
				deleteTemporaryImport(importTempPath);
			}
			throw new UncheckedIOException("Failed to create temporary import file.", e);
		}

		return importTempPath;
	}

	private void deleteTemporaryImport(final Path importTempPath) {
		try {
			Files.deleteIfExists(importTempPath);
		} catch (final IOException e) {
			throw new UncheckedIOException("Failed to delete temporary import file.", e);
		}
	}
}
