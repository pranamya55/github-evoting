/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.upload;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;

import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

@Repository
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class UploadSetupComponentPublicKeysRepository {

	private final WebClientFactory webClientFactory;
	private final RetryBackoffSpec retryBackoffSpec;

	public UploadSetupComponentPublicKeysRepository(
			final WebClientFactory webClientFactory,
			final RetryBackoffSpec retryBackoffSpec) {
		this.webClientFactory = webClientFactory;
		this.retryBackoffSpec = retryBackoffSpec;
	}

	/**
	 * Uploads the setup component public keys payload to the vote verification.
	 *
	 * @param setupComponentPublicKeysPayload the setup component public keys payload to upload. Must be non-null.
	 * @throws IllegalStateException if the upload was unsuccessful.
	 * @throws NullPointerException  if the setup component public keys payload is null.
	 */
	public void upload(final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload) {
		checkNotNull(setupComponentPublicKeysPayload);

		final String electionEventId = setupComponentPublicKeysPayload.getElectionEventId();
		final ResponseEntity<Void> response = webClientFactory.getWebClient(
						String.format("Request for uploading setup component public keys failed. [electionEventId: %s]", electionEventId))
				.post()
				.uri(uriBuilder -> uriBuilder.path("api/v1/processor/configuration/setupkeys/electionevent/{electionEventId}").build(electionEventId))
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(setupComponentPublicKeysPayload), SetupComponentPublicKeysPayload.class)
				.retrieve()
				.toBodilessEntity()
				.retryWhen(retryBackoffSpec)
				.block();

		checkState(checkNotNull(response).getStatusCode().is2xxSuccessful());
	}

}
