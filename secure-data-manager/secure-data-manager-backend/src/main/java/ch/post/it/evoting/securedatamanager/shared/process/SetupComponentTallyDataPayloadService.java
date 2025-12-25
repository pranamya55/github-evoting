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

import ch.post.it.evoting.evotinglibraries.domain.configuration.SetupComponentTallyDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Allows saving, retrieving and finding setup component tally data payloads.
 */
@Service
public class SetupComponentTallyDataPayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentTallyDataPayloadService.class);

	private final SetupComponentTallyDataPayloadFileRepository setupComponentTallyDataPayloadFileRepository;

	public SetupComponentTallyDataPayloadService(final SetupComponentTallyDataPayloadFileRepository setupComponentTallyDataPayloadFileRepository) {
		this.setupComponentTallyDataPayloadFileRepository = setupComponentTallyDataPayloadFileRepository;
	}

	/**
	 * Saves a setup component tally data payload in the corresponding election event folder.
	 *
	 * @param setupComponentTallyDataPayload the setup component tally data payload to save. Must be non-null.
	 * @throws NullPointerException if {@code setupComponentTallyDataPayload} is null.
	 */
	public void save(final SetupComponentTallyDataPayload setupComponentTallyDataPayload) {
		checkNotNull(setupComponentTallyDataPayload);

		setupComponentTallyDataPayloadFileRepository.save(setupComponentTallyDataPayload);

		LOGGER.info("Saved setup component tally data payload. [electionEventId: {}, verificationCardSetId: {}]",
				setupComponentTallyDataPayload.getElectionEventId(), setupComponentTallyDataPayload.getVerificationCardSetId());
	}

	/**
	 * Checks if the setup component tally data payload is present for the given election event id.
	 *
	 * @param electionEventId       the election event id to check. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return {@code true} if the setup component tally data payload is present, {@code false} otherwise.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if any input is not a valid UUID.
	 */
	public boolean exist(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		return setupComponentTallyDataPayloadFileRepository.existsById(electionEventId, verificationCardSetId);
	}

	/**
	 * Loads the setup component tally data payload for the given {@code electionEventId}. The result of this method is stored in a synchronized
	 * cache.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return the setup component tally data payload for this {@code electionEventId}.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if any input is not a valid UUID.
	 */
	@Cacheable(value = "setupComponentTallyDataPayloads", sync = true)
	public SetupComponentTallyDataPayload load(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final SetupComponentTallyDataPayload setupComponentTallyDataPayload =
				setupComponentTallyDataPayloadFileRepository.findById(electionEventId, verificationCardSetId)
						.orElseThrow(() -> new IllegalStateException(String.format(
								"Requested setup component tally data payload is not present. [electionEventId: %s, verificationCardSetId: %s]",
								electionEventId, verificationCardSetId)));

		LOGGER.info("Loaded setup component tally data payload. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);

		return setupComponentTallyDataPayload;
	}

}
