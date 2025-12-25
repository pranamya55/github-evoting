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

import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.Constants;

@Repository
public class SetupComponentVoterAuthenticationPayloadFileRepository {

	static final String PAYLOAD_FILE_NAME = "setupComponentVoterAuthenticationDataPayload" + Constants.JSON;

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentVoterAuthenticationPayloadFileRepository.class);

	private final ObjectMapper objectMapper;
	private final PathResolver pathResolver;

	public SetupComponentVoterAuthenticationPayloadFileRepository(
			final ObjectMapper objectMapper,
			final PathResolver pathResolver) {
		this.objectMapper = objectMapper;
		this.pathResolver = pathResolver;
	}

	/**
	 * Persists a setup component voter authentication payload to the file system.
	 *
	 * @param setupComponentVoterAuthenticationDataPayload the setup component voter authentication payload to persist. Must be non-null.
	 * @return the path where the setup component voter authentication payload has been successfully persisted.
	 * @throws NullPointerException if {@code setupComponentVoterAuthenticationDataPayload} is null.
	 * @throws UncheckedIOException if the serialization of the setup component voter authentication payload fails.
	 */
	public Path save(final SetupComponentVoterAuthenticationDataPayload setupComponentVoterAuthenticationDataPayload) {
		checkNotNull(setupComponentVoterAuthenticationDataPayload);

		final String electionEventId = setupComponentVoterAuthenticationDataPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentVoterAuthenticationDataPayload.getVerificationCardSetId();

		final Path verificationCardSetPath = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId);
		final Path payloadPath = verificationCardSetPath.resolve(PAYLOAD_FILE_NAME);

		try {
			final byte[] payloadBytes = objectMapper.writeValueAsBytes(setupComponentVoterAuthenticationDataPayload);

			final Path writePath = Files.write(payloadPath, payloadBytes);
			LOGGER.debug("Successfully persisted setup component voter authentication payload. [electionEventId: {}, path: {}]", electionEventId,
					payloadPath);

			return writePath;
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to serialize setup component voter authentication payload. [electionEventId: %s, path: %s]",
							electionEventId, payloadPath), e);
		}
	}

	/**
	 * Retrieves from the file system a setup component voter authentication payload by election event id and verification card set id.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return the setup component voter authentication payload with the given ids or {@link Optional#empty} if none found.
	 * @throws NullPointerException      if {@code electionEventId} or {@code verificationCardSetId} is null.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is not a valid UUID.
	 * @throws UncheckedIOException      if the deserialization of the setup component voter authentication payload fails.
	 */
	public Optional<SetupComponentVoterAuthenticationDataPayload> findByElectionEventIdAndVerificationCardSetId(final String electionEventId,
			final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final Path verificationCardSetPath = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId);
		final Path payloadPath = verificationCardSetPath.resolve(PAYLOAD_FILE_NAME);

		if (!Files.exists(payloadPath)) {
			LOGGER.debug(
					"Requested setup component voter authentication payload does not exist. [electionEventId: {}, verificationCardSetId: {}, path: {}]",
					electionEventId, verificationCardSetId, payloadPath);
			return Optional.empty();
		}

		try {
			return Optional.of(objectMapper.readValue(payloadPath.toFile(), SetupComponentVoterAuthenticationDataPayload.class));
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format(
							"Failed to deserialize setup component voter authentication payload. [electionEventId: %s, verificationCardSetId: %s, path: %s]",
							electionEventId, verificationCardSetId, payloadPath), e);
		}
	}

}
