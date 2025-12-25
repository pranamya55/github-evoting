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

import ch.post.it.evoting.domain.configuration.SetupComponentVerificationCardKeystoresPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.Constants;

/**
 * Allows performing operations with the setup component verification card keystores payload. The payload is persisted/retrieved to/from the file
 * system of the SDM, in its workspace.
 */
@Repository
public class SetupComponentVerificationCardKeystoresPayloadFileRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentVerificationCardKeystoresPayloadFileRepository.class);

	private final ObjectMapper objectMapper;
	private final PathResolver payloadResolver;

	public SetupComponentVerificationCardKeystoresPayloadFileRepository(final ObjectMapper objectMapper, final PathResolver payloadResolver) {
		this.objectMapper = objectMapper;
		this.payloadResolver = payloadResolver;
	}

	/**
	 * Saves a setup component verification card keystores payload for the given {@code electionEventId} and {@code verificationCardSetId}.
	 *
	 * @param setupComponentVerificationCardKeystoresPayload thesetup component verification card keystores payload to save.
	 * @throws FailedValidationException if any of {@code electionEventId}, {@code verificationCardSetId}, {@code verificationCardId} are invalid.
	 * @throws NullPointerException      if any of {@code electionEventId}, {@code verificationCardSetId}, {@code verificationCardId}, {@code value}
	 *                                   is null.
	 */
	public Path save(final SetupComponentVerificationCardKeystoresPayload setupComponentVerificationCardKeystoresPayload) {
		checkNotNull(setupComponentVerificationCardKeystoresPayload);

		final String electionEventId = setupComponentVerificationCardKeystoresPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentVerificationCardKeystoresPayload.getVerificationCardSetId();

		final Path payloadPath = getPayloadPath(electionEventId, verificationCardSetId);
		try {
			final byte[] payloadBytes = objectMapper.writeValueAsBytes(setupComponentVerificationCardKeystoresPayload);

			final Path writePath = Files.write(payloadPath, payloadBytes);
			LOGGER.debug(
					"Successfully persisted setup component verification card keystores payload. [electionEventId: {}, verificationCardSetId: {}, path: {}]",
					electionEventId, verificationCardSetId, payloadPath);

			return writePath;
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format(
							"Failed to serialize setup component verification card keystores payload. [electionEventId: %s, verificationCardSetId: %s, path: %s]",
							electionEventId, verificationCardSetId, payloadPath), e);
		}
	}

	/**
	 * Checks if the setup component verification card keystores payload file exists for the given {@code electionEventId} and
	 * {@code verificationCardSetId}.
	 *
	 * @param electionEventId       the election event id to check. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return {@code true} if the setup component verification card keystores payload file exists, {@code false} otherwise.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if any input is not a valid UUID.
	 */
	public boolean existsById(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final Path payloadPath = getPayloadPath(electionEventId, verificationCardSetId);
		LOGGER.debug(
				"Checking setup component verification card keystores payload file existence. [electionEventId: {}, verificationCardSetId: {}, path: {}]",
				electionEventId, verificationCardSetId, payloadPath);

		return Files.exists(payloadPath);
	}

	/**
	 * Retrieves from the file system a setup component verification card keystores payload by election event and verification card set id.
	 *
	 * @param electionEventId       the payload's election event id.
	 * @param verificationCardSetId the payload's verification card set id.
	 * @return the payload with the given ids or {@link Optional#empty} if not found.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is invalid.
	 * @throws UncheckedIOException      if the deserialization of the payload fails.
	 */
	public Optional<SetupComponentVerificationCardKeystoresPayload> findById(final String electionEventId,
			final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final Path payloadPath = getPayloadPath(electionEventId, verificationCardSetId);
		if (!Files.exists(payloadPath)) {
			LOGGER.debug(
					"Requested setup component verification card keystores payload does not exist. [electionEventId: {}, verificationCardSetId: {}, path: {}]",
					electionEventId, verificationCardSetId, payloadPath);
			return Optional.empty();
		}

		try {
			return Optional.of(objectMapper.readValue(payloadPath.toFile(), SetupComponentVerificationCardKeystoresPayload.class));
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format(
							"Failed to deserialize setup component verification card keystores payload. [electionEventId: %s, verificationCardSetId: %s, path: %s]",
							electionEventId, verificationCardSetId, payloadPath), e);
		}
	}

	private Path getPayloadPath(final String electionEventId, final String verificationCardSetId) {
		final Path verificationCardSetPath = payloadResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId);
		return verificationCardSetPath.resolve(Constants.CONFIG_FILE_NAME_SETUP_COMPONENT_VERIFICATION_CARD_KEYSTORES_PAYLOAD);
	}
}
