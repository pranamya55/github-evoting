/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.decrypt;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.tally.BallotBoxResult;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBox;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

@RestController
@RequestMapping("/sdm-tally/decrypt")
@ConditionalOnProperty("role.isTally")
public class DecryptController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DecryptController.class);

	private final DecryptService decryptService;
	private final ElectionEventService electionEventService;
	private final ElectionEventResultService electionEventResultService;

	public DecryptController(
			final DecryptService decryptService,
			final ElectionEventService electionEventService,
			final ElectionEventResultService electionEventResultService) {
		this.decryptService = decryptService;
		this.electionEventService = electionEventService;
		this.electionEventResultService = electionEventResultService;
	}

	@PostMapping
	public void decrypt(
			@RequestBody
			final DecryptInput decryptInput) {

		checkNotNull(decryptInput);

		final String electionEventId = electionEventService.findElectionEventId();

		LOGGER.debug("Received request to decrypt ballot boxes. [electionEventId: {}]", electionEventId);

		final ImmutableList<SafePasswordHolder> electoralBoardPasswords = checkNotNull(decryptInput.electoralBoardPasswords()).stream()
				.map(SafePasswordHolder::new)
				.collect(toImmutableList());
		decryptInput.electoralBoardPasswords().forEach(pw -> Arrays.fill(pw, '0'));

		decryptService.decrypt(electionEventId, decryptInput.ballotBoxIds(), electoralBoardPasswords);

		LOGGER.info("Decryption process has been started. [electionEventId: {}]", electionEventId);
	}

	/**
	 * Returns the list of all ballot downloaded boxes.
	 */
	@GetMapping(value = "ballotboxes", produces = "application/json")
	public ImmutableList<BallotBox> getBallotBoxes() {
		final String electionEventId = electionEventService.findElectionEventId();

		return decryptService.getBallotBoxes(electionEventId);
	}

	/**
	 * Returns the decrypted result of the specified ballot box.
	 */
	@GetMapping(value = "ballotboxresults/{ballotBoxId}", produces = "application/json")
	public BallotBoxResult getBallotBoxResult(
			@PathVariable("ballotBoxId")
			final String ballotBoxId
	) {
		validateUUID(ballotBoxId);
		final String electionEventId = electionEventService.findElectionEventId();

		LOGGER.debug("Received request to compute ballot box result. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		final BallotBoxResult ballotBoxResult = electionEventResultService.getBallotBoxResult(electionEventId, ballotBoxId);
		LOGGER.info("Ballot box result has been computed. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		return ballotBoxResult;
	}

}
