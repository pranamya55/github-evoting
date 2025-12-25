/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.download;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

@RestController
@RequestMapping("/sdm-online/download")
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class DownloadController {
	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadController.class);

	private final DownloadService downloadService;
	private final ElectionEventService electionEventService;

	public DownloadController(
			final DownloadService downloadService,
			final ElectionEventService electionEventService) {
		this.downloadService = downloadService;
		this.electionEventService = electionEventService;
	}

	@PostMapping()
	public void download() {
		final String electionEventId = electionEventService.findElectionEventId();

		LOGGER.debug("Received request to download configuration. [electionEventId: {}]", electionEventId);

		downloadService.download(electionEventId);

		LOGGER.info("The download has been started. [electionEventId: {}]", electionEventId);
	}

}
