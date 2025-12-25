/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.mixdownload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBox;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

@RestController
@RequestMapping("/sdm-online/mix-download")
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class MixDownloadController {
	private static final Logger LOGGER = LoggerFactory.getLogger(MixDownloadController.class);

	private final MixDownloadService mixDownloadService;

	private final BallotBoxService ballotBoxService;
	private final ElectionEventService electionEventService;

	public MixDownloadController(
			final MixDownloadService mixDownloadService,
			final BallotBoxService ballotBoxService,
			final ElectionEventService electionEventService) {
		this.mixDownloadService = mixDownloadService;
		this.ballotBoxService = ballotBoxService;
		this.electionEventService = electionEventService;
	}

	@PostMapping()
	public void mixAndDownload(
			@RequestBody
			final ImmutableList<String> ballotBoxIds) {
		final String electionEventId = electionEventService.findElectionEventId();

		LOGGER.debug("Starting mix & download. [electionEventId: {}]", electionEventId);

		mixDownloadService.mixAndDownload(electionEventId, ballotBoxIds);

		LOGGER.info("The mix & download has been started. [electionEventId: {}]", electionEventId);
	}

	/**
	 * Returns the list of all ballot boxes.
	 */
	@GetMapping(value = "ballotboxes", produces = "application/json")
	public ImmutableList<BallotBox> getBallotBoxes() {
		final String electionEventId = electionEventService.findElectionEventId();

		return ballotBoxService.getBallotBoxes(electionEventId);
	}

}
