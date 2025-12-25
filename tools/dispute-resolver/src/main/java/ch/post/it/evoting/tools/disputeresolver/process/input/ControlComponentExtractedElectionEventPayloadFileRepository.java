/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.input;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.tools.disputeresolver.process.PathService.getNodeId;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.tools.disputeresolver.process.PathService;

@Repository
public class ControlComponentExtractedElectionEventPayloadFileRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(ControlComponentExtractedElectionEventPayloadFileRepository.class);

	private final ObjectMapper objectMapper;
	private final PathService pathService;

	public ControlComponentExtractedElectionEventPayloadFileRepository(final ObjectMapper objectMapper, final PathService pathService) {
		this.objectMapper = objectMapper;
		this.pathService = pathService;
	}

	/**
	 * Finds all control component extracted election event payloads in the file system. This method does not validate the signatures of the
	 * payloads.
	 *
	 * @return an immutable list of control component extracted election event payloads.
	 * @throws UncheckedIOException  if an I/O error occurs while reading the payloads from the file system.
	 * @throws IllegalStateException if any payload's file name does not match the node id in the payload.
	 */
	public ImmutableList<ControlComponentExtractedElectionEventPayload> findAll() {
		LOGGER.debug("Finding all control component extracted election event payloads...");

		final ImmutableList<ControlComponentExtractedElectionEventPayload> payloads = pathService.getControlComponentExtractedElectionEventPayloadsPaths()
				.stream()
				.map(this::findByPath)
				.collect(toImmutableList());

		LOGGER.debug("Successfully found all control component extracted election event payloads.");

		return payloads;
	}

	private ControlComponentExtractedElectionEventPayload findByPath(final Path controlComponentExtractedElectionEventPayloadPath) {
		checkNotNull(controlComponentExtractedElectionEventPayloadPath);

		LOGGER.debug("Loading control component extracted election event payload... [path: {}]", controlComponentExtractedElectionEventPayloadPath);

		final ControlComponentExtractedElectionEventPayload payload;
		try {
			payload = objectMapper.readValue(controlComponentExtractedElectionEventPayloadPath.toFile(),
					ControlComponentExtractedElectionEventPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Unable to deserialize control component extracted election event payload. [path: %s]",
					controlComponentExtractedElectionEventPayloadPath), e);
		}

		LOGGER.debug("Successfully deserialized control component extracted election event payload. [path: {}]",
				controlComponentExtractedElectionEventPayloadPath);

		final int fileNameNodeId = getNodeId(controlComponentExtractedElectionEventPayloadPath);
		checkState(fileNameNodeId == payload.getNodeId(),
				"The control component extracted election event payload file name does not match the node id in the payload. [path: %s, payloadNodeId: %s, fileNameNodeId: %s]",
				controlComponentExtractedElectionEventPayloadPath, payload.getNodeId(), fileNameNodeId);

		LOGGER.debug("Node id in control component extracted election event payload matches file name. [path: {}]",
				controlComponentExtractedElectionEventPayloadPath);

		return payload;
	}

}
