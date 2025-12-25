/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
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
import ch.post.it.evoting.domain.configuration.VoterReturnCodesPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.Constants;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

@Repository
@ConditionalOnProperty("role.isSetup")
public class VoterReturnCodesPayloadFileRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(VoterReturnCodesPayloadFileRepository.class);

	private final ObjectMapper objectMapper;
	private final PathResolver payloadResolver;

	public VoterReturnCodesPayloadFileRepository(
			final ObjectMapper objectMapper,
			final PathResolver payloadResolver) {
		this.objectMapper = objectMapper;
		this.payloadResolver = payloadResolver;
	}

	/**
	 * Saves the voter return codes payload to the filesystem for the given election event and verification card set.
	 *
	 * @return the path of the saved file.
	 * @throws NullPointerException     if any of the inputs is null.
	 * @throws IllegalArgumentException if any of the inputs is not valid.
	 * @see PathResolver to get the resolved file Path.
	 */
	public Path save(final VoterReturnCodesPayload payload, final String verificationCardSetId) {
		checkNotNull(payload);

		final String electionEventId = validateUUID(payload.electionEventId());
		validateUUID(verificationCardSetId);

		final Path payloadPath = payloadPath(electionEventId, verificationCardSetId);

		try {
			final byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);
			final Path filePath = Files.write(payloadPath, payloadBytes);

			LOGGER.info("Successfully persisted voter return codes payload. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
					verificationCardSetId);

			return filePath;
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Unable to write the voter return codes payload file. [electionEventId: %s, verificationCardSetId: %s]",
							electionEventId, verificationCardSetId), e);
		}
	}

	/**
	 * Retrieves from the file system a voter return codes payload by election event and verification card set ids.
	 *
	 * @param electionEventId       the payload's election event id.
	 * @param verificationCardSetId the payload's verification card set id.
	 * @return the voter return codes payload with the given ids or {@link Optional#empty} if not found.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is invalid.
	 * @throws UncheckedIOException      if the deserialization of the payload fails.
	 */
	public Optional<VoterReturnCodesPayload> findByElectionEventIdAndVerificationCardSetId(final String electionEventId,
			final String verificationCardSetId) {

		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final Path payloadPath = payloadPath(electionEventId, verificationCardSetId);

		if (!Files.exists(payloadPath)) {
			LOGGER.warn("Requested voter return codes payload does not exist. [electionEventId: {}, verificationCardSetId: {}]",
					electionEventId, verificationCardSetId);
			return Optional.empty();
		}

		return Optional.of(deserializeVoterReturnCodesPayload(electionEventId, payloadPath));
	}

	/**
	 * Retrieves from the file system all voter return codes payloads by election event id.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the list of voter return codes payloads, empty if none found.
	 * @throws NullPointerException      if the election event id is null.
	 * @throws FailedValidationException if the election event id is not a valid UUID.
	 * @throws UncheckedIOException      if the deserialization of the voter return codes payloads fails.
	 */
	public ImmutableList<VoterReturnCodesPayload> findAllByElectionEventId(final String electionEventId) {
		validateUUID(electionEventId);

		final ImmutableList<Path> votingCarSetIdPaths;
		final Path printingPath = payloadResolver.resolveVerificationCardSetsPath(electionEventId);
		try (final Stream<Path> paths = Files.walk(printingPath, 1)) {
			votingCarSetIdPaths = paths
					.filter(path -> !printingPath.equals(path))
					.filter(Files::isDirectory)
					.collect(toImmutableList());
		} catch (final IOException e) {
			LOGGER.warn("Unable to find voter return codes payload. [electionEventId: {}, path: {}]", electionEventId, printingPath);
			return ImmutableList.emptyList();
		}

		return votingCarSetIdPaths.stream().parallel()
				.map(votingCarSetIdPath -> votingCarSetIdPath.resolve(Constants.CONFIG_FILE_NAME_VOTER_RETURN_CODES_PAYLOAD))
				.map(payloadPath -> deserializeVoterReturnCodesPayload(electionEventId, payloadPath))
				.collect(toImmutableList());
	}

	private VoterReturnCodesPayload deserializeVoterReturnCodesPayload(final String electionEventId, final Path payloadPath) {
		try {
			return objectMapper.readValue(payloadPath.toFile(), VoterReturnCodesPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to deserialize voter return codes payload. [electionEventId: %s, path: %s]", electionEventId, payloadPath),
					e);
		}
	}

	private Path payloadPath(final String electionEventId, final String verificationCardSet) {
		return payloadResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSet)
				.resolve(Constants.CONFIG_FILE_NAME_VOTER_RETURN_CODES_PAYLOAD);
	}
}
