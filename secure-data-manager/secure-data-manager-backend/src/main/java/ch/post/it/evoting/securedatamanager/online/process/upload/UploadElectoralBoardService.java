/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.upload;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.configuration.SetupComponentVerificationCardKeystoresPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoard;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoardService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentPublicKeysPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.Status;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

/**
 * Service which uploads files to voter portal after creating the electoral board
 */
@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class UploadElectoralBoardService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadElectoralBoardService.class);

	private final ElectoralBoardService electoralBoardService;
	private final VerificationCardSetService verificationCardSetService;
	private final SetupComponentPublicKeysPayloadService setupComponentPublicKeysPayloadService;
	private final UploadSetupComponentPublicKeysRepository uploadSetupComponentPublicKeysRepository;
	private final SetupComponentVerificationCardKeystoresPayloadService setupComponentVerificationCardKeystoresPayloadService;

	public UploadElectoralBoardService(
			final ElectoralBoardService electoralBoardService,
			final VerificationCardSetService verificationCardSetService,
			final SetupComponentPublicKeysPayloadService setupComponentPublicKeysPayloadService,
			final UploadSetupComponentPublicKeysRepository uploadSetupComponentPublicKeysRepository,
			final SetupComponentVerificationCardKeystoresPayloadService setupComponentVerificationCardKeystoresPayloadService) {
		this.electoralBoardService = electoralBoardService;
		this.verificationCardSetService = verificationCardSetService;
		this.uploadSetupComponentPublicKeysRepository = uploadSetupComponentPublicKeysRepository;
		this.setupComponentPublicKeysPayloadService = setupComponentPublicKeysPayloadService;
		this.setupComponentVerificationCardKeystoresPayloadService = setupComponentVerificationCardKeystoresPayloadService;
	}

	/**
	 * Uploads the signed authentication context configuration to the voter portal:
	 * <ul>
	 *     <li>The setup component public keys payload.</li>
	 *     <li>The setup component verification card keystores payloads.</li>
	 * </ul>
	 */
	public void upload(final String electionEventId) {
		validateUUID(electionEventId);

		final ElectoralBoard electoralBoard = electoralBoardService.getElectoralBoard();

		LOGGER.debug("Uploading the signed authentication context configuration. [electionEventId: {}, electoralBoardId: {}]", electionEventId,
				electoralBoard.id());

		uploadSetupComponentPublicKeysPayload(electionEventId);
		uploadSetupComponentVerificationCardKeystoresPayloads(electionEventId);

		electoralBoardService.updateStatus(electoralBoard.id(), Status.UPLOADED);

		LOGGER.info("The signed authentication context configuration was uploaded successfully. [electionEventId: {}, electoralBoardId: {}]",
				electionEventId, electoralBoard.id());
	}

	/**
	 * Uploads the setup component public keys payload.
	 *
	 * @param electionEventId the election event id.
	 * @throws IllegalStateException            if an error occurred while verifying the signature of the setup component public keys payload or the
	 *                                          request for uploading the setup component public keys failed.
	 * @throws InvalidPayloadSignatureException if the signature of the setup component public keys payload is invalid.
	 */
	private void uploadSetupComponentPublicKeysPayload(final String electionEventId) {

		LOGGER.info("Uploading setup component public keys payload... [electionEventId: {}]", electionEventId);

		final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload = setupComponentPublicKeysPayloadService.load(electionEventId);

		uploadSetupComponentPublicKeysRepository.upload(setupComponentPublicKeysPayload);
		LOGGER.info("Successfully uploaded setup component public keys payload. [electionEventId: {}]", electionEventId);
	}

	/**
	 * Uploads the setup component verification card keystores payloads.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @throws FailedValidationException if the election event id is not a valid UUID.
	 * @throws NullPointerException      if the election event id is null.
	 */
	private void uploadSetupComponentVerificationCardKeystoresPayloads(final String electionEventId) {
		validateUUID(electionEventId);

		LOGGER.info("Uploading setup component verification card keystores payloads... [electionEventId: {}]", electionEventId);

		final ImmutableList<String> verificationCardSetIds = verificationCardSetService.getVerificationCardSetIds(Status.UPLOADED);

		final ImmutableList<SetupComponentVerificationCardKeystoresPayload> setupComponentVerificationCardKeystoresPayloads =
				verificationCardSetIds.stream()
						.map(verificationCardId -> setupComponentVerificationCardKeystoresPayloadService.load(electionEventId, verificationCardId))
						.collect(toImmutableList());

		setupComponentVerificationCardKeystoresPayloadService.upload(setupComponentVerificationCardKeystoresPayloads);

		LOGGER.info("Successfully uploaded setup component verification card keystores payloads. [electionEventId: {}]", electionEventId);
	}

}
