/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.collectdataverifier;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.COLLECT_DATA_VERIFIER_TALLY;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.securedatamanager.shared.process.DatasetInfo;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;
import ch.post.it.evoting.securedatamanager.shared.process.TallyFileInfo;
import ch.post.it.evoting.securedatamanager.shared.process.VerifierCollectorService;
import ch.post.it.evoting.securedatamanager.shared.process.VerifierExportType;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStepRunner;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowTask;
import ch.post.it.evoting.securedatamanager.tally.process.TallyPathResolver;

@Service
@ConditionalOnProperty("role.isTally")
public class CollectDataVerifierService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CollectDataVerifierService.class);

	private final PathResolver pathResolver;
	private final TallyPathResolver tallyPathResolver;
	private final WorkflowStepRunner workflowStepRunner;
	private final VerifierCollectorService verifierCollectorService;
	private final TallyComponentFilesService tallyComponentFilesService;

	public CollectDataVerifierService(
			final PathResolver pathResolver,
			final TallyPathResolver tallyPathResolver,
			final WorkflowStepRunner workflowStepRunner,
			final VerifierCollectorService verifierCollectorService,
			final TallyComponentFilesService tallyComponentFilesService) {
		this.pathResolver = pathResolver;
		this.tallyPathResolver = tallyPathResolver;
		this.workflowStepRunner = workflowStepRunner;
		this.verifierCollectorService = verifierCollectorService;
		this.tallyComponentFilesService = tallyComponentFilesService;
	}

	public void collectData(final String electionEventId) {
		validateUUID(electionEventId);

		LOGGER.debug("Collecting the data for the Verifier (Tally)... [electionEventId: {}]", electionEventId);

		final WorkflowTask workflowTask = new WorkflowTask(
				() -> performCollect(electionEventId),
				() -> LOGGER.info("Collection of data for the Verifier (Tally) successful. [electionEventId: {}]", electionEventId),
				throwable -> LOGGER.error("Collection of data for the Verifier (Tally) failed. [electionEventId: {}]", electionEventId, throwable)
		);

		workflowStepRunner.run(COLLECT_DATA_VERIFIER_TALLY, workflowTask);
	}

	public DatasetInfo getDatasetFilenameList() {
		final String verifierOutputFolderPath = pathResolver.resolveVerifierOutputPath().toString();

		return new DatasetInfo(verifierOutputFolderPath,
				ImmutableList.of(verifierCollectorService.getExportFilename(VerifierExportType.TALLY, false)));
	}

	public TallyFileInfo getTallyFileInfo() {
		final String tallyOutputFolderPath = tallyPathResolver.resolveTallyOutputPath().toString();

		return new TallyFileInfo(tallyOutputFolderPath,
				ImmutableList.of(verifierCollectorService.getExportECH0222Filename(VerifierExportType.TALLY)));
	}

	private void performCollect(final String electionEventId) {
		tallyComponentFilesService.generate(electionEventId);
		ImmutableList.of(VerifierExportType.TALLY)
				.forEach(verifierExportType -> verifierCollectorService.collectDataset(verifierExportType, electionEventId));
	}

}
