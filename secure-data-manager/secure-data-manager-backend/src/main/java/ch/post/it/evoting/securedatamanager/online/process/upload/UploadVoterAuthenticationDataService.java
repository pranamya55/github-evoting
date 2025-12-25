/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.upload;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVoterAuthenticationPayloadFileRepository;

import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class UploadVoterAuthenticationDataService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadVoterAuthenticationDataService.class);

	private final WebClientFactory webClientFactory;
	private final RetryBackoffSpec retryBackoffSpec;
	private final SetupComponentVoterAuthenticationPayloadFileRepository setupComponentVoterAuthenticationPayloadFileRepository;

	public UploadVoterAuthenticationDataService(
			final WebClientFactory webClientFactory,
			final RetryBackoffSpec retryBackoffSpec,
			final SetupComponentVoterAuthenticationPayloadFileRepository setupComponentVoterAuthenticationPayloadFileRepository) {
		this.webClientFactory = webClientFactory;
		this.retryBackoffSpec = retryBackoffSpec;
		this.setupComponentVoterAuthenticationPayloadFileRepository = setupComponentVoterAuthenticationPayloadFileRepository;
	}

	/**
	 * Uploads the setup component voter authentication data payload corresponding to the given election event id and verification card set id to the
	 * control components through the message broker orchestrator. If the election event id is empty, the upload is not done.
	 *
	 * @param electionEventId       the election event id. Must be non-null. If the election event id is not empty, it must be a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if {@code electionEventId} or {@code verificationCardSetId} is null.
	 * @throws FailedValidationException if {@code electionEventId} is not empty and not a valid UUID or if {@code verificationCardSetId} is not a
	 *                                   valid UUID.
	 */
	public void upload(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final SetupComponentVoterAuthenticationDataPayload setupComponentVoterAuthenticationDataPayload = load(electionEventId,
				verificationCardSetId);

		final ResponseEntity<Void> response = webClientFactory.getWebClient(
						String.format("Request for uploading voter authentication data payloads failed. [electionEventId: %s, verificationCardSetId: %s]",
								electionEventId, verificationCardSetId))
				.post()
				.uri(uriBuilder -> uriBuilder.path(
								"api/v1/processor/configuration/setupvoting/voterauthenticationdata/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}")
						.build(electionEventId, verificationCardSetId))
				.body(Mono.just(setupComponentVoterAuthenticationDataPayload), setupComponentVoterAuthenticationDataPayload.getClass())
				.retrieve()
				.toBodilessEntity()
				.retryWhen(retryBackoffSpec)
				.block();

		checkState(checkNotNull(response).getStatusCode().is2xxSuccessful());

		LOGGER.info("Successfully uploaded voter authentication data payload. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);
	}

	/**
	 * Loads the setup component voter authentication data payload for the given election event id and verification card set id.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return the setup component voter authentication payload for this {@code electionEventId} and {@code verificationCardSetId}.
	 * @throws NullPointerException      if {@code electionEventId} or {@code verificationCardSetId} is null.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is not a valid UUID.
	 */
	private SetupComponentVoterAuthenticationDataPayload load(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		return setupComponentVoterAuthenticationPayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(electionEventId,
						verificationCardSetId)
				.orElseThrow(() -> new IllegalStateException(String.format(
						"Requested setup component voter authentication payload is not present. [electionEventId: %s, verificationCardSetId: %s]",
						electionEventId, verificationCardSetId)));
	}
}
