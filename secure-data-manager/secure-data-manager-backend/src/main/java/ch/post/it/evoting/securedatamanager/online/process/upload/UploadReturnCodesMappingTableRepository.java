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
import org.springframework.stereotype.Repository;

import ch.post.it.evoting.domain.configuration.SetupComponentCMTablePayload;
import ch.post.it.evoting.domain.configuration.SetupComponentCMTablePayloadChunks;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;

import reactor.core.publisher.Flux;
import reactor.util.retry.RetryBackoffSpec;

@Repository
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class UploadReturnCodesMappingTableRepository {
	private static final Logger LOGGER = LoggerFactory.getLogger(UploadReturnCodesMappingTableRepository.class);

	private final WebClientFactory webClientFactory;
	private final RetryBackoffSpec retryBackoffSpec;

	public UploadReturnCodesMappingTableRepository(
			final WebClientFactory webClientFactory,
			final RetryBackoffSpec retryBackoffSpec) {
		this.webClientFactory = webClientFactory;
		this.retryBackoffSpec = retryBackoffSpec;
	}

	/**
	 * Uploads the setup component CMTable payload to the vote verification.
	 *
	 * @param electionEventId                    the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId              the verification card set id. Must be non-null and a valid UUID.
	 * @param setupComponentCMTablePayloadChunks the list of {@link SetupComponentCMTablePayload} to upload. Must be non-null.
	 * @throws NullPointerException      if any of the inputs is null.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is invalid.
	 */
	public void upload(final String electionEventId, final String verificationCardSetId,
			final SetupComponentCMTablePayloadChunks setupComponentCMTablePayloadChunks) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkNotNull(setupComponentCMTablePayloadChunks);

		LOGGER.debug("Uploading CMTable chunk. [electionEventId: {}, verificationCardSetId: {}]", electionEventId, verificationCardSetId);

		final ResponseEntity<Void> response = webClientFactory.getWebClient(String.format(
						"Request for uploading setup component CMTable payloads failed. [electionEventId: %s, verificationCardSetId: %s]",
						electionEventId, verificationCardSetId))
				.post()
				.uri(uriBuilder -> uriBuilder.path(
								"api/v1/processor/configuration/returncodesmappingtable/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}")
						.build(electionEventId, verificationCardSetId))
				.contentType(MediaType.APPLICATION_NDJSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Flux.fromIterable(setupComponentCMTablePayloadChunks.payloads()), SetupComponentCMTablePayload.class)
				.retrieve()
				.toBodilessEntity()
				.retryWhen(retryBackoffSpec)
				.block();

		checkState(checkNotNull(response).getStatusCode().is2xxSuccessful());

		LOGGER.debug("Successfully uploaded setup component CMTable payloads. [electionEventId: {}, verificationCardSetId: {}, chunkCount: {}]",
				electionEventId, verificationCardSetId, setupComponentCMTablePayloadChunks.getChunkCount());
	}

}
