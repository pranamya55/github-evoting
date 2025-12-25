/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_NAME_VOTER_INITIAL_CODES_PAYLOAD;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodesPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

/**
 * Allows performing operations with the voter initial codes payload. The voter initial codes payload is persisted/retrieved to/from the file system
 * of the SDM, in its workspace.
 */
@Repository
@ConditionalOnProperty("role.isSetup")
public class VoterInitialCodesPayloadFileRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(VoterInitialCodesPayloadFileRepository.class);

	private final ObjectMapper objectMapper;
	private final PathResolver pathResolver;

	public VoterInitialCodesPayloadFileRepository(
			final ObjectMapper objectMapper,
			final PathResolver pathResolver) {
		this.objectMapper = objectMapper;
		this.pathResolver = pathResolver;
	}

	/**
	 * Persists a voter initial codes payload to the file system.
	 *
	 * @param voterInitialCodesPayload the voter initial codes payload to persist. Must be non-null.
	 * @param verificationCardSetId    the verification card set id. Must be non-null and a valid UUID.
	 * @return the path where the voter initial codes payload has been successfully persisted.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if the verification card set id is not a valid UUID.
	 * @throws UncheckedIOException      if the serialization of the voter initial codes payload fails.
	 */
	public Path save(final VoterInitialCodesPayload voterInitialCodesPayload, final String verificationCardSetId) {
		checkNotNull(voterInitialCodesPayload);
		validateUUID(verificationCardSetId);

		final String electionEventId = voterInitialCodesPayload.electionEventId();

		final Path verificationCardSetIdPath = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId);
		final Path payloadPath = verificationCardSetIdPath.resolve(CONFIG_FILE_NAME_VOTER_INITIAL_CODES_PAYLOAD);
		try {
			final byte[] payloadBytes = objectMapper.writeValueAsBytes(voterInitialCodesPayload);

			final Path writePath = Files.write(payloadPath, payloadBytes);
			LOGGER.info("Successfully persisted voter initial codes payload. [electionEventId: {}, verificationCardSetId: {}, path: {}]",
					electionEventId, verificationCardSetId, payloadPath);

			return writePath;
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to serialize voter initial codes payload. [electionEventId: %s, verificationCardSetId: %s, path: %s]",
							electionEventId, verificationCardSetId, payloadPath), e);
		}
	}

	/**
	 * Retrieves from the file system a voter initial codes payload by election event id and verification card set id.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return the voter initial codes payload with the given id or {@link Optional#empty} if none found.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if any parameter is not a valid UUID.
	 * @throws UncheckedIOException      if the deserialization of the voter initial codes payload fails.
	 */
	public Optional<VoterInitialCodesPayload> findByElectionEventIdAndVerificationCardSetId(final String electionEventId,
			final String verificationCardSetId) {

		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final Path payloadPath = getPayloadPath(electionEventId, verificationCardSetId);
		if (!Files.exists(payloadPath)) {
			LOGGER.debug("Requested voter initial codes payload does not exist. [electionEventId: {}, verificationCardSetId: {}, path: {}]",
					electionEventId, verificationCardSetId, payloadPath);
			return Optional.empty();
		}

		return Optional.of(deserializeVoterInitialCodesPayload(electionEventId, payloadPath));
	}

	/**
	 * Retrieves from the file system all voter initial codes payloads by election event id.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the list of voter initial codes payloads, empty if none found.
	 * @throws NullPointerException      if the election event id is null.
	 * @throws FailedValidationException if the election event id is not a valid UUID.
	 * @throws UncheckedIOException      if the deserialization of the voter initial codes payloads fails.
	 */
	public ImmutableList<VoterInitialCodesPayload> findAllByElectionEventId(final String electionEventId) {
		validateUUID(electionEventId);

		final ImmutableList<Path> verificationCardSetIdPaths;
		final Path printingPath = pathResolver.resolveVerificationCardSetsPath(electionEventId);
		try (final Stream<Path> paths = Files.walk(printingPath, 1)) {
			verificationCardSetIdPaths = paths
					.filter(path -> !printingPath.equals(path))
					.filter(Files::isDirectory)
					.collect(toImmutableList());
		} catch (final IOException e) {
			LOGGER.info("Unable to find voter initial codes payload. [electionEventId: {}, path: {}]", electionEventId, printingPath);
			return ImmutableList.emptyList();
		}

		return verificationCardSetIdPaths.stream().parallel()
				.map(verificationCardSetIdPath -> verificationCardSetIdPath.resolve(CONFIG_FILE_NAME_VOTER_INITIAL_CODES_PAYLOAD))
				.map(payloadPath -> deserializeVoterInitialCodesPayload(electionEventId, payloadPath))
				.collect(toImmutableList());
	}

	private VoterInitialCodesPayload deserializeVoterInitialCodesPayload(final String electionEventId, final Path payloadPath) {
		try {
			return objectMapper.readValue(payloadPath.toFile(), VoterInitialCodesPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to deserialize voter initial codes payload. [electionEventId: %s, path: %s]", electionEventId, payloadPath),
					e);
		}
	}

	private Path getPayloadPath(final String electionEventId, final String verificationCardSetId) {
		return pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId)
				.resolve(CONFIG_FILE_NAME_VOTER_INITIAL_CODES_PAYLOAD);
	}

}
