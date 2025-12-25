/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.preconfigure;

import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.PRE_CONFIGURE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.securedatamanager.setup.process.SetupEvotingConfigFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigService;
import ch.post.it.evoting.securedatamanager.shared.process.summary.ConfigurationSummary;
import ch.post.it.evoting.securedatamanager.shared.process.summary.SummaryService;
import ch.post.it.evoting.securedatamanager.shared.workflow.PreWorkflowTask;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStepRunner;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowTask;

@Service
@ConditionalOnProperty("role.isSetup")
public class PreConfigureService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PreConfigureService.class);

	private final SummaryService summaryService;
	private final WorkflowStepRunner workflowStepRunner;
	private final EvotingConfigService evotingConfigService;
	private final PreConfigureRepository preConfigureRepository;
	private final SetupEvotingConfigFileRepository setupEvotingConfigFileRepository;
	private final ElectionEventConfigService electionEventConfigService;

	public PreConfigureService(
			final SummaryService summaryService,
			final WorkflowStepRunner workflowStepRunner,
			final EvotingConfigService evotingConfigService,
			final PreConfigureRepository preConfigureRepository,
			final SetupEvotingConfigFileRepository setupEvotingConfigFileRepository,
			final ElectionEventConfigService electionEventConfigService) {
		this.summaryService = summaryService;
		this.setupEvotingConfigFileRepository = setupEvotingConfigFileRepository;
		this.workflowStepRunner = workflowStepRunner;
		this.evotingConfigService = evotingConfigService;
		this.preConfigureRepository = preConfigureRepository;
		this.electionEventConfigService = electionEventConfigService;
	}

	/**
	 * Gets for preview the {@link ConfigurationSummary} from the configuration anonymized file located in the external configuration path.
	 *
	 * @return the configuration summary.
	 */
	public ConfigurationSummary previewConfigurationSummary() {
		LOGGER.debug("Previewing the configuration summary...");

		final Configuration configuration = setupEvotingConfigFileRepository.loadExternalConfiguration()
				.orElseThrow(() -> new PreviewSummaryException("The configuration-anonymized file does not exist."));

		return summaryService.getConfigurationSummary(configuration);
	}

	/**
	 * Asynchronously pre-configures the election event. First, a pre-task is executed to copy the configuration anonymized file from the external to
	 * the internal location.
	 */
	public void preConfigureElectionEvent() {
		LOGGER.debug("Preconfiguring the election event...");

		final PreWorkflowTask<Void> preWorkflowTask = new PreWorkflowTask<>(() -> {
			copyConfigurationToInternalPath();
			return null;
		});

		final WorkflowTask workflowTask = new WorkflowTask(
				this::performPreconfigure,
				() -> LOGGER.info("Preconfigure of election event successful."),
				throwable -> LOGGER.error("Preconfigure of election event failed.", throwable)
		);

		workflowStepRunner.run(PRE_CONFIGURE, preWorkflowTask, workflowTask);
	}

	/**
	 * Copies the configuration-anonymized file from the external to the internal path.
	 * <p>
	 * This method validates the configuration-anonymized file against the provided schema.
	 *
	 * @throws IllegalStateException if the configuration-anonymized file does not exist in the external configuration path.
	 */
	private void copyConfigurationToInternalPath() {

		evotingConfigService.copyConfigurationToInternalPath();

		LOGGER.info("Successfully copied the evoting-config file.");
	}

	/**
	 * Implements the preconfigure process:
	 * <ul>
	 *     <li>creates the election_config.json file,</li>
	 *     <li>loads the database from the election_config,</li>
	 *     <li>creates the election event.</li>
	 * </ul>
	 */
	private void performPreconfigure() {
		final String electionEventId;
		LOGGER.debug("Creating the database entities...");
		electionEventId = preConfigureRepository.createElectionsConfig();
		LOGGER.info("Successfully created the database entities. [electionEventId: {}]", electionEventId);

		LOGGER.debug("Creating the election event context...");
		electionEventConfigService.create(electionEventId);
		LOGGER.info("Successfully created the election event context. [electionEventId: {}]", electionEventId);
	}

}
