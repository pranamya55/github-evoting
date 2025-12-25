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

import ch.post.it.evoting.domain.configuration.ElectoralBoardHashesPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.Constants;

/**
 * Allows performing operations with the electoral board hashes payload. The electoral board hashes payload is persisted/retrieved to/from the
 * file system of the SDM, in its workspace.
 */
@Repository
public class ElectoralBoardHashesPayloadFileRepository {
	@VisibleForTesting
	public static final String PAYLOAD_FILE_NAME = Constants.CONFIG_SETUP_COMPONENT_ELECTORAL_BOARD_HASHES_PAYLOAD;

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectoralBoardHashesPayloadFileRepository.class);

	private final ObjectMapper objectMapper;
	private final PathResolver pathResolver;

	public ElectoralBoardHashesPayloadFileRepository(final ObjectMapper objectMapper, final PathResolver pathResolver) {
		this.objectMapper = objectMapper;
		this.pathResolver = pathResolver;
	}

	/**
	 * Persists an electoral board hashes payload to the file system.
	 *
	 * @param electoralBoardHashesPayload the electoral board hashes payload to persist. Must be non-null.
	 * @return the path where the electoral board hashes payload has been successfully persisted.
	 * @throws NullPointerException if {@code electoralBoardHashesPayload} is null.
	 * @throws UncheckedIOException if the serialization of the electoral board hashes payload fails.
	 */
	public Path save(final ElectoralBoardHashesPayload electoralBoardHashesPayload) {
		checkNotNull(electoralBoardHashesPayload);

		final String electionEventId = electoralBoardHashesPayload.getElectionEventId();
		final Path payloadPath = pathResolver.resolveElectionEventPath(electionEventId).resolve(PAYLOAD_FILE_NAME);

		try {
			final byte[] payloadBytes = objectMapper.writeValueAsBytes(electoralBoardHashesPayload);

			final Path writePath = Files.write(payloadPath, payloadBytes);
			LOGGER.debug("Successfully persisted electoral board hashes payload. [electionEventId: {}, path: {}]", electionEventId, payloadPath);

			return writePath;
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to serialize electoral board hashes payload. [electionEventId: %s, path: %s]", electionEventId,
							payloadPath), e);
		}
	}

	/**
	 * Checks if the electoral board hashes payload file exists for the given {@code electionEventId}.
	 *
	 * @param electionEventId the election event id to check. Must be non-null and a valid UUID.
	 * @return {@code true} if the electoral board hashes payload file exists, {@code false} otherwise.
	 * @throws FailedValidationException if {@code electionEventId} is null or not a valid UUID.
	 */
	public boolean existsById(final String electionEventId) {
		validateUUID(electionEventId);

		final Path payloadPath = pathResolver.resolveElectionEventPath(electionEventId).resolve(PAYLOAD_FILE_NAME);
		LOGGER.debug("Checking electoral board hashes payload file existence. [electionEventId: {}, path: {}]", electionEventId, payloadPath);

		return Files.exists(payloadPath);
	}

	/**
	 * Retrieves from the file system an electoral board hashes payload by election event id.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the electoral board hashes payload with the given id or {@link Optional#empty} if none found.
	 * @throws FailedValidationException if {@code electionEventId} is null or not a valid UUID.
	 * @throws UncheckedIOException      if the deserialization of the electoral board hashes payload fails.
	 */
	public Optional<ElectoralBoardHashesPayload> findById(final String electionEventId) {
		validateUUID(electionEventId);

		final Path payloadPath = pathResolver.resolveElectionEventPath(electionEventId).resolve(PAYLOAD_FILE_NAME);
		if (!Files.exists(payloadPath)) {
			LOGGER.debug("Requested electoral board hashes payload does not exist. [electionEventId: {}, path: {}]", electionEventId, payloadPath);
			return Optional.empty();
		}

		try {
			return Optional.of(objectMapper.readValue(payloadPath.toFile(), ElectoralBoardHashesPayload.class));
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to deserialize electoral board hashes payload. [electionEventId: %s, path: %s]", electionEventId,
							payloadPath), e);
		}
	}

}
