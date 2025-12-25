/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.constituteelectoralboard;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.domain.configuration.SetupComponentVerificationCardKeystoresPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationCardKeystoresPayloadFileRepository;

@Service
@ConditionalOnProperty("role.isSetup")
public class SetupComponentVerificationCardKeystoresPayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentVerificationCardKeystoresPayloadService.class);

	private final SetupComponentVerificationCardKeystoresPayloadFileRepository setupComponentVerificationCardKeystoresPayloadFileRepository;

	public SetupComponentVerificationCardKeystoresPayloadService(
			final SetupComponentVerificationCardKeystoresPayloadFileRepository setupComponentVerificationCardKeystoresPayloadFileRepository) {
		this.setupComponentVerificationCardKeystoresPayloadFileRepository = setupComponentVerificationCardKeystoresPayloadFileRepository;
	}

	/**
	 * Saves a setup component verification card keystores payload for the given {@code electionEventId} and {@code verificationCardSetId}.
	 *
	 * @param payload the setup component verification card keystores payload to save.
	 * @throws FailedValidationException if any of the ids are invalid.
	 * @throws NullPointerException      if any of {@code electionEventId}, {@code verificationCardSetId} or {@code value} is null.
	 * @throws IllegalStateException     if the verification card secret key cannot be saved.
	 */
	public void save(final SetupComponentVerificationCardKeystoresPayload payload) {
		checkNotNull(payload);

		setupComponentVerificationCardKeystoresPayloadFileRepository.save(payload);

		LOGGER.info("Successfully persisted setup component verification card keystores payload. [electionEventId: {}, verificationCardSetId: {}]",
				payload.getElectionEventId(), payload.getVerificationCardSetId());
	}

}
