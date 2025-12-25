/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.requestcckeys;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

/**
 * Allows performing operations with the control component public keys payloads. The payloads are persisted/retrieved to/from the file system of the
 * SDM, in its workspace.
 */
@Repository
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class ControlComponentPublicKeysPayloadFileRepository {

	@VisibleForTesting
	static final String PAYLOAD_FILE_NAME = "controlComponentPublicKeysPayload.%s.json";

	private static final Logger LOGGER = LoggerFactory.getLogger(ControlComponentPublicKeysPayloadFileRepository.class);

	private final ObjectMapper objectMapper;
	private final PathResolver pathResolver;

	public ControlComponentPublicKeysPayloadFileRepository(
			final ObjectMapper objectMapper, final PathResolver pathResolver) {
		this.objectMapper = objectMapper;
		this.pathResolver = pathResolver;
	}

	/**
	 * Persists a control component public keys payload to the file system.
	 *
	 * @param controlComponentPublicKeysPayload the payload to persist.
	 * @return the path where the payload has been successfully persisted.
	 * @throws NullPointerException if {@code controlComponentPublicKeysPayload} is null.
	 * @throws UncheckedIOException if the serialization of the payload fails.
	 */
	public Path save(final ControlComponentPublicKeysPayload controlComponentPublicKeysPayload) {
		checkNotNull(controlComponentPublicKeysPayload);

		final String electionEventId = controlComponentPublicKeysPayload.getElectionEventId();
		final int nodeId = controlComponentPublicKeysPayload.getControlComponentPublicKeys().nodeId();

		final Path electionEventPath = pathResolver.resolveElectionEventPath(electionEventId);
		final Path payloadPath = electionEventPath.resolve(String.format(PAYLOAD_FILE_NAME, nodeId));

		try {
			final byte[] payloadBytes = objectMapper.writeValueAsBytes(controlComponentPublicKeysPayload);
			final Path writePath = Files.write(payloadPath, payloadBytes);
			LOGGER.debug("Successfully persisted control component public keys payloads. [electionEventId: {}, nodeId: {}, path: {}]",
					electionEventId, nodeId, payloadPath);

			return writePath;
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to serialize control component public keys payload. [electionEventId: %s, nodeId: %s, path: %s]",
							electionEventId, nodeId, payloadPath), e);
		}
	}

	/**
	 * Checks if the control component public keys payload file exists for the given {@code electionEventId} and {@code nodeId}.
	 *
	 * @param electionEventId the election event id to check.
	 * @param nodeId          the node id to check.
	 * @return {@code true} if the payload file exists, {@code false} otherwise.
	 * @throws FailedValidationException if {@code electionEventId} is invalid.
	 */
	public boolean existsById(final String electionEventId, final int nodeId) {
		validateUUID(electionEventId);
		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);

		final Path electionEventPath = pathResolver.resolveElectionEventPath(electionEventId);
		final Path payloadPath = electionEventPath.resolve(String.format(PAYLOAD_FILE_NAME, nodeId));
		LOGGER.debug("Checking control component public keys payload file existence. [electionEventId: {}, nodeId: {}, path: {}]", electionEventId,
				nodeId, payloadPath);

		return Files.exists(payloadPath);
	}

}
