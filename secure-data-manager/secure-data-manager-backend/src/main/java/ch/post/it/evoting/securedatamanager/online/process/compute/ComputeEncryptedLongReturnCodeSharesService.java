/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.compute;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;

import reactor.core.publisher.Flux;
import reactor.util.retry.RetryBackoffSpec;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class ComputeEncryptedLongReturnCodeSharesService {

	private final WebClientFactory webClientFactory;
	private final RetryBackoffSpec retryBackoffSpec;

	public ComputeEncryptedLongReturnCodeSharesService(
			final WebClientFactory webClientFactory,
			final RetryBackoffSpec retryBackoffSpec) {
		this.webClientFactory = webClientFactory;
		this.retryBackoffSpec = retryBackoffSpec;
	}

	/**
	 * Send a Setup Component verification data list to the control components to generate the encrypted long Return Code Shares (encrypted long
	 * Choice Return Code shares and encrypted long Vote Cast Return Code shares).
	 */
	public void compute(final String electionEventId, final String verificationCardSetId,
			final ImmutableList<SetupComponentVerificationDataPayload> setupComponentVerificationDataPayloads) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkNotNull(setupComponentVerificationDataPayloads);

		setupComponentVerificationDataPayloads.forEach(setupComponentVerificationDataPayload -> {
			checkArgument(setupComponentVerificationDataPayload.getElectionEventId().equals(electionEventId));
			checkArgument(setupComponentVerificationDataPayload.getVerificationCardSetId().equals(verificationCardSetId));
		});

		final ResponseEntity<Void> response = webClientFactory.getWebClient(
						String.format("Compute unsuccessful. [electionEventId: %s, verificationCardSetId: %s]",
								electionEventId, verificationCardSetId))
				.put()
				.uri(uriBuilder -> uriBuilder.path(
								"api/v1/configuration/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}/computegenenclongcodeshares")
						.build(electionEventId, verificationCardSetId))
				.contentType(MediaType.APPLICATION_NDJSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Flux.fromIterable(setupComponentVerificationDataPayloads), SetupComponentVerificationDataPayload.class)
				.retrieve()
				.toBodilessEntity()
				.retryWhen(retryBackoffSpec)
				.block();

		checkState(checkNotNull(response).getStatusCode().is2xxSuccessful());
	}
}
