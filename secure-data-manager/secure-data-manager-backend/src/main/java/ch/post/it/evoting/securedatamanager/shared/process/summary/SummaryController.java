/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

/**
 * Endpoint to get a summary of the election event.
 */
@RestController
@RequestMapping("/sdm-shared/summary")
public class SummaryController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SummaryController.class);

	private final SummaryService summaryService;
	private final ElectionEventService electionEventService;

	public SummaryController(
			final SummaryService summaryService,
			final ElectionEventService electionEventService) {
		this.summaryService = summaryService;
		this.electionEventService = electionEventService;
	}

	@GetMapping
	public ConfigurationSummary getSummary() {
		final String electionEventId = electionEventService.findElectionEventId();

		LOGGER.debug("Received request to get the contest summary. [electionEventId: {}]", electionEventId);

		return summaryService.getConfigurationSummary();
	}

}
