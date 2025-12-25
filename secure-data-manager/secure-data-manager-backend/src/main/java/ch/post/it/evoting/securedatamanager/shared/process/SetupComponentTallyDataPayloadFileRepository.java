/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_NAME_SETUP_COMPONENT_TALLY_DATA_PAYLOAD;
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

import ch.post.it.evoting.evotinglibraries.domain.configuration.SetupComponentTallyDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Allows performing operations with the setup component tally data payload. The setup component tally data payload is persisted/retrieved to/from the
 * file system of the SDM, in its workspace.
 */
@Repository
public class SetupComponentTallyDataPayloadFileRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentTallyDataPayloadFileRepository.class);

	private final ObjectMapper objectMapper;
	private final PathResolver pathResolver;

	public SetupComponentTallyDataPayloadFileRepository(final ObjectMapper objectMapper, final PathResolver pathResolver) {
		this.objectMapper = objectMapper;
		this.pathResolver = pathResolver;
	}

	/**
	 * Persists a setup component tally data payload to the file system.
	 *
	 * @param setupComponentTallyDataPayload the setup component tally data payload to persist. Must be non-null.
	 * @return the path where the setup component tally data payload has been successfully persisted.
	 * @throws NullPointerException if {@code setupComponentTallyDataPayload} is null.
	 * @throws UncheckedIOException if the serialization of the setup component tally data payload fails.
	 */
	public Path save(final SetupComponentTallyDataPayload setupComponentTallyDataPayload) {
		checkNotNull(setupComponentTallyDataPayload);

		final String electionEventId = setupComponentTallyDataPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentTallyDataPayload.getVerificationCardSetId();

		final Path payloadPath = getPayloadPath(electionEventId, verificationCardSetId);
		try {
			final byte[] payloadBytes = objectMapper.writeValueAsBytes(setupComponentTallyDataPayload);

			final Path writePath = Files.write(payloadPath, payloadBytes);
			LOGGER.debug("Successfully persisted setup component tally data payload. [electionEventId: {}, verificationCardSetId: {}, path: {}]",
					electionEventId, verificationCardSetId, payloadPath);

			return writePath;
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format(
							"Failed to serialize setup component tally data payload. [electionEventId: %s, verificationCardSetId: %s, path: %s]",
							electionEventId, verificationCardSetId, payloadPath), e);
		}
	}

	/**
	 * Checks if the setup component tally data payload file exists for the given {@code electionEventId} and {@code verificationCardSetId}.
	 *
	 * @param electionEventId       the election event id to check. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return {@code true} if the setup component tally data payload file exists, {@code false} otherwise.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if any input is not a valid UUID.
	 */
	public boolean existsById(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final Path payloadPath = getPayloadPath(electionEventId, verificationCardSetId);
		LOGGER.debug("Checking setup component tally data payload file existence. [electionEventId: {}, verificationCardSetId: {}, path: {}]",
				electionEventId, verificationCardSetId, payloadPath);

		return Files.exists(payloadPath);
	}

	/**
	 * Retrieves from the file system a setup component tally data payload by election event id and verification card set id.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return the setup component tally data payload with the given id or {@link Optional#empty} if none found.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if any input is not a valid UUID.
	 * @throws UncheckedIOException      if the deserialization of the setup component tally data payload fails.
	 */
	public Optional<SetupComponentTallyDataPayload> findById(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final Path payloadPath = getPayloadPath(electionEventId, verificationCardSetId);
		if (!Files.exists(payloadPath)) {
			LOGGER.debug("Requested setup component tally data payload does not exist. [electionEventId: {}, verificationCardSetId: {}, path: {}]",
					electionEventId, verificationCardSetId, payloadPath);
			return Optional.empty();
		}

		try {
			return Optional.of(objectMapper.readValue(payloadPath.toFile(), SetupComponentTallyDataPayload.class));
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format(
							"Failed to deserialize setup component tally data payload. [electionEventId: %s, verificationCardSetId: %s, path: %s]",
							electionEventId, verificationCardSetId, payloadPath), e);
		}
	}

	private Path getPayloadPath(final String electionEventId, final String verificationCardSetId) {
		final Path verificationCardSetPath = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId);
		return verificationCardSetPath.resolve(CONFIG_FILE_NAME_SETUP_COMPONENT_TALLY_DATA_PAYLOAD);
	}

}
