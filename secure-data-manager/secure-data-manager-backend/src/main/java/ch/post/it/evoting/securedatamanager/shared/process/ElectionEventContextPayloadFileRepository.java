/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.Constants;

/**
 * Allows performing operations with the election event context payload. The election event context payload is persisted/retrieved to/from the file
 * system of the SDM, in its workspace.
 */
@Repository
public class ElectionEventContextPayloadFileRepository {
	@VisibleForTesting
	public static final String PAYLOAD_FILE_NAME = Constants.CONFIG_FILE_NAME_ELECTION_EVENT_CONTEXT_PAYLOAD;

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectionEventContextPayloadFileRepository.class);

	private final ObjectMapper objectMapper;
	private final PathResolver pathResolver;

	public ElectionEventContextPayloadFileRepository(final ObjectMapper objectMapper, final PathResolver pathResolver) {
		this.objectMapper = objectMapper;
		this.pathResolver = pathResolver;
	}

	/**
	 * Persists an election event context payload to the file system.
	 *
	 * @param electionEventContextPayload the election event context payload to persist. Must be non-null.
	 * @return the path where the election event context payload has been successfully persisted.
	 * @throws NullPointerException if {@code electionEventContextPayload} is null.
	 * @throws UncheckedIOException if the serialization of the election event context payload fails.
	 */
	public Path save(final ElectionEventContextPayload electionEventContextPayload) {
		checkNotNull(electionEventContextPayload);

		final String electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();

		final Path electionEventPath = pathResolver.resolveElectionEventPath(electionEventId);
		final Path payloadPath = electionEventPath.resolve(PAYLOAD_FILE_NAME);

		try {
			final byte[] payloadBytes = objectMapper.writeValueAsBytes(electionEventContextPayload);

			final Path writePath = Files.write(payloadPath, payloadBytes);
			LOGGER.debug("Successfully persisted election event context payload. [electionEventId: {}, path: {}]", electionEventId, payloadPath);

			return writePath;
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to serialize election event context payload. [electionEventId: %s, path: %s]", electionEventId,
							payloadPath), e);
		}
	}

	/**
	 * Checks if the election event context payload file exists for the given {@code electionEventId}.
	 *
	 * @param electionEventId the election event id to check. Must be non-null and a valid UUID.
	 * @return {@code true} if the election event context payload file exists, {@code false} otherwise.
	 * @throws FailedValidationException if {@code electionEventId} is null or not a valid UUID.
	 */
	public boolean existsById(final String electionEventId) {
		validateUUID(electionEventId);

		final Path electionEventPath = pathResolver.resolveElectionEventPath(electionEventId);
		final Path payloadPath = electionEventPath.resolve(PAYLOAD_FILE_NAME);
		LOGGER.debug("Checking election event context payload file existence. [electionEventId: {}, path: {}]", electionEventId, payloadPath);

		return Files.exists(payloadPath);
	}

	/**
	 * Retrieves from the file system an election event context payload by election event id.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the election event context payload with the given id or {@link Optional#empty} if none found.
	 * @throws FailedValidationException if {@code electionEventId} is null or not a valid UUID.
	 * @throws UncheckedIOException      if the deserialization of the election event context payload fails.
	 */
	public Optional<ElectionEventContextPayload> findById(final String electionEventId) {
		validateUUID(electionEventId);

		final Path electionEventPath = pathResolver.resolveElectionEventPath(electionEventId);
		final Path payloadPath = electionEventPath.resolve(PAYLOAD_FILE_NAME);

		if (!Files.exists(payloadPath)) {
			LOGGER.debug("Requested election event context payload does not exist. [electionEventId: {}, path: {}]", electionEventId, payloadPath);
			return Optional.empty();
		}

		try {
			return Optional.of(objectMapper.readValue(payloadPath.toFile(), ElectionEventContextPayload.class));
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to deserialize election event context payload. [electionEventId: %s, path: %s]", electionEventId,
							payloadPath), e);
		}
	}

}
