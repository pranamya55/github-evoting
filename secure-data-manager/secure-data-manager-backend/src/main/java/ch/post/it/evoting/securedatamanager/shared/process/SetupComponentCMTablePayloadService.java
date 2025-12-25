/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.domain.configuration.SetupComponentCMTablePayload;
import ch.post.it.evoting.domain.configuration.SetupComponentCMTablePayloadChunks;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Allows to generate and persist {@link SetupComponentCMTablePayload}.
 */
@Service
public class SetupComponentCMTablePayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentCMTablePayloadService.class);

	private final SetupComponentCMTablePayloadFileRepository setupComponentCMTablePayloadFileRepository;

	public SetupComponentCMTablePayloadService(final SetupComponentCMTablePayloadFileRepository setupComponentCMTablePayloadFileRepository) {
		this.setupComponentCMTablePayloadFileRepository = setupComponentCMTablePayloadFileRepository;
	}

	/**
	 * Persists a list of {@link SetupComponentCMTablePayload}.
	 *
	 * @param setupComponentCMTablePayloadChunks, the payloads to be saved. Must be non-null.
	 * @throws NullPointerException any payload is null.
	 */
	public void save(final SetupComponentCMTablePayloadChunks setupComponentCMTablePayloadChunks) {
		checkNotNull(setupComponentCMTablePayloadChunks);

		setupComponentCMTablePayloadChunks.payloads().stream().parallel().forEach(setupComponentCMTablePayloadFileRepository::save);
		LOGGER.info("Return codes mapping table successfully saved. [electionEventId: {}, verificationCardSetId: {}, chunkCount: {}]",
				setupComponentCMTablePayloadChunks.getElectionEventId(), setupComponentCMTablePayloadChunks.getVerificationCardSetId(),
				setupComponentCMTablePayloadChunks.getChunkCount());
	}

	/**
	 * Loads all the {@link SetupComponentCMTablePayload} for the given the election event and verification card set.
	 *
	 * @param electionEventId,       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId, the verification card set id. Must be non-null and a valid UUID.
	 * @return the list of {@link SetupComponentCMTablePayload}.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is invalid.
	 * @throws IllegalStateException     if the requested setup component CMTable payloads are not present.
	 */
	public SetupComponentCMTablePayloadChunks load(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		return setupComponentCMTablePayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(electionEventId, verificationCardSetId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Requested setup component CMTable payloads are not present. [electionEventId: %s, verificationCardSetId: %s]",
								electionEventId, verificationCardSetId)));
	}

}
