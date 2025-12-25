/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generateprintfile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

@RestController
@ConditionalOnProperty("role.isSetup")
@RequestMapping("/sdm-setup/generate-print-file")
public class GeneratePrintFileController {

	private static final Logger LOGGER = LoggerFactory.getLogger(GeneratePrintFileController.class);

	private final ElectionEventService electionEventService;
	private final GeneratePrintFileService generatePrintFileService;

	public GeneratePrintFileController(
			final ElectionEventService electionEventService,
			final GeneratePrintFileService generatePrintFileService) {
		this.electionEventService = electionEventService;
		this.generatePrintFileService = generatePrintFileService;
	}

	@PostMapping
	public void generatePrintFile() {
		final String electionEventId = electionEventService.findElectionEventId();

		LOGGER.debug("Received request to generate evoting-print and ballot boxes report files. [electionEventId: {}]", electionEventId);

		generatePrintFileService.generate(electionEventId);

		LOGGER.info("The evoting-print and ballot boxes report files generation has been started. [electionEventId: {}]", electionEventId);
	}

	@GetMapping
	public PrintInfo getPrintInfo() {
		LOGGER.debug("Received request to retrieve the print information.");

		final PrintInfo printInfo = generatePrintFileService.getPrintInfo();
		LOGGER.debug("Retrieved print information.");

		return printInfo;
	}

}
