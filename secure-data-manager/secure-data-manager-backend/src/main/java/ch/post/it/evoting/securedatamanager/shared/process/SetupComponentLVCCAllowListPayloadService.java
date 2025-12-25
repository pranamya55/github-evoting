/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Allows saving, retrieving and finding existing setup component LVCC allow list payloads.
 */
@Service
public class SetupComponentLVCCAllowListPayloadService {
	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentLVCCAllowListPayloadService.class);

	private final SetupComponentLVCCAllowListPayloadFileRepository setupComponentLVCCAllowListPayloadFileRepository;

	public SetupComponentLVCCAllowListPayloadService(
			final SetupComponentLVCCAllowListPayloadFileRepository setupComponentLVCCAllowListPayloadFileRepository) {
		this.setupComponentLVCCAllowListPayloadFileRepository = setupComponentLVCCAllowListPayloadFileRepository;
	}

	/**
	 * Saves a setup component LVCC allow list payload in the corresponding verification card set folder.
	 *
	 * @param setupComponentLVCCAllowListPayload the LVCC allow lis payload to save.
	 * @throws NullPointerException if {@code setupComponentLVCCAllowListPayload} is null.
	 */
	public void save(final SetupComponentLVCCAllowListPayload setupComponentLVCCAllowListPayload) {
		checkNotNull(setupComponentLVCCAllowListPayload);

		final String electionEventId = setupComponentLVCCAllowListPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentLVCCAllowListPayload.getVerificationCardSetId();

		setupComponentLVCCAllowListPayloadFileRepository.save(setupComponentLVCCAllowListPayload);
		LOGGER.info("Saved setup component LVCC allow list payload. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);
	}

	/**
	 * Checks if the setup component LVCC allow list payload is present for the given election event id and verification card set id.
	 *
	 * @param electionEventId       the election event id to check. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return {@code true} if the setup component LVCC allow list payload is present, {@code false} otherwise.
	 * @throws NullPointerException      if {@code electionEventId} or {@code verificationCardSetId} is null.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is not a valid UUID.
	 */
	public boolean exist(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		return setupComponentLVCCAllowListPayloadFileRepository.existsByElectionEventIdAndVerificationCardSetId(electionEventId,
				verificationCardSetId);
	}

	/**
	 * Loads the setup component LVCC allow list payload for the given election event id and verification card set id.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return the setup component LVCC allow list payload for this {@code electionEventId} and {@code verificationCardSetId}.
	 * @throws NullPointerException      if {@code electionEventId} or {@code verificationCardSetId} is null.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is not a valid UUID.
	 */
	public SetupComponentLVCCAllowListPayload load(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		return setupComponentLVCCAllowListPayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(electionEventId,
						verificationCardSetId)
				.orElseThrow(() -> new IllegalStateException(String.format(
						"Requested setup component LVCC allow list payload is not present. [electionEventId: %s, verificationCardSetId: %s]",
						electionEventId, verificationCardSetId)));
	}

}
