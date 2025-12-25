/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.mixdownload;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.DOWNLOAD_UNSUCCESSFUL_MESSAGE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.UncheckedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.tally.BallotBoxStatus;
import ch.post.it.evoting.domain.tally.MixDecryptOnlinePayload;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineRawPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentBallotBoxPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentShufflePayloadFileRepository;

import reactor.util.retry.RetryBackoffSpec;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class DownloadBallotBoxService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadBallotBoxService.class);
	private final ObjectMapper objectMapper;
	private final BallotBoxService ballotBoxService;
	private final WebClientFactory webClientFactory;
	private final RetryBackoffSpec retryBackoffSpec;
	private final ControlComponentShufflePayloadFileRepository controlComponentShufflePayloadFileRepository;
	private final ControlComponentBallotBoxPayloadFileRepository controlComponentBallotBoxPayloadFileRepository;

	public DownloadBallotBoxService(
			final ObjectMapper objectMapper,
			final BallotBoxService ballotBoxService,
			final WebClientFactory webClientFactory,
			final RetryBackoffSpec retryBackoffSpec,
			final ControlComponentShufflePayloadFileRepository controlComponentShufflePayloadFileRepository,
			final ControlComponentBallotBoxPayloadFileRepository controlComponentBallotBoxPayloadFileRepository) {
		this.objectMapper = objectMapper;
		this.ballotBoxService = ballotBoxService;
		this.webClientFactory = webClientFactory;
		this.retryBackoffSpec = retryBackoffSpec;
		this.controlComponentShufflePayloadFileRepository = controlComponentShufflePayloadFileRepository;
		this.controlComponentBallotBoxPayloadFileRepository = controlComponentBallotBoxPayloadFileRepository;
	}

	/**
	 * Downloads and persists the outputs generated when mixing the requested ballot box on the online control component nodes.
	 *
	 * @param electionEventId the election event id.
	 * @param ballotBoxId     the ballot box id.
	 * @throws IllegalArgumentException         if the ballot box is not {@link BallotBoxStatus#MIXED}.
	 * @throws UncheckedIOException             if the communication with the orchestrator fails.
	 * @throws IllegalStateException            if the response from the orchestrator was unsuccessful.
	 * @throws InvalidPayloadSignatureException if the signature of the response's content is invalid.
	 */
	public void download(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		LOGGER.info("Requesting payloads for ballot box... [ballotBoxId: {}]", ballotBoxId);

		// Check that the full ballot box has been mixed.
		if (!ballotBoxService.hasStatus(ballotBoxId, BallotBoxStatus.MIXED)) {
			throw new IllegalArgumentException(String.format("Ballot box is not mixed [ballotBoxId : %s]", ballotBoxId));
		}

		final MixDecryptOnlinePayload mixDecryptOnlinePayload = webClientFactory.getWebClient(
						String.format("%s [ballotBoxId: %s]", DOWNLOAD_UNSUCCESSFUL_MESSAGE, ballotBoxId))
				.get()
				.uri(uriBuilder -> uriBuilder.path("api/v1/tally/mixonline/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/download")
						.build(electionEventId, ballotBoxId))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(MixDecryptOnlineRawPayload.class)
				.retryWhen(retryBackoffSpec)
				.map(mixDecryptOnlineRawPayload -> {
					LOGGER.debug("Downloaded raw payloads for ballot box. [ballotBoxId: {}]", ballotBoxId);
					return MixDecryptOnlinePayload.from(mixDecryptOnlineRawPayload, objectMapper);
				})
				.block();

		LOGGER.info("Downloaded payloads for ballot box. [ballotBoxId: {}]", ballotBoxId);

		checkNotNull(mixDecryptOnlinePayload, "Downloaded content is null. [ballotBoxId: %s]", ballotBoxId);

		final ImmutableList<ControlComponentBallotBoxPayload> controlComponentBallotBoxPayloads = mixDecryptOnlinePayload.controlComponentBallotBoxPayloads();
		final ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads = mixDecryptOnlinePayload.controlComponentShufflePayloads();

		// Check consistency of ballot box ID and election event ID
		controlComponentBallotBoxPayloads.forEach(ballotBoxPayload -> checkState(ballotBoxPayload.getBallotBoxId().equals(ballotBoxId),
				"The controlComponentBallotBoxPayload's ballot box id must correspond to the ballot box id of the request"));
		controlComponentBallotBoxPayloads.forEach(ballotBoxPayload -> checkState(ballotBoxPayload.getElectionEventId().equals(electionEventId),
				"The controlComponentBallotBoxPayload's election event id must correspond to the election event id of the request"));

		controlComponentShufflePayloads.forEach(shufflePayload -> checkState(shufflePayload.getBallotBoxId().equals(ballotBoxId),
				"The controlComponentShufflePayload's ballot box id must correspond to the ballot box id of the request"));
		controlComponentShufflePayloads.forEach(shufflePayload -> checkState(shufflePayload.getElectionEventId().equals(electionEventId),
				"The controlComponentShufflePayload's election event id must correspond to the election event id of the request"));

		// Save mix net payloads.
		controlComponentBallotBoxPayloadFileRepository.saveAll(controlComponentBallotBoxPayloads);
		LOGGER.info("Confirmed encrypted votes payloads successfully stored. [electionEventId:{}, ballotBoxId:{}]", electionEventId, ballotBoxId);

		controlComponentShufflePayloads.forEach(controlComponentShufflePayloadFileRepository::savePayload);
		LOGGER.info("Control component shuffle payloads successfully stored. [electionEventId:{}, ballotBoxId:{}]", electionEventId, ballotBoxId);

		ballotBoxService.updateStatus(ballotBoxId, BallotBoxStatus.DOWNLOADED);
		LOGGER.info("Ballot box status updated. [electionEventId: {}, ballotBoxId: {}, status: {}]", electionEventId,
				ballotBoxId, BallotBoxStatus.DOWNLOADED);
	}

}
