/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.input;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.domain.Constants.CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_NAME_FORMAT;
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
import ch.post.it.evoting.domain.generators.ControlComponentExtractedVerificationCardsPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.tools.disputeresolver.process.PathService;

@DisplayName("ControlComponentExtractedVerificationCardsPayloadFileRepositoryTest calling findAll")
class ControlComponentExtractedVerificationCardsPayloadFileRepositoryTest {
	private static final ObjectMapper mapper = DomainObjectMapper.getNewInstance();
	private static final RandomService randomService = new RandomService();

	private final PathService pathService = mock(PathService.class);
	private ImmutableList<ControlComponentExtractedVerificationCardsPayload> controlComponentExtractedVerificationCardsPayloads;
	private ControlComponentExtractedVerificationCardsPayloadFileRepository controlComponentExtractedVerificationCardsPayloadFileRepository;
	private Path input;

	@BeforeEach
	void setUp(
			@TempDir
			final Path input) {
		this.input = input;
		controlComponentExtractedVerificationCardsPayloads = new ControlComponentExtractedVerificationCardsPayloadGenerator().generate();
	}

	@Test
	@DisplayName("behaves as expected.")
	void findAllHappyPath() {
		// Save all control component extracted verification cards payloads to the file system.
		final ImmutableList<Path> controlComponentExtractedVerificationCardsPayloadsPaths = controlComponentExtractedVerificationCardsPayloads.stream()
				.map(controlComponentExtractedVerificationCardsPayload -> savePayload(controlComponentExtractedVerificationCardsPayload, input))
				.collect(toImmutableList());

		when(pathService.getControlComponentExtractedVerificationCardsPayloadsPaths()).thenReturn(
				controlComponentExtractedVerificationCardsPayloadsPaths);

		controlComponentExtractedVerificationCardsPayloadFileRepository = new ControlComponentExtractedVerificationCardsPayloadFileRepository(mapper,
				pathService);

		final ImmutableList<ControlComponentExtractedVerificationCardsPayload> retrievedControlComponentExtractedVerificationCardsPayloads = assertDoesNotThrow(
				() -> controlComponentExtractedVerificationCardsPayloadFileRepository.findAll());

		assertTrue(
				controlComponentExtractedVerificationCardsPayloads.stream()
						.allMatch(retrievedControlComponentExtractedVerificationCardsPayloads::contains));
		assertTrue(
				retrievedControlComponentExtractedVerificationCardsPayloads.stream()
						.allMatch(controlComponentExtractedVerificationCardsPayloads::contains));
		assertEquals(retrievedControlComponentExtractedVerificationCardsPayloads.size(), controlComponentExtractedVerificationCardsPayloads.size());
	}

	@Test
	@DisplayName("throws an exception when the payload file name node id is invalid.")
	void findAllThrowsWhenPayloadFileNameNodeIdIsInvalid() {
		// Save all control component extracted verification cards payloads to the file system and intentionally use an invalid file name node id for some payloads.
		final int firstNodeIndexToSwap = randomService.genRandomInteger(ControlComponentNode.ids().size());
		int secondNodeIndexToSwap;
		do {
			secondNodeIndexToSwap = randomService.genRandomInteger(ControlComponentNode.ids().size());
		} while (secondNodeIndexToSwap == firstNodeIndexToSwap);

		final int firstNodeId = firstNodeIndexToSwap + 1;
		final int secondNodeId = secondNodeIndexToSwap + 1;

		Path firstNodePayloadPath = null;
		Path secondNodePayloadPath = null;
		final List<Path> controlComponentVerificationCardsPayloadsPaths = new ArrayList<>(ControlComponentNode.ids().size());
		for (final ControlComponentExtractedVerificationCardsPayload controlComponentExtractedVerificationCardsPayload : controlComponentExtractedVerificationCardsPayloads) {
			final int nodeId = controlComponentExtractedVerificationCardsPayload.getNodeId();

			if (nodeId == firstNodeId) {
				// Intentionally save a payload with an invalid file name node id.
				firstNodePayloadPath = input.resolve(String.format(CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_NAME_FORMAT, secondNodeId));
				controlComponentVerificationCardsPayloadsPaths.add(firstNodePayloadPath);
			} else if (nodeId == secondNodeId) {
				// Intentionally save a payload with an invalid file name node id.
				secondNodePayloadPath = input.resolve(String.format(CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_NAME_FORMAT, firstNodeId));
				controlComponentVerificationCardsPayloadsPaths.add(secondNodePayloadPath);
			} else {
				controlComponentVerificationCardsPayloadsPaths.add(
						input.resolve(String.format(CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_NAME_FORMAT, nodeId)));
			}

			try {
				Files.write(controlComponentVerificationCardsPayloadsPaths.getLast(),
						mapper.writeValueAsBytes(controlComponentExtractedVerificationCardsPayload));
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		when(pathService.getControlComponentExtractedVerificationCardsPayloadsPaths()).thenReturn(
				ImmutableList.from(controlComponentVerificationCardsPayloadsPaths));

		controlComponentExtractedVerificationCardsPayloadFileRepository = new ControlComponentExtractedVerificationCardsPayloadFileRepository(mapper,
				pathService);

		final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
				() -> controlComponentExtractedVerificationCardsPayloadFileRepository.findAll());

		final String firstExceptionMessage = String.format(
				"The control component extracted verification cards payload file name does not match the node id in the payload. [path: %s, payloadNodeId: %s, fileNameNodeId: %s]",
		secondNodePayloadPath, secondNodeId, firstNodeId);

		final String secondExceptionMessage = String.format(
				"The control component extracted verification cards payload file name does not match the node id in the payload. [path: %s, payloadNodeId: %s, fileNameNodeId: %s]",
				firstNodePayloadPath, firstNodeId, secondNodeId);

		assertTrue(illegalStateException.getMessage().equals(firstExceptionMessage) ||
				illegalStateException.getMessage().equals(secondExceptionMessage));
	}

	private Path savePayload(final ControlComponentExtractedVerificationCardsPayload controlComponentExtractedVerificationCardsPayload,
			final Path path) {
		final int nodeId = controlComponentExtractedVerificationCardsPayload.getNodeId();
		final Path payloadPath = path.resolve(
				String.format(CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_NAME_FORMAT, nodeId));
		try {
			Files.write(payloadPath, mapper.writeValueAsBytes(controlComponentExtractedVerificationCardsPayload));
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		return payloadPath;
	}
}
