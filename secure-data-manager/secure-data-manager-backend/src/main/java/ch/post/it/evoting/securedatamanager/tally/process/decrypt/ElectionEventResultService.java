/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.decrypt;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.domain.tally.BallotBoxStatus;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.tally.BallotBoxResult;
import ch.post.it.evoting.evotinglibraries.domain.tally.ElectionEventResultUtils;
import ch.post.it.evoting.evotinglibraries.domain.tally.TallyComponentVotesPayload;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.tally.process.TallyComponentVotesService;

@Service
@ConditionalOnProperty("role.isTally")
public class ElectionEventResultService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectionEventResultService.class);

	private final BallotBoxService ballotBoxService;
	private final TallyComponentVotesService tallyComponentVotesService;
	private final ElectionEventContextPayloadService electionEventContextPayloadService;

	public ElectionEventResultService(
			final BallotBoxService ballotBoxService,
			final TallyComponentVotesService tallyComponentVotesService,
			final ElectionEventContextPayloadService electionEventContextPayloadService) {
		this.ballotBoxService = ballotBoxService;
		this.tallyComponentVotesService = tallyComponentVotesService;
		this.electionEventContextPayloadService = electionEventContextPayloadService;
	}

	/**
	 * Retrieves the votes, elections and write-ins results for a given ballot box.
	 */
	public BallotBoxResult getBallotBoxResult(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		LOGGER.debug("Retrieving ballot box result. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		// Election event context payload.
		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadService.load(electionEventId);

		// Tally component votes' payload.
		final TallyComponentVotesPayload tallyComponentVotesPayload = tallyComponentVotesService.load(electionEventId, ballotBoxId);

		// Check if the ballot box is decrypted.
		final BallotBoxStatus ballotBoxStatus = ballotBoxService.getBallotBoxStatus(ballotBoxId);
		if (BallotBoxStatus.DECRYPTED.equals(ballotBoxStatus)) {
			LOGGER.info("Returning ballot box result. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);
			return ElectionEventResultUtils.getBallotBoxResult(electionEventContextPayload, tallyComponentVotesPayload);
		}

		LOGGER.warn("Ballot box result is not available as ballot box is not decrypted. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);
		return null;
	}
}
