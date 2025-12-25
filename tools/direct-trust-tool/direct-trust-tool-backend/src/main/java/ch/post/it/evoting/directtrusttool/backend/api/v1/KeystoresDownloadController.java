/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.api.v1;

import static ch.post.it.evoting.directtrusttool.backend.api.v1.RouteConstants.BASE_PATH;
import static ch.post.it.evoting.directtrusttool.backend.session.SessionIdValidator.validateSessionId;

import java.util.Base64;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.directtrusttool.backend.process.NameService;
import ch.post.it.evoting.directtrusttool.backend.process.downloadkeystores.KeystoresDownloadDto;
import ch.post.it.evoting.directtrusttool.backend.process.downloadkeystores.KeystoresDownloadService;

@RestController
@RequestMapping(BASE_PATH + "/key-store-download")
public class KeystoresDownloadController {

	private final KeystoresDownloadService keystoresDownloadService;
	private final NameService nameService;

	public KeystoresDownloadController(final KeystoresDownloadService keystoresDownloadService, final NameService nameService) {
		this.keystoresDownloadService = keystoresDownloadService;
		this.nameService = nameService;
	}

	@GetMapping(value = "{sessionId}", produces = "application/json")
	public KeystoresDownloadDto downloadKeystores(
			@PathVariable
			final String sessionId) {
		validateSessionId(sessionId);
		return new KeystoresDownloadDto(
				nameService.getArchiveName(sessionId),
				new String(Base64.getEncoder().encode(keystoresDownloadService.downloadKeystores(sessionId).elements()))
		);
	}
}
