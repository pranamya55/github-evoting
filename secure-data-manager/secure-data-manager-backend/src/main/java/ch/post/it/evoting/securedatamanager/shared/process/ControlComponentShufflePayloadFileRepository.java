/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.securedatamanager.shared.Constants;

@Repository
public class ControlComponentShufflePayloadFileRepository {

	private static final String FILE_PREFIX = "controlComponentShufflePayload_";

	private final ObjectMapper objectMapper;
	private final PathResolver payloadResolver;

	public ControlComponentShufflePayloadFileRepository(final ObjectMapper objectMapper, final PathResolver payloadResolver) {
		this.objectMapper = objectMapper;
		this.payloadResolver = payloadResolver;
	}

	/**
	 * Gets the control component shuffle payload stored on the filesystem for the given election event, ballot box, control component combination.
	 *
	 * @return the ControlComponentShufflePayload object read from the stored file.
	 * @throws NullPointerException     if any of the inputs is null.
	 * @throws IllegalArgumentException if any of the inputs is not valid.
	 * @see PathResolver to get the resolved file Path.
	 */
	public ControlComponentShufflePayload getPayload(final String electionEventId, final String ballotBoxId, final int nodeId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);

		final Path payloadPath = payloadPath(electionEventId, ballotBoxId, nodeId);

		try {
			return objectMapper.readValue(payloadPath.toFile(), ControlComponentShufflePayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Unable to read the control component shuffle payload file. [electionEventId: %s, ballotBoxId: %s, nodeId: %d]",
							electionEventId, ballotBoxId, nodeId), e);
		}
	}

	/**
	 * Saves the control component shuffle payload to the filesystem for the given election event, ballot box, control component combination.
	 *
	 * @return the path of the saved file.
	 * @throws NullPointerException     if any of the inputs is null.
	 * @throws IllegalArgumentException if any of the inputs is not valid.
	 * @see PathResolver to get the resolved file Path.
	 */
	public Path savePayload(final ControlComponentShufflePayload payload) {
		checkNotNull(payload);

		final String electionEventId = payload.getElectionEventId();
		final String ballotBoxId = payload.getBallotBoxId();
		final int nodeId = payload.getNodeId();

		final Path payloadPath = payloadPath(electionEventId, ballotBoxId, nodeId);

		try {
			final Path filePath = Files.createFile(payloadPath);
			objectMapper.writeValue(filePath.toFile(), payload);
			return filePath;
		} catch (final IOException e) {
			throw new UncheckedIOException("Unable to write the mix net payload file", e);
		}
	}

	private Path payloadPath(final String electionEventId, final String ballotBoxId, final int nodeId) {
		final Path ballotBoxPath = payloadResolver.resolveBallotBoxPath(electionEventId, ballotBoxId);
		return ballotBoxPath.resolve(FILE_PREFIX + nodeId + Constants.JSON);
	}
}
