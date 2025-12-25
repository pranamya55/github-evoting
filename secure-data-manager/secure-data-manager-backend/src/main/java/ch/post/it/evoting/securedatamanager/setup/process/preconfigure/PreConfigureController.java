/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.preconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.securedatamanager.shared.process.summary.ConfigurationSummary;

@RestController
@RequestMapping("/sdm-setup/pre-configure")
@ConditionalOnProperty("role.isSetup")
public class PreConfigureController {

	private static final Logger LOGGER = LoggerFactory.getLogger(PreConfigureController.class);

	private final PreConfigureService preConfigureService;

	public PreConfigureController(final PreConfigureService preConfigureService) {
		this.preConfigureService = preConfigureService;
	}

	@GetMapping("preview")
	public ConfigurationSummary previewConfigurationSummary() {

		LOGGER.debug("Received request to preview the configuration summary.");

		return preConfigureService.previewConfigurationSummary();
	}

	@PostMapping
	@ResponseStatus(value = HttpStatus.CREATED)
	public void preConfigureElectionEvent() {

		LOGGER.debug("Starting pre-configuration...");

		preConfigureService.preConfigureElectionEvent();

		LOGGER.info("The pre-configuration has been started.");
	}

}
