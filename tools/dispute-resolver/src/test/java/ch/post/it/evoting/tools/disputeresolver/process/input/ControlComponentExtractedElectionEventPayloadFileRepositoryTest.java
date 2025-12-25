/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.input;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.domain.Constants.CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_NAME_FORMAT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.internal.math.RandomService;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedElectionEventPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.tools.disputeresolver.process.PathService;

@DisplayName("ControlComponentExtractedElectionEventPayloadFileRepository calling findAll")
class ControlComponentExtractedElectionEventPayloadFileRepositoryTest {
	private static final ObjectMapper mapper = DomainObjectMapper.getNewInstance();
	private static final RandomService randomService = new RandomService();

	private final PathService pathService = mock(PathService.class);
	private ImmutableList<ControlComponentExtractedElectionEventPayload> controlComponentExtractedElectionEventPayloads;
	private ControlComponentExtractedElectionEventPayloadFileRepository controlComponentExtractedElectionEventPayloadFileRepository;
	private Path input;

	@BeforeEach
	void setUp(
			@TempDir
			final Path input) {
		this.input = input;
		controlComponentExtractedElectionEventPayloads = new ControlComponentExtractedElectionEventPayloadGenerator().generate();
	}

	@Test
	@DisplayName("behaves as expected.")
	void findAllHappyPath() {
		// Save all control component extracted election event payloads to the file system.
		final ImmutableList<Path> controlComponentExtractedElectionEventPayloadsPaths = controlComponentExtractedElectionEventPayloads.stream()
				.map(controlComponentExtractedElectionEventPayload -> savePayload(controlComponentExtractedElectionEventPayload, input))
				.collect(toImmutableList());

		when(pathService.getControlComponentExtractedElectionEventPayloadsPaths()).thenReturn(controlComponentExtractedElectionEventPayloadsPaths);

		controlComponentExtractedElectionEventPayloadFileRepository = new ControlComponentExtractedElectionEventPayloadFileRepository(mapper,
				pathService);

		final ImmutableList<ControlComponentExtractedElectionEventPayload> retrievedControlComponentExtractedElectionEventPayloads = assertDoesNotThrow(
				() -> controlComponentExtractedElectionEventPayloadFileRepository.findAll());

		assertTrue(
				controlComponentExtractedElectionEventPayloads.stream().allMatch(retrievedControlComponentExtractedElectionEventPayloads::contains));
		assertTrue(
				retrievedControlComponentExtractedElectionEventPayloads.stream().allMatch(controlComponentExtractedElectionEventPayloads::contains));
		assertEquals(retrievedControlComponentExtractedElectionEventPayloads.size(), controlComponentExtractedElectionEventPayloads.size());
	}

	@Test
	@DisplayName("throws an exception when the payload file name node id is invalid.")
	void findAllThrowsWhenPayloadFileNameNodeIdIsInvalid() {
		// Save all control component extracted election event payloads to the file system and intentionally use an invalid file name node id for some payloads.
		final int firstNodeIndexToSwap = randomService.genRandomInteger(ControlComponentNode.ids().size());
		int secondNodeIndexToSwap;
		do {
			secondNodeIndexToSwap = randomService.genRandomInteger(ControlComponentNode.ids().size());
		} while (secondNodeIndexToSwap == firstNodeIndexToSwap);

		final int firstNodeId = firstNodeIndexToSwap + 1;
		final int secondNodeId = secondNodeIndexToSwap + 1;

		Path firstNodePayloadPath = null;
		Path secondNodePayloadPath = null;
		final List<Path> controlComponentExtractedElectionEventPayloadsPaths = new ArrayList<>(ControlComponentNode.ids().size());
		for (final ControlComponentExtractedElectionEventPayload controlComponentExtractedElectionEventPayload : controlComponentExtractedElectionEventPayloads) {
			final int nodeId = controlComponentExtractedElectionEventPayload.getNodeId();

			if (nodeId == firstNodeId) {
				// Intentionally save a payload with an invalid file name node id.
				firstNodePayloadPath = input.resolve(String.format(CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_NAME_FORMAT, secondNodeId));
				controlComponentExtractedElectionEventPayloadsPaths.add(firstNodePayloadPath);
			} else if (nodeId == secondNodeId) {
				// Intentionally save a payload with an invalid file name node id.
				secondNodePayloadPath = input.resolve(String.format(CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_NAME_FORMAT, firstNodeId));
				controlComponentExtractedElectionEventPayloadsPaths.add(secondNodePayloadPath);
			} else {
				controlComponentExtractedElectionEventPayloadsPaths.add(
						input.resolve(String.format(CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_NAME_FORMAT, nodeId)));
			}

			try {
				Files.write(controlComponentExtractedElectionEventPayloadsPaths.getLast(),
						mapper.writeValueAsBytes(controlComponentExtractedElectionEventPayload));
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		when(pathService.getControlComponentExtractedElectionEventPayloadsPaths()).thenReturn(
				ImmutableList.from(controlComponentExtractedElectionEventPayloadsPaths));

		controlComponentExtractedElectionEventPayloadFileRepository = new ControlComponentExtractedElectionEventPayloadFileRepository(mapper,
				pathService);

		final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
				() -> controlComponentExtractedElectionEventPayloadFileRepository.findAll());

		final String firstExceptionMessage = String.format(
				"The control component extracted election event payload file name does not match the node id in the payload. [path: %s, payloadNodeId: %s, fileNameNodeId: %s]",
				secondNodePayloadPath, secondNodeId, firstNodeId);

		final String secondExceptionMessage = String.format(
				"The control component extracted election event payload file name does not match the node id in the payload. [path: %s, payloadNodeId: %s, fileNameNodeId: %s]",
				firstNodePayloadPath, firstNodeId, secondNodeId);

		assertTrue(illegalStateException.getMessage().equals(firstExceptionMessage) ||
				illegalStateException.getMessage().equals(secondExceptionMessage));
	}

	private Path savePayload(final ControlComponentExtractedElectionEventPayload controlComponentExtractedElectionEventPayload, final Path path) {
		final int nodeId = controlComponentExtractedElectionEventPayload.getNodeId();
		final Path payloadPath = path.resolve(String.format(CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_NAME_FORMAT, nodeId));
		try {
			Files.write(payloadPath, mapper.writeValueAsBytes(controlComponentExtractedElectionEventPayload));
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		return payloadPath;
	}

}
