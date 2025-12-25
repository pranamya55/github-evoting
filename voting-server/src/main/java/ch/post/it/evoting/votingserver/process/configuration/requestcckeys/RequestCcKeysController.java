/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.requestcckeys;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceContext;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceService;
import ch.post.it.evoting.votingserver.process.ElectionEventContextService;

@RestController
@RequestMapping("api/v1/configuration/setupvoting")
public class RequestCcKeysController {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestCcKeysController.class);

	private final RequestCcKeysService requestCcKeysService;
	private final ElectionEventContextService electionEventContextService;
	private final IdempotenceService<IdempotenceContext> idempotenceService;

	public RequestCcKeysController(
			final RequestCcKeysService requestCcKeysService,
			final ElectionEventContextService electionEventContextService,
			final IdempotenceService<IdempotenceContext> idempotenceService) {
		this.idempotenceService = idempotenceService;
		this.requestCcKeysService = requestCcKeysService;
		this.electionEventContextService = electionEventContextService;
	}

	@PostMapping("keygeneration/electionevent/{electionEventId}")
	public ImmutableList<ControlComponentPublicKeysPayload> requestCcKeys(
			@PathVariable
			final String electionEventId,
			@RequestBody
			final ElectionEventContextPayload electionEventContextPayload) {

		validateUUID(electionEventId);
		checkNotNull(electionEventContextPayload);
		checkArgument(electionEventId.equals(electionEventContextPayload.getElectionEventContext().electionEventId()));
		electionEventContextService.verifyPayloadSignature(electionEventContextPayload);

		LOGGER.debug("Saving the election event context. [contextId: {}]", electionEventId);
		idempotenceService.execute(IdempotenceContext.SAVE_ELECTION_EVENT_CONTEXT, electionEventId, electionEventContextPayload,
				() -> electionEventContextService.saveElectionEventContext(electionEventContextPayload));

		LOGGER.debug("Requesting control components Key Generation. [contextId: {}]", electionEventId);

		final String correlationId = requestCcKeysService.onRequest(electionEventContextPayload);
		LOGGER.info("Requested control components Key Generation. [contextId: {}, correlationId: {}]", electionEventId, correlationId);

		final ImmutableList<ControlComponentPublicKeysPayload> controlComponentPublicKeysPayloads = requestCcKeysService.waitForResponse(
				correlationId);
		LOGGER.info("Successfully saved and uploaded the election event context. [electionEventId: {}]", electionEventId);

		return controlComponentPublicKeysPayloads;
	}

}
