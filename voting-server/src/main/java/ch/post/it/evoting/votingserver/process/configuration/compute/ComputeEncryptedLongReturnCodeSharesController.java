/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.compute;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.domain.configuration.setupvoting.ComputingStatus;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/configuration/")
public class ComputeEncryptedLongReturnCodeSharesController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ComputeEncryptedLongReturnCodeSharesController.class);

	private final ComputeEncryptedLongReturnCodeSharesService computeEncryptedLongReturnCodeSharesService;

	public ComputeEncryptedLongReturnCodeSharesController(
			final ComputeEncryptedLongReturnCodeSharesService computeEncryptedLongReturnCodeSharesService) {
		this.computeEncryptedLongReturnCodeSharesService = computeEncryptedLongReturnCodeSharesService;
	}

	@PutMapping(value = "/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}/computegenenclongcodeshares", consumes = MediaType.APPLICATION_NDJSON_VALUE)
	public Mono<ResponseEntity<Void>> compute(
			@PathVariable
			final String electionEventId,
			@PathVariable
			final String verificationCardSetId,
			@RequestBody
			final Flux<SetupComponentVerificationDataPayload> setupComponentVerificationDataPayloads) {

		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkNotNull(setupComponentVerificationDataPayloads);

		return setupComponentVerificationDataPayloads
				.publishOn(Schedulers.boundedElastic())
				.doOnNext(setupComponentVerificationDataPayload -> {
					checkNotNull(setupComponentVerificationDataPayload);
					checkArgument(setupComponentVerificationDataPayload.getElectionEventId().equals(electionEventId));
					checkArgument(setupComponentVerificationDataPayload.getVerificationCardSetId().equals(verificationCardSetId));
					final int chunkId = setupComponentVerificationDataPayload.getChunkId();
					checkArgument(chunkId >= 0);

					LOGGER.debug("Received request to generate EncLongCodeShares. [electionEventId: {}, verificationCardSetId: {},  chunkId: {}]",
							electionEventId, verificationCardSetId, chunkId);

					// Idempotence implemented in the onRequest method.
					computeEncryptedLongReturnCodeSharesService.onRequest(setupComponentVerificationDataPayload);

					LOGGER.info("Broadcasted requests to generate EncLongCodeShares. [electionEventId: {}, verificationCardSetId: {},  chunkId: {}]",
							electionEventId, verificationCardSetId, chunkId);
				})
				.then(Mono.just(new ResponseEntity<>(HttpStatus.CREATED)));
	}

	@GetMapping(value = "/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}/chunkcount/{chunkCount}/status", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ComputingStatus> getComputingStatus(
			@PathVariable
			final String electionEventId,
			@PathVariable
			final String verificationCardSetId,
			@PathVariable
			final int chunkCount) {

		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkArgument(chunkCount >= 0);

		LOGGER.debug(
				"Received request to check EncLongCodeShares generation status. [electionEventId: {}, verificationCardSetId: {},  chunkCount: {}]",
				electionEventId, verificationCardSetId, chunkCount);

		final ComputingStatus computingStatus = computeEncryptedLongReturnCodeSharesService.getComputingStatus(electionEventId,
				verificationCardSetId, chunkCount);

		LOGGER.info("Checked computing status. [electionEventId: {}, verificationCardSetId: {},  chunkCount: {}, status: {}]", electionEventId,
				verificationCardSetId, chunkCount, computingStatus);

		return new ResponseEntity<>(computingStatus, HttpStatus.OK);
	}
}
