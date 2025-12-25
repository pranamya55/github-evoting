/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.collectdataverifier;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.COLLECT_DATA_VERIFIER_SETUP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.securedatamanager.shared.process.DatasetInfo;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;
import ch.post.it.evoting.securedatamanager.shared.process.VerifierCollectorService;
import ch.post.it.evoting.securedatamanager.shared.process.VerifierExportType;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStepRunner;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowTask;

@Service
@ConditionalOnProperty("role.isSetup")
public class CollectDataVerifierService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CollectDataVerifierService.class);

	private final PathResolver pathResolver;
	private final WorkflowStepRunner workflowStepRunner;
	private final VerifierCollectorService verifierCollectorService;

	public CollectDataVerifierService(
			final PathResolver pathResolver,
			final WorkflowStepRunner workflowStepRunner,
			final VerifierCollectorService verifierCollectorService) {
		this.pathResolver = pathResolver;
		this.workflowStepRunner = workflowStepRunner;
		this.verifierCollectorService = verifierCollectorService;
	}

	public void collectData(final String electionEventId) {
		validateUUID(electionEventId);

		LOGGER.debug("Collecting the data for the Verifier (Setup)... [electionEventId: {}]", electionEventId);

		final WorkflowTask workflowTask = new WorkflowTask(
				() -> performCollect(electionEventId),
				() -> LOGGER.info("Collection of data for the Verifier (Setup) successful. [electionEventId: {}]", electionEventId),
				throwable -> LOGGER.error("Collection of data for the Verifier (Setup) failed. [electionEventId: {}]", electionEventId, throwable)
		);

		workflowStepRunner.run(COLLECT_DATA_VERIFIER_SETUP, workflowTask);
	}

	public DatasetInfo getDatasetFilenameList() {
		final String verifierOutputFolderPath = pathResolver.resolveVerifierOutputPath().toString();
		return new DatasetInfo(verifierOutputFolderPath,
				ImmutableList.of(verifierCollectorService.getExportFilename(VerifierExportType.CONTEXT, false)));
	}

	private void performCollect(final String electionEventId) {
		ImmutableList.of(VerifierExportType.CONTEXT)
				.forEach(verifierExportType -> verifierCollectorService.collectDataset(verifierExportType, electionEventId));
	}

}
