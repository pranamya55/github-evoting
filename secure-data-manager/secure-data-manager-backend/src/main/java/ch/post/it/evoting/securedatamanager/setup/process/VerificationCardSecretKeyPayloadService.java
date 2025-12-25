/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.UncheckedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Allows saving and retrieving verification card secret key payloads.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class VerificationCardSecretKeyPayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationCardSecretKeyPayloadService.class);

	private final VerificationCardSecretKeyPayloadFileRepository verificationCardSecretKeyPayloadFileRepository;

	public VerificationCardSecretKeyPayloadService(
			final VerificationCardSecretKeyPayloadFileRepository verificationCardSecretKeyPayloadFileRepository) {
		this.verificationCardSecretKeyPayloadFileRepository = verificationCardSecretKeyPayloadFileRepository;
	}

	/**
	 * Saves the verification card secret key payload.
	 *
	 * @param verificationCardSecretKeyPayload the verification card secret key payload to save.
	 * @throws NullPointerException if the payload to save is null.
	 * @throws UncheckedIOException if the verification card secret key cannot be saved.
	 */
	public void save(final VerificationCardSecretKeyPayload verificationCardSecretKeyPayload) {
		checkNotNull(verificationCardSecretKeyPayload);

		verificationCardSecretKeyPayloadFileRepository.save(verificationCardSecretKeyPayload);

		final String electionEventId = verificationCardSecretKeyPayload.electionEventId();
		final String verificationCardSetId = verificationCardSecretKeyPayload.verificationCardSetId();

		LOGGER.info("Saved verification card secret key payload. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);
	}

	/**
	 * Loads the verification card secret key payload by the given ids.
	 *
	 * @param electionEventId       the payload's election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the payload's verification card set id. Must be non-null and a valid UUID.
	 * @return the verification card secret key payload for the given ids.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if any input is not a valid UUID.
	 */
	public VerificationCardSecretKeyPayload load(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final VerificationCardSecretKeyPayload verificationCardSecretKeyPayload =
				verificationCardSecretKeyPayloadFileRepository.findById(electionEventId, verificationCardSetId)
						.orElseThrow(() -> new IllegalStateException(String.format(
								"Requested verification card secret key payload is not present. [electionEventId: %s, verificationCardSetId: %s]",
								electionEventId, verificationCardSetId)));

		LOGGER.info("Loaded verification card secret key payload. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);

		return verificationCardSecretKeyPayload;
	}
}
