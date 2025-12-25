/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import ch.post.it.evoting.domain.generators.ControlComponentShufflePayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.securedatamanager.shared.Constants;

@DisplayName("Use ControlComponentShufflePayloadFileRepository to ")
class ControlComponentShufflePayloadFileRepositoryTest {

	private static final int NODE_ID = 1;
	private static final Random random = new SecureRandom();

	private static ControlComponentShufflePayload expectedControlComponentShufflePayload;
	private static ControlComponentShufflePayloadFileRepository controlComponentShufflePayloadFileRepository;
	private static PathResolver payloadResolver;

	@TempDir
	Path tempDir;

	@BeforeAll
	static void setUpAll() {
		final int numVotes = random.nextInt(10) + 2; // Should be at least 2
		final int voteSize = random.nextInt(10) + 1;
		final ControlComponentShufflePayloadGenerator controlComponentShufflePayloadGenerator = new ControlComponentShufflePayloadGenerator();
		expectedControlComponentShufflePayload = controlComponentShufflePayloadGenerator.generate(NODE_ID, numVotes, voteSize);
		payloadResolver = Mockito.mock(PathResolver.class);
		controlComponentShufflePayloadFileRepository = new ControlComponentShufflePayloadFileRepository(DomainObjectMapper.getNewInstance(),
				payloadResolver);
	}

	@Test
	@DisplayName("read ControlComponentShufflePayload file")
	void readControlComponentShufflePayload() {
		// Mock payloadResolver path and write payload
		final String electionEventId = expectedControlComponentShufflePayload.getElectionEventId();
		final String ballotBoxId = expectedControlComponentShufflePayload.getBallotBoxId();
		when(payloadResolver.resolveBallotBoxPath(electionEventId, ballotBoxId)).thenReturn(tempDir);
		controlComponentShufflePayloadFileRepository.savePayload(expectedControlComponentShufflePayload);

		// Read payload and check
		final ControlComponentShufflePayload actualMixnetPayload = controlComponentShufflePayloadFileRepository.getPayload(electionEventId,
				ballotBoxId, NODE_ID);

		assertEquals(expectedControlComponentShufflePayload, actualMixnetPayload);
	}

	@Test
	@DisplayName("save ControlComponentShufflePayload file")
	void saveControlComponentShufflePayload() {
		// Mock payloadResolver path
		final String electionEventId = expectedControlComponentShufflePayload.getElectionEventId();
		final String ballotBoxId = expectedControlComponentShufflePayload.getBallotBoxId();
		when(payloadResolver.resolveBallotBoxPath(electionEventId, ballotBoxId)).thenReturn(tempDir);
		final Path expectedPayloadPath = tempDir.resolve("controlComponentShufflePayload_" + NODE_ID + Constants.JSON);

		assertFalse(Files.exists(expectedPayloadPath), "The mix net payload file should not exist at this point");

		// Write payload
		final Path actualPayloadPath = controlComponentShufflePayloadFileRepository.savePayload(expectedControlComponentShufflePayload);

		assertTrue(Files.exists(actualPayloadPath), "The mix net payload file should exist at this point");
		assertEquals(expectedPayloadPath, actualPayloadPath, "Both payload paths should resolve to the same file");
	}

}
