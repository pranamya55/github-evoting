/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Allows saving, retrieving and finding existing election event context payloads.
 */
@Service
public class SetupComponentPublicKeysPayloadService {
	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentPublicKeysPayloadService.class);

	private final SetupComponentPublicKeysPayloadFileRepository setupComponentPublicKeysPayloadFileRepository;

	public SetupComponentPublicKeysPayloadService(final SetupComponentPublicKeysPayloadFileRepository setupComponentPublicKeysPayloadFileRepository) {
		this.setupComponentPublicKeysPayloadFileRepository = setupComponentPublicKeysPayloadFileRepository;
	}

	/**
	 * Saves a setup component public keys payload in the corresponding election event folder.
	 *
	 * @param setupComponentPublicKeysPayload the election event context payload to save.
	 * @throws NullPointerException if {@code electionEventContext} is null.
	 */
	public void save(final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload) {
		checkNotNull(setupComponentPublicKeysPayload);

		final String electionEventId = setupComponentPublicKeysPayload.getElectionEventId();

		setupComponentPublicKeysPayloadFileRepository.save(setupComponentPublicKeysPayload);
		LOGGER.info("Saved election event context payload. [electionEventId: {}]", electionEventId);
	}

	/**
	 * Checks if the setup component public keys payload is present for the given election event id.
	 *
	 * @param electionEventId the election event id to check.
	 * @return {@code true} if the election event context payload is present, {@code false} otherwise.
	 * @throws FailedValidationException if {@code electionEventId} is invalid.
	 */
	public boolean exist(final String electionEventId) {
		validateUUID(electionEventId);

		return setupComponentPublicKeysPayloadFileRepository.existsById(electionEventId);
	}

	/**
	 * Loads the setup component public keys payload for the given {@code electionEventId}. The result of this method is stored in a synchronized
	 * cache.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the setup component public keys payload for this {@code electionEventId}.
	 * @throws FailedValidationException if {@code electionEventId} is invalid.
	 * @throws IllegalStateException     if the requested setup component public keys is not present.
	 */
	@Cacheable(value = "setupComponentPublicKeysPayloads", sync = true)
	public SetupComponentPublicKeysPayload load(final String electionEventId) {
		validateUUID(electionEventId);

		final SetupComponentPublicKeysPayload payload = setupComponentPublicKeysPayloadFileRepository.findById(electionEventId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Requested setup component public keys payload is not present. [electionEventId: %s]", electionEventId)));

		LOGGER.info("Loaded setup component public keys payload. [electionEventId: {}]", electionEventId);

		return payload;
	}

}
