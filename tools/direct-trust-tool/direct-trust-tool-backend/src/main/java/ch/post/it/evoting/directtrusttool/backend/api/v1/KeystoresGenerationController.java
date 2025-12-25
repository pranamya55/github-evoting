/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.api.v1;

import static ch.post.it.evoting.directtrusttool.backend.api.v1.RouteConstants.BASE_PATH;
import static ch.post.it.evoting.directtrusttool.backend.session.SessionIdValidator.validateSessionId;
import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.directtrusttool.backend.process.generatekeystores.KeystorePropertiesDto;
import ch.post.it.evoting.directtrusttool.backend.process.generatekeystores.KeystoresGenerationService;

@RestController
@RequestMapping(BASE_PATH + "/key-store-generation")
public class KeystoresGenerationController {

	private final KeystoresGenerationService keystoresGenerationService;

	public KeystoresGenerationController(final KeystoresGenerationService keystoresGenerationService) {
		this.keystoresGenerationService = keystoresGenerationService;
	}

	@PostMapping(value = "{sessionId}", consumes = "application/json")
	public void generateKeystores(
			@PathVariable
			final String sessionId,
			@RequestBody
			final KeystorePropertiesDto properties) {
		validateSessionId(sessionId);
		checkNotNull(properties);
		keystoresGenerationService.generateKeystores(sessionId, properties);
	}
}
