/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.upload;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.configuration.SetupComponentVerificationCardKeystoresPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationCardKeystoresPayloadFileRepository;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class SetupComponentVerificationCardKeystoresPayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentVerificationCardKeystoresPayloadService.class);

	private final SetupComponentVerificationCardKeystoresPayloadFileRepository setupComponentVerificationCardKeystoresPayloadFileRepository;
	private final UploadSetupComponentVerificationCardKeystoresRepository uploadSetupComponentVerificationCardKeystoresRepository;

	public SetupComponentVerificationCardKeystoresPayloadService(
			final SetupComponentVerificationCardKeystoresPayloadFileRepository setupComponentVerificationCardKeystoresPayloadFileRepository,
			final UploadSetupComponentVerificationCardKeystoresRepository uploadSetupComponentVerificationCardKeystoresRepository) {
		this.setupComponentVerificationCardKeystoresPayloadFileRepository = setupComponentVerificationCardKeystoresPayloadFileRepository;
		this.uploadSetupComponentVerificationCardKeystoresRepository = uploadSetupComponentVerificationCardKeystoresRepository;
	}

	/**
	 * Loads the setup component verification card keystores payload for the given {@code electionEventId} and {@code verificationCardSetId}.
	 *
	 * @param electionEventId       the payload's election event id.
	 * @param verificationCardSetId the payload's verification card set id.
	 * @return the setup component verification card keystores payload for the given ids.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if any input is not a valid UUID.
	 */
	public SetupComponentVerificationCardKeystoresPayload load(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final SetupComponentVerificationCardKeystoresPayload setupComponentVerificationCardKeystoresPayload =
				setupComponentVerificationCardKeystoresPayloadFileRepository.findById(electionEventId, verificationCardSetId)
						.orElseThrow(() -> new IllegalStateException(String.format(
								"Requested setup component verification card keystores payload is not present. [electionEventId: %s, verificationCardSetId: %s]",
								electionEventId, verificationCardSetId)));

		LOGGER.info("Loaded setup component verification card keystores payload. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);

		return setupComponentVerificationCardKeystoresPayload;
	}

	/**
	 * Uploads the list of setup component verification card keystores payloads to the voting-server.
	 *
	 * @param setupComponentVerificationCardKeystoresPayloads the list of setup component verification card keystores payloads. Must be non-null.
	 * @throws NullPointerException if the input is null.
	 */
	public void upload(final ImmutableList<SetupComponentVerificationCardKeystoresPayload> setupComponentVerificationCardKeystoresPayloads) {
		checkNotNull(setupComponentVerificationCardKeystoresPayloads);

		setupComponentVerificationCardKeystoresPayloads.forEach(setupComponentVerificationCardKeystoresPayload -> {
			final String electionEventId = setupComponentVerificationCardKeystoresPayload.getElectionEventId();
			final String verificationCardSetId = setupComponentVerificationCardKeystoresPayload.getVerificationCardSetId();

			LOGGER.info("Uploading setup component verification card keystores payload... [electionEventId: {}, verificationCardSetId: {}]",
					electionEventId, verificationCardSetId);

			uploadSetupComponentVerificationCardKeystoresRepository.upload(setupComponentVerificationCardKeystoresPayload);

			LOGGER.info("Successfully uploaded setup component verification card keystores payload. [electionEventId: {}, verificationCardSetId: {}]",
					electionEventId, verificationCardSetId);
		});

	}
}
