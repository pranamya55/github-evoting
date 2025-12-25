/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.requestcckeys;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Allows saving and checking existence of control component public keys.
 */
@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class ControlComponentPublicKeysService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ControlComponentPublicKeysService.class);
	private final ControlComponentPublicKeysPayloadFileRepository controlComponentPublicKeysPayloadFileRepository;

	public ControlComponentPublicKeysService(final ControlComponentPublicKeysPayloadFileRepository controlComponentPublicKeysPayloadFileRepository) {
		this.controlComponentPublicKeysPayloadFileRepository = controlComponentPublicKeysPayloadFileRepository;
	}

	/**
	 * Saves a control component public keys payloads in the corresponding election event folder.
	 *
	 * @param controlComponentPublicKeysPayload the payload to save.
	 * @throws NullPointerException if {@code controlComponentPublicKeysPayload} is null.
	 */
	public void save(final ControlComponentPublicKeysPayload controlComponentPublicKeysPayload) {
		checkNotNull(controlComponentPublicKeysPayload);

		final String electionEventId = controlComponentPublicKeysPayload.getElectionEventId();
		final int nodeId = controlComponentPublicKeysPayload.getControlComponentPublicKeys().nodeId();

		controlComponentPublicKeysPayloadFileRepository.save(controlComponentPublicKeysPayload);
		LOGGER.info("Saved control component public keys payload. [electionEventId: {}, nodeId: {}]", electionEventId, nodeId);
	}

	/**
	 * Checks if all control component public keys payloads are present for the given election event id.
	 *
	 * @param electionEventId the election event id to check.
	 * @return {@code true} if all payloads are present, {@code false} otherwise.
	 * @throws FailedValidationException if {@code electionEventId} is invalid.
	 */
	public boolean exist(final String electionEventId) {
		validateUUID(electionEventId);

		return ControlComponentNode.ids().stream()
				.allMatch(nodeId -> controlComponentPublicKeysPayloadFileRepository.existsById(electionEventId, nodeId));
	}
}
