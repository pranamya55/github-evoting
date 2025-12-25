/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.collectdataverifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.securedatamanager.shared.process.DatasetInfo;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

@RestController
@RequestMapping("/sdm-setup/collect-data-verifier")
@ConditionalOnProperty("role.isSetup")
public class CollectDataVerifierController {

	private static final Logger LOGGER = LoggerFactory.getLogger(CollectDataVerifierController.class);
	private final CollectDataVerifierService collectDataVerifierService;
	private final ElectionEventService electionEventService;

	public CollectDataVerifierController(
			final CollectDataVerifierService collectDataVerifierService,
			final ElectionEventService electionEventService) {
		this.collectDataVerifierService = collectDataVerifierService;
		this.electionEventService = electionEventService;
	}

	@PostMapping()
	public void collectDataVerifier() {

		final String electionEventId = electionEventService.findElectionEventId();

		LOGGER.debug("Starting data collection for the Verifier (Setup)... [electionEventId: {}]", electionEventId);

		collectDataVerifierService.collectData(electionEventId);

		LOGGER.info("Data collection for the Verifier (Setup) has been started. [electionEventId: {}]", electionEventId);
	}

	@GetMapping("/dataset-info")
	public DatasetInfo getFilenames() {
		return collectDataVerifierService.getDatasetFilenameList();
	}
}
