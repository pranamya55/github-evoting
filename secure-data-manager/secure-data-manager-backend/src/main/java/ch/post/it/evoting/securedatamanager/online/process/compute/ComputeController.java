/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.compute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

@RestController
@RequestMapping("/sdm-online/compute")
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class ComputeController {
	private static final Logger LOGGER = LoggerFactory.getLogger(ComputeController.class);

	private final ComputeService computeService;
	private final ElectionEventService electionEventService;

	public ComputeController(
			final ComputeService computeService,
			final ElectionEventService electionEventService) {
		this.computeService = computeService;
		this.electionEventService = electionEventService;
	}

	@PostMapping()
	public void compute() {
		final String electionEventId = electionEventService.findElectionEventId();

		LOGGER.debug("Received request to start computation. [electionEventId: {}]", electionEventId);

		computeService.compute(electionEventId);

		LOGGER.info("The computation has been started. [electionEventId: {}]", electionEventId);

	}

}
