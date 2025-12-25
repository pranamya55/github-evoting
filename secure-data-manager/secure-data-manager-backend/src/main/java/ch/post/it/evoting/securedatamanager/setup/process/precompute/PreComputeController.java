/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.precompute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

@RestController
@RequestMapping("/sdm-setup/pre-compute")
@ConditionalOnProperty("role.isSetup")
public class PreComputeController {
	private static final Logger LOGGER = LoggerFactory.getLogger(PreComputeController.class);
	private final ElectionEventService electionEventService;
	private final PreComputeService preComputeService;

	public PreComputeController(
			final ElectionEventService electionEventService,
			final PreComputeService preComputeService) {
		this.electionEventService = electionEventService;
		this.preComputeService = preComputeService;
	}

	@PostMapping()
	public void precompute() {
		LOGGER.debug("Starting pre-computation...");

		final String electionEventId = electionEventService.findElectionEventId();
		preComputeService.preCompute(electionEventId);

		LOGGER.info("The pre-computation has been started. [electionEventId: {}]", electionEventId);
	}
}
