/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.votingcardmanagement;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validatePartialUUID;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.votingserver.process.Constants.PARAMETER_VALUE_ELECTION_EVENT_ID;
import static ch.post.it.evoting.votingserver.process.Constants.VOTING_CARD_ID;
import static ch.post.it.evoting.votingserver.process.VerificationCardService.MIN_PARTIAL_UUID_LENGTH;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.votingserver.process.ElectionEventContextService;
import ch.post.it.evoting.votingserver.process.ElectionEventService;
import ch.post.it.evoting.votingserver.process.VerificationCardService;

@RestController
@RequestMapping("api/v1/votingcardmanager")
public class VotingCardManagerController {

	private static final Logger LOGGER = LoggerFactory.getLogger(VotingCardManagerController.class);

	private final ElectionEventService electionEventService;
	private final ElectionEventContextService electionEventContextService;
	private final VerificationCardService verificationCardService;

	public VotingCardManagerController(
			final ElectionEventService electionEventService,
			final VerificationCardService verificationCardService,
			final ElectionEventContextService electionEventContextService) {
		this.verificationCardService = verificationCardService;
		this.electionEventService = electionEventService;
		this.electionEventContextService = electionEventContextService;
	}

	/**
	 * @param votingCardId voting card id. Must be non-null and a valid UUID.
	 * @return a VotingCardDto corresponding to the id searched.
	 */
	@GetMapping("votingcards/{" + VOTING_CARD_ID + "}")
	public VotingCardDto getVotingCard(
			@PathVariable(VOTING_CARD_ID)
			final String votingCardId) {
		validateUUID(votingCardId);

		LOGGER.debug("Received request to get voting card. [votingCardId: {}]", votingCardId);

		return verificationCardService.getVotingCard(votingCardId);
	}

	/**
	 * @param partialVotingCardId partial voting card id. Must be non-null and a valid partial UUID with minimal length of
	 *                            {@value VerificationCardService#MIN_PARTIAL_UUID_LENGTH}.
	 * @return a list of VotingCardDto corresponding to the partial voting id searched.
	 */
	@GetMapping(value = "votingcards/")
	public VotingCardSearchDto searchVotingCards(
			@RequestParam("partialVotingCardId")
			final String partialVotingCardId) {
		validatePartialUUID(partialVotingCardId, MIN_PARTIAL_UUID_LENGTH);

		LOGGER.debug("Received request to search voting card. [partialVotingCardId: {}]", partialVotingCardId);

		return verificationCardService.searchVotingCard(partialVotingCardId);
	}

	/**
	 * Block the specified voting card.
	 *
	 * @param votingCardId the voting card id. Must be non-null and a valid UUID.
	 */
	@PutMapping("votingcards/{" + VOTING_CARD_ID + "}/block")
	public void blockVotingCard(
			@PathVariable(VOTING_CARD_ID)
			final String votingCardId) {
		validateUUID(votingCardId);

		LOGGER.debug("Received request to block a voting card. [votingCardId: {}]", votingCardId);

		verificationCardService.blockVotingCard(votingCardId);

		LOGGER.info("Voting card blocked. [votingCardId: {}]", votingCardId);
	}

	/**
	 * Lists all used voting cards for a given election event id.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @param usageDateTime   the date and time of usage from which to list the used voting cards. Optional.
	 * @return The list of used voting cards for a given election event id and, if provided, from a given date and time.
	 */
	@GetMapping(value = "electionevents/{" + PARAMETER_VALUE_ELECTION_EVENT_ID + "}/votingcards/used")
	public ImmutableList<UsedVotingCardDto> getUsedVotingCardsByElectionEventId(
			@PathVariable(PARAMETER_VALUE_ELECTION_EVENT_ID)
			final String electionEventId,
			@RequestParam(name = "from", required = false)
			final LocalDateTime usageDateTime) {
		validateUUID(electionEventId);

		if (usageDateTime != null) {
			LOGGER.debug("Received request to get used voting cards. [electionEventId: {}, usageDateTime: {}]", electionEventId, usageDateTime);
		} else {
			LOGGER.debug("Received request to get used voting cards. [electionEventId: {}]", electionEventId);
		}

		if (!electionEventService.exists(electionEventId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					String.format("The election event id does not correspond to any existing event. [electionEventId: %s]", electionEventId));
		}

		return verificationCardService.getUsedVotingCardsByElectionEventIdAndSinceUsageDateTime(electionEventId, usageDateTime);
	}

	/**
	 * Lists all election events.
	 *
	 * @return The list of all election events.
	 */
	@GetMapping(value = "electionevents/")
	public ImmutableList<ElectionEventDto> getAllElectionEvents() {

		LOGGER.debug("Received request to get all election events.");

		return electionEventContextService.retrieveAll();
	}
}
