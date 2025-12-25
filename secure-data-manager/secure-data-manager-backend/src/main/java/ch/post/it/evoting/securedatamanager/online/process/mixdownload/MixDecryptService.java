/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.mixdownload;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.BALLOT_BOX_NOT_CLOSED_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.START_ONLINE_MIXING_FAILED_MESSAGE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.UncheckedIOException;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.domain.tally.BallotBoxStatus;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;

import reactor.util.retry.RetryBackoffSpec;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class MixDecryptService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixDecryptService.class);
	private final BallotBoxService ballotBoxService;
	private final WebClientFactory webClientFactory;
	private final RetryBackoffSpec retryBackoffSpec;

	public MixDecryptService(
			final BallotBoxService ballotBoxService,
			final WebClientFactory webClientFactory,
			final RetryBackoffSpec retryBackoffSpec) {
		this.ballotBoxService = ballotBoxService;
		this.webClientFactory = webClientFactory;
		this.retryBackoffSpec = retryBackoffSpec;
	}

	/**
	 * Requests the control-components to mix the ballot box with given id {@code ballotBoxId}.
	 *
	 * @param electionEventId the election event id of the ballot box.
	 * @param ballotBoxId     the id of the ballot box to mix.
	 * @throws UncheckedIOException if the communication with the orchestrator fails.
	 */
	public void mix(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		final boolean isTestBallotBox = ballotBoxService.isTestBallotBox(ballotBoxId);
		final LocalDateTime ballotBoxFinishDate = ballotBoxService.getFinishTime(ballotBoxId);

		final int gracePeriod = ballotBoxService.getGracePeriod(ballotBoxId);
		final boolean beforeGracePeriod = LocalDateTimeUtils.now().isBefore(ballotBoxFinishDate.plusSeconds(gracePeriod));

		if (!isTestBallotBox && beforeGracePeriod) {
			LOGGER.error("The ballot box is not yet closed and cannot be mixed. [electionEventId: {}, ballotBoxId: {}]", electionEventId,
					ballotBoxId);
			final String errorMessage = String.format(BALLOT_BOX_NOT_CLOSED_MESSAGE + "[electionEventId: %s, ballotBoxId: %s]",
					electionEventId, ballotBoxId);
			throw new IllegalStateException(errorMessage);
		}

		final ResponseEntity<Void> response = webClientFactory.getWebClient(
						String.format(START_ONLINE_MIXING_FAILED_MESSAGE + "[electionEventId: %s, ballotBoxId: %s]", electionEventId, ballotBoxId))
				.put()
				.uri(uriBuilder -> uriBuilder.path("api/v1/tally/mixonline/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/mix")
						.build(electionEventId, ballotBoxId))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.toBodilessEntity()
				.retryWhen(retryBackoffSpec)
				.block();

		checkState(checkNotNull(response).getStatusCode().is2xxSuccessful());

		ballotBoxService.updateStatus(ballotBoxId, BallotBoxStatus.MIXING);
		LOGGER.info("Ballot box status updated. [ballotBoxId: {}, status: {}", ballotBoxId, BallotBoxStatus.MIXING);

	}

	/**
	 * Gets the status of the ballot box with id {@code ballotBoxId} from the control-components.
	 *
	 * @param electionEventId the election event id of the ballot box.
	 * @param ballotBoxId     the id of the ballot box to get the status.
	 * @return the status of the ballot box.
	 * @throws UncheckedIOException  if the communication with the orchestrator fails.
	 * @throws IllegalStateException if the response from the orchestrator was unsuccessful.
	 */
	public BallotBoxStatus getMixingStatus(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		final BallotBoxStatus ballotBoxStatus = webClientFactory.getWebClient(
						String.format("Get online status unsuccessful. [ballotBoxId: %s]", ballotBoxId))
				.get()
				.uri(uriBuilder -> uriBuilder.path("api/v1/tally/mixonline/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/status")
						.build(electionEventId, ballotBoxId))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(BallotBoxStatus.class)
				.block();

		checkNotNull(ballotBoxStatus, "Get online status returned a null response. [ballotBoxId: %s]", ballotBoxId);

		return ballotBoxStatus;
	}

}
