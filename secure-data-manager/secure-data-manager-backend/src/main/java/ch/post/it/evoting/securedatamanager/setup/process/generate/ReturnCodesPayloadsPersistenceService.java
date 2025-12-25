/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generate;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.domain.configuration.SetupComponentCMTablePayload;
import ch.post.it.evoting.domain.configuration.SetupComponentCMTablePayloadChunks;
import ch.post.it.evoting.domain.configuration.VoterReturnCodesPayload;
import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.VoterReturnCodesPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentCMTablePayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentLVCCAllowListPayloadService;

@Service
@ConditionalOnProperty("role.isSetup")
public class ReturnCodesPayloadsPersistenceService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReturnCodesPayloadsPersistenceService.class);

	private final VoterReturnCodesPayloadService voterReturnCodesPayloadService;
	private final SetupComponentCMTablePayloadService setupComponentCMTablePayloadService;
	private final SetupComponentLVCCAllowListPayloadService setupComponentLVCCAllowListPayloadService;

	public ReturnCodesPayloadsPersistenceService(
			final VoterReturnCodesPayloadService voterReturnCodesPayloadService,
			final SetupComponentCMTablePayloadService setupComponentCMTablePayloadService,
			final SetupComponentLVCCAllowListPayloadService setupComponentLVCCAllowListPayloadService) {
		this.voterReturnCodesPayloadService = voterReturnCodesPayloadService;
		this.setupComponentCMTablePayloadService = setupComponentCMTablePayloadService;
		this.setupComponentLVCCAllowListPayloadService = setupComponentLVCCAllowListPayloadService;
	}

	/**
	 * Persists on the file system the following payloads:
	 * <ul>
	 *     <li>Setup component CMTable payloads</li>
	 *     <li>Voter Return Codes payload</li>
	 *     <li>Setup component LVCC allow list payload</li>
	 * </ul>
	 *
	 * @param electionEventId                    the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId              the verification card set id. Must be non-null and a valid UUID.
	 * @param setupComponentCMTablePayloadChunks the list of {@link SetupComponentCMTablePayload}. Must be non-null.
	 * @param voterReturnCodesPayload            the {@link VoterReturnCodesPayload}. Must be non-null.
	 * @param setupComponentLVCCAllowListPayload the {@link SetupComponentLVCCAllowListPayload}. Must be non-null.
	 * @throws NullPointerException      if any of the input is null.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is invalid.
	 */
	public void save(final String electionEventId, final String verificationCardSetId,
			final SetupComponentCMTablePayloadChunks setupComponentCMTablePayloadChunks, final VoterReturnCodesPayload voterReturnCodesPayload,
			final SetupComponentLVCCAllowListPayload setupComponentLVCCAllowListPayload) {

		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkNotNull(setupComponentCMTablePayloadChunks);
		checkNotNull(voterReturnCodesPayload);
		checkNotNull(setupComponentLVCCAllowListPayload);

		setupComponentCMTablePayloadService.save(setupComponentCMTablePayloadChunks);
		LOGGER.info("Setup component CMTable payloads successfully persisted. [electionEventId: {}, verificationCardSetId: {}, chunkCount: {}]",
				electionEventId, verificationCardSetId, setupComponentCMTablePayloadChunks.getChunkCount());

		voterReturnCodesPayloadService.save(voterReturnCodesPayload, verificationCardSetId);
		LOGGER.info("Voter return codes payload successfully persisted. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);

		setupComponentLVCCAllowListPayloadService.save(setupComponentLVCCAllowListPayload);
		LOGGER.info("Setup component LVCC allow list payload successfully persisted. [electionEventId: {}, verificationCardSetId: {}]",
				electionEventId, verificationCardSetId);
	}

}
