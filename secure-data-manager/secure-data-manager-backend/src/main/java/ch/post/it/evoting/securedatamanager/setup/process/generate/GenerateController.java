/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

@RestController
@RequestMapping("/sdm-setup/generate")
@ConditionalOnProperty("role.isSetup")
public class GenerateController {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateController.class);
	private final GenerateService generateService;
	private final ElectionEventService electionEventService;

	public GenerateController(
			final GenerateService generateService,
			final ElectionEventService electionEventService) {
		this.generateService = generateService;
		this.electionEventService = electionEventService;
	}

	@PostMapping()
	public void generate() {

		final String electionEventId = electionEventService.findElectionEventId();

		LOGGER.debug("Received request to generate the voting cards. [electionEventId: {}]", electionEventId);

		generateService.generate(electionEventId);

		LOGGER.info("The generation has been started. [electionEventId: {}]", electionEventId);
	}

}
