/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.requestcckeys;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet.toImmutableSet;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.UPLOAD_ELECTION_EVENT_CONTEXT_FAILED_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.REQUEST_CC_KEYS;
import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStepRunner;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowTask;

import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class RequestCcKeysService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestCcKeysService.class);

	private final WebClientFactory webClientFactory;
	private final RetryBackoffSpec retryBackoffSpec;
	private final WorkflowStepRunner workflowStepRunner;
	private final ControlComponentPublicKeysService controlComponentPublicKeysService;
	private final ElectionEventContextPayloadService electionEventContextPayloadService;

	public RequestCcKeysService(
			final WebClientFactory webClientFactory,
			final RetryBackoffSpec retryBackoffSpec,
			final WorkflowStepRunner workflowStepRunner,
			final ControlComponentPublicKeysService controlComponentPublicKeysService,
			final ElectionEventContextPayloadService electionEventContextPayloadService) {
		this.webClientFactory = webClientFactory;
		this.retryBackoffSpec = retryBackoffSpec;
		this.workflowStepRunner = workflowStepRunner;
		this.controlComponentPublicKeysService = controlComponentPublicKeysService;
		this.electionEventContextPayloadService = electionEventContextPayloadService;
	}

	/**
	 * Uploads the election event context and requests the Control Components public keys for an election event based on the given id.
	 *
	 * @param electionEventId identifies the election event for which the Control Component public keys are requested.
	 */
	public void requestCcKeys(final String electionEventId) {
		validateUUID(electionEventId);

		final WorkflowTask workflowTask = new WorkflowTask(
				() -> performRequestCcKeys(electionEventId),
				() -> LOGGER.info("Control Component public keys requested successfully. [electionEventId: {}]", electionEventId),
				throwable -> LOGGER.error("Control Component public keys request failed. [electionEventId: {}]", electionEventId, throwable)
		);

		workflowStepRunner.run(REQUEST_CC_KEYS, workflowTask);
	}

	void performRequestCcKeys(final String electionEventId) {
		// Upload the Election Event Context and get the Control Component public keys.
		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadService.load(electionEventId);
		final ImmutableList<ControlComponentPublicKeysPayload> controlComponentPublicKeysPayloads = webClientFactory.getWebClient(
						String.format(UPLOAD_ELECTION_EVENT_CONTEXT_FAILED_MESSAGE + "[electionEventId: %s]",
								electionEventId))
				.post()
				.uri(uriBuilder -> uriBuilder.path("api/v1/configuration/setupvoting/keygeneration/electionevent/{electionEventId}")
						.build(electionEventId))
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(electionEventContextPayload), ElectionEventContextPayload.class)
				.retrieve()
				.bodyToFlux(ControlComponentPublicKeysPayload.class)
				.retryWhen(retryBackoffSpec)
				.collect(toImmutableList())
				.block();

		checkState(controlComponentPublicKeysPayloads != null,
				"Response of the request for the Control Component public keys body is null. [electionEventId: %s]",
				electionEventId);
		LOGGER.info(
				"Successfully uploaded the election event context payload and retrieved Control Component public keys payloads. [electionEventId: {}]",
				electionEventId);

		// Check the consistency of the Control component public keys.
		checkState(controlComponentPublicKeysPayloads.size() == ControlComponentNode.ids().size(),
				"There number of Control Component public keys payloads expected is incorrect. [received: %s, expected: %s, electionEventId: %s]",
				controlComponentPublicKeysPayloads.size(), ControlComponentNode.ids().size(), electionEventId);

		final ImmutableSet<Integer> payloadNodeIds = controlComponentPublicKeysPayloads.stream()
				.map(ControlComponentPublicKeysPayload::getControlComponentPublicKeys)
				.map(ControlComponentPublicKeys::nodeId)
				.collect(toImmutableSet());

		checkState(payloadNodeIds.equals(ControlComponentNode.ids()),
				"The Control Component public keys payloads node ids do not match the expected node ids. [received: %s, expected: %s, electionEventId: %s]",
				payloadNodeIds, ControlComponentNode.ids(), electionEventId);

		controlComponentPublicKeysPayloads.forEach(payload -> checkState(payload.getElectionEventId().equals(electionEventId),
				"The controlComponentPublicKeysPayload's election event id must correspond to the election event id of the request"));
		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		controlComponentPublicKeysPayloads.forEach(payload -> checkState(payload.getEncryptionGroup().equals(encryptionGroup),
				"The controlComponentPublicKeysPayload's group must correspond to the group stored by the secure-data-manager."));

		// Persists the Control Component public keys.
		controlComponentPublicKeysPayloads.forEach(controlComponentPublicKeysService::save);
	}

}
