/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;
import ch.post.it.evoting.securedatamanager.shared.Constants;

@Repository
public class ControlComponentBallotBoxPayloadFileRepository {

	private static final String FILE_PREFIX = "controlComponentBallotBoxPayload_";

	private final ObjectMapper objectMapper;
	private final PathResolver payloadResolver;

	public ControlComponentBallotBoxPayloadFileRepository(final ObjectMapper objectMapper, final PathResolver payloadResolver) {
		this.objectMapper = objectMapper;
		this.payloadResolver = payloadResolver;
	}

	public ControlComponentBallotBoxPayload getPayload(final String electionEventId, final String ballotBoxId, final int nodeId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);

		final Path payloadPath = payloadPath(electionEventId, ballotBoxId, nodeId);

		try {
			return objectMapper.readValue(payloadPath.toFile(), ControlComponentBallotBoxPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Unable to read the control component ballot box payload file. [electionEventId: %s, ballotBoxId: %s, nodeId: %d]",
							electionEventId, ballotBoxId, nodeId), e);
		}
	}

	/**
	 * Saves the control component ballot box payload to the filesystem for the given election event, ballot box, control component combination.
	 *
	 * @param controlComponentBallotBoxPayloads the payloads to save.
	 * @throws NullPointerException     if any of the inputs is null.
	 * @throws IllegalArgumentException if any of the inputs is not valid.
	 * @see PathResolver to get the resolved file Path.
	 */
	public void saveAll(final ImmutableList<ControlComponentBallotBoxPayload> controlComponentBallotBoxPayloads) {
		checkNotNull(controlComponentBallotBoxPayloads);

		final int numberOfPayloads = controlComponentBallotBoxPayloads.size();
		final int expectedNumberOfPayloads = ControlComponentNode.ids().size();
		checkArgument(numberOfPayloads == expectedNumberOfPayloads, "Wrong number of payload to save. [actual: %s, expected: %s]", numberOfPayloads,
				expectedNumberOfPayloads);

		checkArgument(allEqual(controlComponentBallotBoxPayloads.stream(), ControlComponentBallotBoxPayload::getEncryptionGroup),
				"All payloads must have the same encryption group.");
		checkArgument(allEqual(controlComponentBallotBoxPayloads.stream(), ControlComponentBallotBoxPayload::getElectionEventId),
				"All payloads must have the same election event id.");
		checkArgument(allEqual(controlComponentBallotBoxPayloads.stream(), ControlComponentBallotBoxPayload::getBallotBoxId),
				"All payloads must have the same ballot box id.");

		controlComponentBallotBoxPayloads.stream().parallel()
				.forEach(controlComponentBallotBoxPayload -> {
					final String electionEventId = controlComponentBallotBoxPayload.getElectionEventId();
					final String ballotBoxId = controlComponentBallotBoxPayload.getBallotBoxId();
					final int nodeId = controlComponentBallotBoxPayload.getNodeId();

					final Path ballotBoxPath = payloadResolver.resolveBallotBoxPath(electionEventId, ballotBoxId);
					final Path payloadPath = ballotBoxPath.resolve(FILE_PREFIX + nodeId + Constants.JSON);

					try {
						final Path filePath = Files.createFile(payloadPath);
						objectMapper.writeValue(filePath.toFile(), controlComponentBallotBoxPayload);
					} catch (final IOException e) {
						throw new UncheckedIOException("Failed to write payload to file system.", e);
					}
				});
	}

	private Path payloadPath(final String electionEventId, final String ballotBoxId, final int nodeId) {
		final Path ballotBoxPath = payloadResolver.resolveBallotBoxPath(electionEventId, ballotBoxId);
		return ballotBoxPath.resolve(FILE_PREFIX + nodeId + Constants.JSON);
	}
}
