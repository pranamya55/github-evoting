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
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.tools.disputeresolver.process.PathService;

@Repository
public class ControlComponentExtractedVerificationCardsPayloadFileRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(ControlComponentExtractedVerificationCardsPayloadFileRepository.class);

	private final ObjectMapper objectMapper;
	private final PathService pathService;

	public ControlComponentExtractedVerificationCardsPayloadFileRepository(final ObjectMapper objectMapper, final PathService pathService) {
		this.objectMapper = objectMapper;
		this.pathService = pathService;
	}

	/**
	 * Finds all control component extracted verification cards payloads in the file system. This method does not validate the signatures of the
	 * payloads.
	 *
	 * @return an immutable list of control component extracted verification cards payloads.
	 * @throws UncheckedIOException             if an I/O error occurs while reading the payloads from the file system.
	 * @throws IllegalStateException            if any payload's file name does not match the node id in the payload.
	 */
	public ImmutableList<ControlComponentExtractedVerificationCardsPayload> findAll() {
		LOGGER.debug("Finding all control component extracted verification cards payloads...");

		final ImmutableList<ControlComponentExtractedVerificationCardsPayload> payloads = pathService.getControlComponentExtractedVerificationCardsPayloadsPaths().stream()
				.map(this::findByPath)
				.collect(toImmutableList());

		LOGGER.debug("Successfully found all control component extracted verification cards payloads.");

		return payloads;
	}

	private ControlComponentExtractedVerificationCardsPayload findByPath(final Path controlComponentExtractedVerificationCardsPayloadPath) {
		checkNotNull(controlComponentExtractedVerificationCardsPayloadPath);

		LOGGER.debug("Loading control component extracted verification cards payload... [path: {}]",
				controlComponentExtractedVerificationCardsPayloadPath);

		final ControlComponentExtractedVerificationCardsPayload payload;
		try {
			payload = objectMapper.readValue(controlComponentExtractedVerificationCardsPayloadPath.toFile(),
					ControlComponentExtractedVerificationCardsPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Unable to deserialize control component extracted verification cards payload. [path: %s]",
					controlComponentExtractedVerificationCardsPayloadPath), e);
		}

		LOGGER.debug("Successfully deserialized control component extracted verification cards payload. [path: {}]",
				controlComponentExtractedVerificationCardsPayloadPath);

		final int fileNameNodeId = getNodeId(controlComponentExtractedVerificationCardsPayloadPath);
		checkState(fileNameNodeId == payload.getNodeId(),
				"The control component extracted verification cards payload file name does not match the node id in the payload. [path: %s, payloadNodeId: %s, fileNameNodeId: %s]",
				controlComponentExtractedVerificationCardsPayloadPath, payload.getNodeId(), fileNameNodeId);

		LOGGER.debug("Node id in control component extracted verification cards payload matches file name. [path: {}]",
				controlComponentExtractedVerificationCardsPayloadPath);

		return payload;
	}

}
