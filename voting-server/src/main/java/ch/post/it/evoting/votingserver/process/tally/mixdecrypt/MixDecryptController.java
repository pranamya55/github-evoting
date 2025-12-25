/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.tally.mixdecrypt;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.domain.tally.BallotBoxStatus;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineRawPayload;

@RestController
@RequestMapping("/api/v1/tally/")
public class MixDecryptController {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixDecryptController.class);

	private final MixDecryptOnlinePayloadService mixDecryptOnlinePayloadService;
	private final GetMixnetInitialCiphertextsService getMixnetInitialCiphertextsService;

	public MixDecryptController(
			final MixDecryptOnlinePayloadService mixDecryptOnlinePayloadService,
			final GetMixnetInitialCiphertextsService getMixnetInitialCiphertextsService) {
		this.mixDecryptOnlinePayloadService = mixDecryptOnlinePayloadService;
		this.getMixnetInitialCiphertextsService = getMixnetInitialCiphertextsService;
	}

	/**
	 * Start the online mixing of the given ballot box.
	 */
	@PutMapping("/mixonline/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/mix")
	public ResponseEntity<Void> mix(
			@PathVariable
			final String electionEventId,
			@PathVariable
			final String ballotBoxId) {

		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		LOGGER.info("Received mix online request. [electionEventId: {}, ballotBoxId:{}]", electionEventId, ballotBoxId);

		getMixnetInitialCiphertextsService.onRequest(electionEventId, ballotBoxId);
		return new ResponseEntity<>(HttpStatus.ACCEPTED);
	}

	/**
	 * Get a MixDecryptOnlineRawPayload if the mixing is finished.
	 */
	@GetMapping("/mixonline/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/download")
	public ResponseEntity<MixDecryptOnlineRawPayload> download(
			@PathVariable
			final String electionEventId,
			@PathVariable
			final String ballotBoxId) {

		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		LOGGER.info("Downloading MixDecryptOnlineRawPayload. [electionEventId: {}, ballotBoxId:{}]", electionEventId, ballotBoxId);

		final ResponseEntity<MixDecryptOnlineRawPayload> mixDecryptOnlineRawPayloadResponseEntity = mixDecryptOnlinePayloadService.getMixDecryptOnlineRawPayload(
						electionEventId, ballotBoxId)
				.map(mixDecryptOnlineRawPayload -> new ResponseEntity<>(mixDecryptOnlineRawPayload, HttpStatus.OK))
				.orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));

		LOGGER.info("Download MixDecryptOnlineRawPayload completed. [electionEventId: {}, ballotBoxId:{}, status: {}]", electionEventId, ballotBoxId,
				mixDecryptOnlineRawPayloadResponseEntity.getStatusCode());

		return mixDecryptOnlineRawPayloadResponseEntity;
	}

	/**
	 * Check the status of the online mixing for the given ballot box. Statuses are defined by {@code BallotBoxStatus}.
	 */
	@GetMapping(value = "/mixonline/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<BallotBoxStatus> getMixingStatus(
			@PathVariable
			final String electionEventId,
			@PathVariable
			final String ballotBoxId) {

		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		LOGGER.info("Status MixDecryptOnline [electionEventId: {}, ballotBoxId:{}]", electionEventId, ballotBoxId);

		final BallotBoxStatus status = switch (mixDecryptOnlinePayloadService.countMixDecryptOnlinePayloads(electionEventId, ballotBoxId)) {
			case 0 -> BallotBoxStatus.MIXING_NOT_STARTED;
			case 1, 2, 3 -> BallotBoxStatus.MIXING;
			case 4 -> BallotBoxStatus.MIXED;
			default -> BallotBoxStatus.MIXING_ERROR;
		};

		return new ResponseEntity<>(status, HttpStatus.OK);
	}
}
