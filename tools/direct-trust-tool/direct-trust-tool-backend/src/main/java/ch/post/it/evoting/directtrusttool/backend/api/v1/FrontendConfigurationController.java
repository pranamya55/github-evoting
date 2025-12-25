/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.api.v1;

import static ch.post.it.evoting.directtrusttool.backend.api.v1.RouteConstants.BASE_PATH;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.directtrusttool.backend.process.configurefrontend.FrontendConfigurationDto;
import ch.post.it.evoting.directtrusttool.backend.process.configurefrontend.FrontendConfigurationService;

@RestController
@RequestMapping(BASE_PATH + "/configuration")
public class FrontendConfigurationController {

	private final FrontendConfigurationService frontendConfigurationService;

	public FrontendConfigurationController(final FrontendConfigurationService frontendConfigurationService) {
		this.frontendConfigurationService = frontendConfigurationService;
	}

	@GetMapping(produces = "application/json")
	public FrontendConfigurationDto getFrontendConfiguration() {
		return frontendConfigurationService.getFrontendConfiguration();
	}
}
