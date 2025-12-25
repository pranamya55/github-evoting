/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_NAME_VERIFICATION_CARD_SECRET_KEY_PAYLOAD;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

/**
 * Allows performing operations with the verification card secret key payload. The verification card secret key payload is persisted/retrieved to/from
 * the file system of the SDM, in its workspace.
 */
@Repository
@ConditionalOnProperty("role.isSetup")
public class VerificationCardSecretKeyPayloadFileRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationCardSecretKeyPayloadFileRepository.class);
	final PathResolver pathResolver;
	private final ObjectMapper objectMapper;

	public VerificationCardSecretKeyPayloadFileRepository(final ObjectMapper objectMapper, final PathResolver pathResolver) {
		this.objectMapper = objectMapper;
		this.pathResolver = pathResolver;
	}

	/**
	 * Saves the verification card secret key payload for the given {@code electionEventId} and {@code verificationCardSetId}.
	 *
	 * @param verificationCardSecretKeyPayload the verification card secret key payload to save.
	 * @throws NullPointerException if {@code verificationCardSecretKeyPayload} is null.
	 * @throws UncheckedIOException if the serialization of the verification card secret key payload fails.
	 */
	public Path save(final VerificationCardSecretKeyPayload verificationCardSecretKeyPayload) {
		checkNotNull(verificationCardSecretKeyPayload);

		final String electionEventId = verificationCardSecretKeyPayload.electionEventId();
		final String verificationCardSetId = verificationCardSecretKeyPayload.verificationCardSetId();

		final Path payloadPath = getPayloadPath(electionEventId, verificationCardSetId);
		try {
			final byte[] payloadBytes = objectMapper.writeValueAsBytes(verificationCardSecretKeyPayload);

			final Path writePath = Files.write(payloadPath, payloadBytes);
			LOGGER.debug(
					"Successfully persisted verification card secret key payload. [electionEventId: {}, verificationCardSetId: {}, path: {}]",
					electionEventId, verificationCardSetId, payloadPath);

			return writePath;
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format(
							"Failed to serialize verification card secret key payload. [electionEventId: %s, verificationCardSetId: %s, path: %s]",
							electionEventId, verificationCardSetId, payloadPath), e);
		}
	}

	/**
	 * Retrieves from the file system a verification card secret key payload by election event id and verification card set id.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return the verification card secret key payload with the given id or {@link Optional#empty} if none found.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if any input is not a valid UUID.
	 * @throws UncheckedIOException      if the deserialization of the verification card secret key payload fails.
	 */
	public Optional<VerificationCardSecretKeyPayload> findById(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final Path payloadPath = getPayloadPath(electionEventId, verificationCardSetId);
		if (!Files.exists(payloadPath)) {
			LOGGER.debug("Requested verification card secret key does not exist. [electionEventId: {}, verificationCardSetId: {}, path: {}]",
					electionEventId, verificationCardSetId, payloadPath);
			return Optional.empty();
		}

		try {
			return Optional.of(objectMapper.readValue(payloadPath.toFile(), VerificationCardSecretKeyPayload.class));
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format(
							"Failed to deserialize verification card secret key. [electionEventId: %s, verificationCardSetId: %s, path: %s]",
							electionEventId, verificationCardSetId, payloadPath), e);
		}
	}

	private Path getPayloadPath(final String electionEventId, final String verificationCardSetId) {
		final Path verificationCardSetPath = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId);
		return verificationCardSetPath.resolve(String.format(CONFIG_FILE_NAME_VERIFICATION_CARD_SECRET_KEY_PAYLOAD));
	}
}
