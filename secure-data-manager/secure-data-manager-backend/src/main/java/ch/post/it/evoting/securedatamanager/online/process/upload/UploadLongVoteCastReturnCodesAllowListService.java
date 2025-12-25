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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentLVCCAllowListPayloadService;

import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class UploadLongVoteCastReturnCodesAllowListService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadLongVoteCastReturnCodesAllowListService.class);

	private final SetupComponentLVCCAllowListPayloadService setupComponentLVCCAllowListPayloadService;
	private final WebClientFactory webClientFactory;
	private final RetryBackoffSpec retryBackoffSpec;

	public UploadLongVoteCastReturnCodesAllowListService(
			final SetupComponentLVCCAllowListPayloadService setupComponentLVCCAllowListPayloadService,
			final WebClientFactory webClientFactory,
			final RetryBackoffSpec retryBackoffSpec) {
		this.setupComponentLVCCAllowListPayloadService = setupComponentLVCCAllowListPayloadService;
		this.webClientFactory = webClientFactory;
		this.retryBackoffSpec = retryBackoffSpec;
	}

	/**
	 * Uploads the setup component LVCC allow list payload corresponding to the given election event id and verification card set id to the control
	 * components through the message broker orchestrator. If the election event id is empty, the upload is not done.
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

		LOGGER.debug("Uploading setup component LVCC allow list payload... [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);

		final SetupComponentLVCCAllowListPayload setupComponentLVCCAllowListPayload = setupComponentLVCCAllowListPayloadService.load(electionEventId,
				verificationCardSetId);

		final ResponseEntity<Void> response = webClientFactory.getWebClient(
						String.format("Request for uploading long vote cast return codes allow list failed. [electionEventId: %s, verificationCardSetId: %s]",
								electionEventId, verificationCardSetId))
				.post()
				.uri(uriBuilder -> uriBuilder.path(
								"api/v1/configuration/setupvoting/longvotecastreturncodesallowlist/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}")
						.build(electionEventId, verificationCardSetId))
				.body(Mono.just(setupComponentLVCCAllowListPayload), SetupComponentLVCCAllowListPayload.class)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.toBodilessEntity()
				.retryWhen(retryBackoffSpec)
				.block();

		checkState(checkNotNull(response).getStatusCode().is2xxSuccessful());

		LOGGER.info("Successfully uploaded setup component LVCC allow list payload. [electionEventId: {}, verificationCardSetId: {}]",
				electionEventId, verificationCardSetId);
	}
}
