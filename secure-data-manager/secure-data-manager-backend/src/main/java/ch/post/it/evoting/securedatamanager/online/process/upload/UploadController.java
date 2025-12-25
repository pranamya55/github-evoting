/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.upload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep;

/**
 * The configuration upload end-point.
 */
@RestController
@RequestMapping("/sdm-online/upload")
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class UploadController {
	private static final Logger LOGGER = LoggerFactory.getLogger(UploadController.class);

	private final UploadService uploadService;
	private final ElectionEventService electionEventService;

	public UploadController(
			final UploadService uploadService,
			final ElectionEventService electionEventService) {
		this.uploadService = uploadService;
		this.electionEventService = electionEventService;
	}

	@PostMapping()
	public void upload(
			@RequestBody
			final int day) {
		final String electionEventId = electionEventService.findElectionEventId();

		final WorkflowStep uploadConfigurationStep = WorkflowStep.getUploadConfigurationStep(day);

		LOGGER.debug("Received request to upload configuration. [electionEventId: {}]", electionEventId);

		uploadService.upload(electionEventId, uploadConfigurationStep);

		LOGGER.info("The upload has been started. [electionEventId: {}]", electionEventId);
	}
}
