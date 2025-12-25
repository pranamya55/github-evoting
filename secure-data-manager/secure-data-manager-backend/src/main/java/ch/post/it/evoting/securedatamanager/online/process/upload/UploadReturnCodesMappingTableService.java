/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.upload;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.domain.configuration.SetupComponentCMTablePayloadChunks;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentCMTablePayloadService;

/**
 * Service which uploads setup component CMTable payload files to the voter portal.
 */
@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class UploadReturnCodesMappingTableService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadReturnCodesMappingTableService.class);

	private final SetupComponentCMTablePayloadService setupComponentCMTablePayloadService;
	private final UploadReturnCodesMappingTableRepository uploadReturnCodesMappingTableRepository;

	public UploadReturnCodesMappingTableService(
			final SetupComponentCMTablePayloadService setupComponentCMTablePayloadService,
			final UploadReturnCodesMappingTableRepository uploadReturnCodesMappingTableRepository) {
		this.setupComponentCMTablePayloadService = setupComponentCMTablePayloadService;
		this.uploadReturnCodesMappingTableRepository = uploadReturnCodesMappingTableRepository;
	}

	/**
	 * Uploads the available return codes mapping tables to the voter portal.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is invalid.
	 */
	public void upload(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		LOGGER.debug("Uploading setup component CMTable payloads... [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);

		final SetupComponentCMTablePayloadChunks setupComponentCMTablePayloadChunks = setupComponentCMTablePayloadService.load(electionEventId,
				verificationCardSetId);

		uploadReturnCodesMappingTableRepository.upload(electionEventId, verificationCardSetId,
				setupComponentCMTablePayloadChunks);

		LOGGER.info("Successfully uploaded setup component CMTable payloads. [electionEventId: {}, verificationCardSetId: {}, chunkCount: {}]",
				electionEventId, verificationCardSetId, setupComponentCMTablePayloadChunks.getChunkCount());
	}

}
