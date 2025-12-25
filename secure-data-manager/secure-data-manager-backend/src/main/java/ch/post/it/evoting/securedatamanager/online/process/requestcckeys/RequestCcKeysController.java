/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.requestcckeys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

/**
 * The request CC keys upload end-point.
 */
@RestController
@RequestMapping("/sdm-online/request-cc-keys")
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class RequestCcKeysController {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestCcKeysController.class);
	private final ElectionEventService electionEventService;
	private final RequestCcKeysService requestCcKeysService;

	public RequestCcKeysController(
			final ElectionEventService electionEventService,
			final RequestCcKeysService requestCcKeysService) {
		this.electionEventService = electionEventService;
		this.requestCcKeysService = requestCcKeysService;
	}

	@PostMapping(produces = "application/json")
	public void requestCcKeys() {
		final String electionEventId = electionEventService.findElectionEventId();

		requestCcKeysService.requestCcKeys(electionEventId);

		LOGGER.info("CC Keys successfully requested.");
	}

}
