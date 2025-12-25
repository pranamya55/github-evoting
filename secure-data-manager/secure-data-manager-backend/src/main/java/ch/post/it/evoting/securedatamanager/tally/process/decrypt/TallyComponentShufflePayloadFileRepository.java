/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.decrypt;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.evotinglibraries.domain.mixnet.TallyComponentShufflePayload;
import ch.post.it.evoting.securedatamanager.shared.Constants;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

@Repository
@ConditionalOnProperty("role.isTally")
public class TallyComponentShufflePayloadFileRepository {

	private final String fileName;
	private final ObjectMapper objectMapper;
	private final PathResolver payloadResolver;

	@Autowired
	public TallyComponentShufflePayloadFileRepository(
			@Value("${finalPayload.fileName:tallyComponentShufflePayload}")
			final String fileName, final ObjectMapper objectMapper, final PathResolver payloadResolver) {

		this.fileName = fileName;
		this.objectMapper = objectMapper;
		this.payloadResolver = payloadResolver;
	}

	/**
	 * Gets the mix net payload stored on the filesystem for the given election and ballot box ids.
	 *
	 * @param electionEventId valid election event id. Must be non-null.
	 * @param ballotBoxId     valid ballot box id. Must be non-null.
	 * @return the {@link TallyComponentShufflePayload} object read from the stored file.
	 * @see PathResolver to get the resolved file Path.
	 */
	public TallyComponentShufflePayload getPayload(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		final Path payloadPath = payloadPath(electionEventId, ballotBoxId);

		try {
			return objectMapper.readValue(payloadPath.toFile(), TallyComponentShufflePayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Unable to read the tally component shuffle payload file. [electionEventID: %s, ballotBoxId: %s]", electionEventId,
							ballotBoxId), e);
		}
	}

	/**
	 * Saves the mix net payload to the filesystem for the given election and ballot box ids combination.
	 *
	 * @param electionEventId valid election event id. Must be non-null.
	 * @param ballotBoxId     valid ballot box id. Must be non-null.
	 * @param payload         the {@link TallyComponentShufflePayload} to persist.
	 * @return the path of the saved file.
	 * @see PathResolver to get the resolved file Path.
	 */
	public Path savePayload(final String electionEventId, final String ballotBoxId, final TallyComponentShufflePayload payload) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		checkNotNull(payload);

		final Path payloadPath = payloadPath(electionEventId, ballotBoxId);

		try {
			final Path filePath = Files.createFile(payloadPath);
			objectMapper.writeValue(filePath.toFile(), payload);
			return filePath;
		} catch (final IOException e) {
			throw new UncheckedIOException("Unable to write the mix net payload file", e);
		}
	}

	@VisibleForTesting
	Path payloadPath(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		final Path ballotBoxPath = payloadResolver.resolveBallotBoxPath(electionEventId, ballotBoxId);
		return ballotBoxPath.resolve(fileName + Constants.JSON);
	}
}
