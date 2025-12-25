/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.upload;

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

import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceContext;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceService;
import ch.post.it.evoting.votingserver.process.Constants;
import ch.post.it.evoting.votingserver.process.SetupComponentPublicKeysService;

@RestController
@RequestMapping("api/v1/processor/configuration")
public class UploadSetupComponentPublicKeysController {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadSetupComponentPublicKeysController.class);

	private final IdempotenceService<IdempotenceContext> idempotenceService;
	private final SetupComponentPublicKeysService setupComponentPublicKeysService;

	public UploadSetupComponentPublicKeysController(
			final IdempotenceService<IdempotenceContext> idempotenceService,
			final SetupComponentPublicKeysService setupComponentPublicKeysService) {
		this.idempotenceService = idempotenceService;
		this.setupComponentPublicKeysService = setupComponentPublicKeysService;
	}

	@PostMapping("setupkeys/electionevent/{electionEventId}")
	public void upload(
			@PathVariable(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
			final String electionEventId,
			@RequestBody
			final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload) {

		validateUUID(electionEventId);
		checkNotNull(setupComponentPublicKeysPayload);
		checkArgument(electionEventId.equals(setupComponentPublicKeysPayload.getElectionEventId()));
		setupComponentPublicKeysService.verifyPayloadSignature(setupComponentPublicKeysPayload);

		LOGGER.debug("Saving the setup component public keys. [electionEventId: {}]", electionEventId);
		idempotenceService.execute(IdempotenceContext.SAVE_SETUP_COMPONENT_PUBLIC_KEYS, electionEventId, setupComponentPublicKeysPayload,
				() -> setupComponentPublicKeysService.save(setupComponentPublicKeysPayload));

		final String correlationId = setupComponentPublicKeysService.onRequest(setupComponentPublicKeysPayload);
		LOGGER.info("Sent the setup component public keys to the Control Components. [electionEventId: {}, correlationId: {}]", electionEventId,
				correlationId);

		setupComponentPublicKeysService.waitForResponse(correlationId);
		LOGGER.info("Successfully saved and uploaded the setup component public keys. [electionEventId: {}]", electionEventId);
	}

}
