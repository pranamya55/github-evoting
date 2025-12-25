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

import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.Constants;

/**
 * Allows performing operations with the setup component LVCC allow list payload. The setup component LVCC allow list payload is persisted/retrieved
 * to/from the file system of the SDM, in its workspace.
 */
@Repository
public class SetupComponentLVCCAllowListPayloadFileRepository {
	@VisibleForTesting
	public static final String PAYLOAD_FILE_NAME = "setupComponentLVCCAllowListPayload" + Constants.JSON;

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentLVCCAllowListPayloadFileRepository.class);

	private final ObjectMapper objectMapper;
	private final PathResolver pathResolver;

	public SetupComponentLVCCAllowListPayloadFileRepository(final ObjectMapper objectMapper, final PathResolver pathResolver) {
		this.objectMapper = objectMapper;
		this.pathResolver = pathResolver;
	}

	/**
	 * Persists a setup component LVCC allow list payload to the file system.
	 *
	 * @param setupComponentLVCCAllowListPayload the setup component LVCC allow list payload to persist. Must be non-null.
	 * @return the path where the setup component LVCC allow list payload has been successfully persisted.
	 * @throws NullPointerException if {@code setupComponentLVCCAllowListPayload} is null.
	 * @throws UncheckedIOException if the serialization of the setup component LVCC allow list payload fails.
	 */
	public Path save(final SetupComponentLVCCAllowListPayload setupComponentLVCCAllowListPayload) {
		checkNotNull(setupComponentLVCCAllowListPayload);

		final String electionEventId = setupComponentLVCCAllowListPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentLVCCAllowListPayload.getVerificationCardSetId();

		final Path verificationCardSetPath = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId);
		final Path payloadPath = verificationCardSetPath.resolve(PAYLOAD_FILE_NAME);

		try {
			final byte[] payloadBytes = objectMapper.writeValueAsBytes(setupComponentLVCCAllowListPayload);

			final Path writePath = Files.write(payloadPath, payloadBytes);
			LOGGER.debug("Successfully persisted setup component LVCC allow list payload. [electionEventId: {}, path: {}]", electionEventId,
					payloadPath);

			return writePath;
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to serialize setup component LVCC allow list payload. [electionEventId: %s, path: %s]",
							electionEventId,
							payloadPath), e);
		}
	}

	/**
	 * Checks if the setup component LVCC allow list payload file exists for the given election event id and verification card set id.
	 *
	 * @param electionEventId       the election event id to check. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return {@code true} if the setup component LVCC allow list payload file exists, {@code false} otherwise.
	 * @throws NullPointerException      if {@code electionEventId} or {@code verificationCardSetId} is null.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is not a valid UUID.
	 */
	public boolean existsByElectionEventIdAndVerificationCardSetId(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final Path verificationCardSetPath = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId);
		final Path payloadPath = verificationCardSetPath.resolve(PAYLOAD_FILE_NAME);
		LOGGER.debug(
				"Checking setup component LVCC allow list payload file existence. [electionEventId: {}, verificationCardSetId: {}, path: {}]",
				electionEventId, verificationCardSetId, payloadPath);

		return Files.exists(payloadPath);
	}

	/**
	 * Retrieves from the file system a setup component LVCC allow list payload by election event id and verification card set id.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return the setup component LVCC allow list payload with the given ids or {@link Optional#empty} if none found.
	 * @throws NullPointerException      if {@code electionEventId} or {@code verificationCardSetId} is null.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is not a valid UUID.
	 * @throws UncheckedIOException      if the deserialization of the setup component LVCC allow list payload fails.
	 */
	public Optional<SetupComponentLVCCAllowListPayload> findByElectionEventIdAndVerificationCardSetId(final String electionEventId,
			final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final Path verificationCardSetPath = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId);
		final Path payloadPath = verificationCardSetPath.resolve(PAYLOAD_FILE_NAME);

		if (!Files.exists(payloadPath)) {
			LOGGER.debug(
					"Requested setup component LVCC allow list payload does not exist. [electionEventId: {}, verificationCardSetId: {}, path: {}]",
					electionEventId, verificationCardSetId, payloadPath);
			return Optional.empty();
		}

		try {
			return Optional.of(objectMapper.readValue(payloadPath.toFile(), SetupComponentLVCCAllowListPayload.class));
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format(
							"Failed to deserialize setup component LVCC allow list payload. [electionEventId: %s, verificationCardSetId: %s, path: %s]",
							electionEventId, verificationCardSetId, payloadPath), e);
		}
	}

}
