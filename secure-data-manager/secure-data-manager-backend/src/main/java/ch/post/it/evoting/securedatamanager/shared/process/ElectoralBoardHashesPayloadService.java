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

import ch.post.it.evoting.domain.configuration.ElectoralBoardHashesPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Allows saving, retrieving and finding existing electoral board hashes payloads.
 */
@Service
public class ElectoralBoardHashesPayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectoralBoardHashesPayloadService.class);

	private final ElectoralBoardHashesPayloadFileRepository electoralBoardHashesPayloadFileRepository;

	public ElectoralBoardHashesPayloadService(
			final ElectoralBoardHashesPayloadFileRepository electoralBoardHashesPayloadFileRepository) {
		this.electoralBoardHashesPayloadFileRepository = electoralBoardHashesPayloadFileRepository;
	}

	/**
	 * Saves an electoral board hashes payload in the corresponding election event folder.
	 *
	 * @param electoralBoardHashesPayload the electoral board hashes payload to save. Must be non-null.
	 * @throws NullPointerException if the payload is null.
	 */
	public void save(final ElectoralBoardHashesPayload electoralBoardHashesPayload) {
		checkNotNull(electoralBoardHashesPayload);

		electoralBoardHashesPayloadFileRepository.save(electoralBoardHashesPayload);
		LOGGER.info("Signed and saved electoral board hashes payload. [electionEventId: {}]", electoralBoardHashesPayload.getElectionEventId());
	}

	/**
	 * Checks if the electoral board hashes payload is present for the given election event id.
	 *
	 * @param electionEventId the election event id to check. Must be non-null and a valid UUID.
	 * @return {@code true} if the electoral board hashes payload is present, {@code false} otherwise.
	 * @throws FailedValidationException if {@code electionEventId} is invalid.
	 */
	public boolean exist(final String electionEventId) {
		validateUUID(electionEventId);

		return electoralBoardHashesPayloadFileRepository.existsById(electionEventId);
	}

	/**
	 * Loads the electoral board hashes for the given {@code electionEventId}. The result of this method is stored in a synchronized cache.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the electoral board key hashes for this {@code electionEventId}.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if {@code electionEventId} is an invalid UUID.
	 * @throws IllegalStateException     if the requested electoral board hashes payload is not present. </li>
	 */
	@Cacheable(value = "electoralBoardHashesPayloads", sync = true)
	public ElectoralBoardHashesPayload load(final String electionEventId) {
		validateUUID(electionEventId);

		final ElectoralBoardHashesPayload electoralBoardHashesPayload = electoralBoardHashesPayloadFileRepository.findById(electionEventId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Requested electoral board hashes payload is not present. [electionEventId: %s]", electionEventId)));

		LOGGER.info("Loaded electoral board hashes payload. [electionEventId: {}]", electionEventId);

		return electoralBoardHashesPayload;
	}

}
