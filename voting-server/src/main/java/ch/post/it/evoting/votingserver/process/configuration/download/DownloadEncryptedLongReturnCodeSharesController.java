/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.download;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.reactor.Box;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/configuration/")
public class DownloadEncryptedLongReturnCodeSharesController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadEncryptedLongReturnCodeSharesController.class);

	private final DownloadEncryptedLongReturnCodeSharesService downloadEncryptedLongReturnCodeSharesService;

	public DownloadEncryptedLongReturnCodeSharesController(
			final DownloadEncryptedLongReturnCodeSharesService downloadEncryptedLongReturnCodeSharesService) {
		this.downloadEncryptedLongReturnCodeSharesService = downloadEncryptedLongReturnCodeSharesService;
	}

	@PostMapping(value = "/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}/download", consumes = MediaType.APPLICATION_NDJSON_VALUE, produces = MediaType.APPLICATION_NDJSON_VALUE)
	public Flux<Box<ImmutableList<ImmutableByteArray>>> download(
			@PathVariable
			final String electionEventId,
			@PathVariable
			final String verificationCardSetId,
			@RequestBody
			final Flux<Integer> chunkIds) {

		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkNotNull(chunkIds);

		return chunkIds
				.map(chunkId -> {
					checkArgument(chunkId >= 0);
					LOGGER.debug(
							"Received request to download control component code shares payload. [electionEventId: {}, verificationCardSetId: {},  chunkId: {}]",
							electionEventId, verificationCardSetId, chunkId);

					final ImmutableList<ImmutableByteArray> controlComponentCodeSharesPayloadsBytes = downloadEncryptedLongReturnCodeSharesService.download(
							electionEventId, verificationCardSetId, chunkId);

					LOGGER.info(
							"Retrieved control component code shares payload for download. [electionEventId: {}, verificationCardSetId: {},  chunkId: {}]",
							electionEventId, verificationCardSetId, chunkId);

					return new Box<>(controlComponentCodeSharesPayloadsBytes);
				})
				.limitRate(1)
				.subscribeOn(Schedulers.parallel());
	}
}