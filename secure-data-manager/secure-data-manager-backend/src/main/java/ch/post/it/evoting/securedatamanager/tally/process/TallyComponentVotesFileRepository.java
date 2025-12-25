/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
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

import ch.post.it.evoting.evotinglibraries.domain.tally.TallyComponentVotesPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.Constants;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

@Repository
@ConditionalOnProperty("role.isTally")
public class TallyComponentVotesFileRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(TallyComponentVotesFileRepository.class);
	private static final String TALLY_COMPONENT_VOTES_FILENAME = "tallyComponentVotesPayload" + Constants.JSON;

	private final PathResolver pathResolver;
	private final ObjectMapper objectMapper;

	public TallyComponentVotesFileRepository(
			final PathResolver pathResolver,
			final ObjectMapper objectMapper) {
		this.pathResolver = pathResolver;
		this.objectMapper = objectMapper;
	}

	/**
	 * Checks if the file {@value TALLY_COMPONENT_VOTES_FILENAME} exists for the given parameters.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID
	 * @param ballotBoxId     the ballot box id. Must be non-null and a valid UUID
	 * @return true is the file exists
	 * @throws IllegalArgumentException if the ballot id, the ballot box id or the election event id is not a valid UUID.
	 */
	public boolean exists(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		final Path path = pathResolver.resolveBallotBoxPath(electionEventId, ballotBoxId).resolve(TALLY_COMPONENT_VOTES_FILENAME);
		return Files.exists(path);
	}

	/**
	 * Persists the payload as a file to the corresponding directory
	 *
	 * @param payload the payload to persist.
	 * @return the path where the file has been persisted.
	 * @throws IllegalStateException if the payload file already exists
	 */
	public Path save(final TallyComponentVotesPayload payload) {
		checkNotNull(payload);

		try {
			final Path payloadPath = pathResolver.resolveBallotBoxPath(payload.getElectionEventId(), payload.getBallotBoxId())
					.resolve(TALLY_COMPONENT_VOTES_FILENAME);
			final byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);
			final Path writePath = Files.write(payloadPath, payloadBytes);
			LOGGER.debug("Successfully persisted tally component votes payload. [electionEventId: {} ballotBoxId: {}, path: {}]",
					payload.getElectionEventId(), payload.getBallotBoxId(), payloadPath);
			return writePath;
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Failed to serialize tally component votes payload. [electionEventId: %s, ballotBoxId: %s]",
					payload.getElectionEventId(), payload.getBallotBoxId()), e);
		}
	}

	/**
	 * Reads the payload from file {@value TALLY_COMPONENT_VOTES_FILENAME}
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID
	 * @param ballotBoxId     the ballot box id. Must be non-null and a valid UUID
	 * @return the payload as {@link Optional}
	 * @throws NullPointerException      if the parameters are null
	 * @throws UncheckedIOException      if unable to deserialize the payload
	 * @throws FailedValidationException if the parameters are not valid UUID
	 */
	public Optional<TallyComponentVotesPayload> load(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		final Path filePath = pathResolver.resolveBallotBoxPath(electionEventId, ballotBoxId).resolve(TALLY_COMPONENT_VOTES_FILENAME);

		if (!Files.exists(filePath)) {
			LOGGER.debug("Requested tally component votes payload does not exist. [electionEventId: {}, ballotBoxId: {}, path: {}]", electionEventId,
					ballotBoxId, filePath);
			return Optional.empty();
		}

		try {
			return Optional.of(objectMapper.readValue(filePath.toFile(), TallyComponentVotesPayload.class));
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to deserialize tally component votes payload. [electionEventId: %s, ballotBoxId: %s, path: %s]",
							electionEventId, ballotBoxId, filePath), e);
		}
	}
}
