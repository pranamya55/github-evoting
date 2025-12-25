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

import ch.post.it.evoting.domain.configuration.SetupComponentVerificationCardKeystoresPayload;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;

import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

@Repository
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class UploadSetupComponentVerificationCardKeystoresRepository {

	private final WebClientFactory webClientFactory;
	private final RetryBackoffSpec retryBackoffSpec;

	public UploadSetupComponentVerificationCardKeystoresRepository(
			final WebClientFactory webClientFactory,
			final RetryBackoffSpec retryBackoffSpec) {
		this.webClientFactory = webClientFactory;
		this.retryBackoffSpec = retryBackoffSpec;
	}

	/**
	 * Uploads the setup component verification card keystores payload to the vote verification.
	 *
	 * @param setupComponentVerificationCardKeystoresPayload the {@link SetupComponentVerificationCardKeystoresPayload} to upload. Must be non-null.
	 * @throws NullPointerException if the input is null.
	 */
	public void upload(final SetupComponentVerificationCardKeystoresPayload setupComponentVerificationCardKeystoresPayload) {

		checkNotNull(setupComponentVerificationCardKeystoresPayload);

		final String electionEventId = setupComponentVerificationCardKeystoresPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentVerificationCardKeystoresPayload.getVerificationCardSetId();

		final ResponseEntity<Void> response = webClientFactory.getWebClient(
						String.format("Request for uploading setup component verification card keystores payload failed. "
								+ "[electionEventId: %s, verificationCardSetId: %s]", electionEventId, verificationCardSetId))
				.post()
				.uri(uriBuilder -> uriBuilder.path(
								"api/v1/processor/configuration/setupcomponentverificationcardkeystores/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}")
						.build(electionEventId, verificationCardSetId))
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(setupComponentVerificationCardKeystoresPayload), SetupComponentVerificationCardKeystoresPayload.class)
				.retrieve()
				.toBodilessEntity()
				.retryWhen(retryBackoffSpec)
				.block();

		checkState(checkNotNull(response).getStatusCode().is2xxSuccessful());
	}

}
