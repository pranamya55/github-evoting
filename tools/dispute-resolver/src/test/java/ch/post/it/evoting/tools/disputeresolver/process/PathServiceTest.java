/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.domain.Constants.CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_NAME_FORMAT;
import static ch.post.it.evoting.domain.Constants.CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_NAME_FORMAT;
import static ch.post.it.evoting.tools.disputeresolver.process.PathService.getNodeId;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.internal.math.RandomService;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

@DisplayName("PathService")
class PathServiceTest {

	private static final RandomService randomService = new RandomService();
	private Path inputDirectory;
	private Path outputDirectory;

	@BeforeEach
	void setUp(
			@TempDir
			final Path inputDirectory,
			@TempDir
			final Path outputDirectory) {
		this.inputDirectory = inputDirectory;
		this.outputDirectory = outputDirectory;
	}

	@Nested
	@DisplayName("instantiated with")
	class InstantiationTests {
		@Test
		@DisplayName("valid paths instantiates as expected.")
		void instantiationHappyPath() {

			// Save the expected payloads in the input directory.
			final ImmutableList<Path> controlComponentExtractedElectionEventPayloadsPaths =
					savePayloads(CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_NAME_FORMAT, inputDirectory);
			final ImmutableList<Path> controlComponentExtractedVerificationCardsPayloadsPaths =
					savePayloads(CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_NAME_FORMAT, inputDirectory);

			final PathService pathService = assertDoesNotThrow(() -> new PathService(inputDirectory, outputDirectory));

			assertEquals(controlComponentExtractedElectionEventPayloadsPaths, pathService.getControlComponentExtractedElectionEventPayloadsPaths());
			assertEquals(controlComponentExtractedVerificationCardsPayloadsPaths.size(),
					pathService.getControlComponentExtractedVerificationCardsPayloadsPaths().size());
			assertTrue(controlComponentExtractedVerificationCardsPayloadsPaths.containsAll(pathService.getControlComponentExtractedVerificationCardsPayloadsPaths()));
			assertTrue(pathService.getControlComponentExtractedVerificationCardsPayloadsPaths().containsAll(controlComponentExtractedVerificationCardsPayloadsPaths));
		}

		@Test
		@DisplayName("a null path throws a NullPointerException.")
		void instantiationWithNullPathThrows() {
			assertThrows(NullPointerException.class, () -> new PathService(null, outputDirectory));
			assertThrows(NullPointerException.class, () -> new PathService(inputDirectory, null));
		}

		@Test
		@DisplayName("a non-existing path throws an UncheckedIOException.")
		void instantiationWithNonExistingPathThrows() {
			final Path unknown = Path.of("unknown");

			UncheckedIOException uncheckedIOException = assertThrows(UncheckedIOException.class,
					() -> new PathService(unknown, outputDirectory));
			assertEquals(String.format("The provided path does not exist. [path: %s]", unknown), uncheckedIOException.getMessage());

			uncheckedIOException = assertThrows(UncheckedIOException.class, () -> new PathService(inputDirectory, unknown));
			assertEquals(String.format("The provided path does not exist. [path: %s]", unknown), uncheckedIOException.getMessage());
		}

		@Test
		@DisplayName("a non-directory path throws an IllegalArgumentException.")
		void instantiationWithNonDirectoryPathThrows(
				@TempDir
				final Path someDirectory) throws IOException {

			final Path nonDirectoryPath = someDirectory.resolve("nonDirectory");
			Files.createFile(someDirectory.resolve(nonDirectoryPath));

			IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
					() -> new PathService(inputDirectory, nonDirectoryPath));
			assertEquals(String.format("The provided path is not a directory. [path: %s]", nonDirectoryPath), illegalArgumentException.getMessage());

			illegalArgumentException = assertThrows(IllegalArgumentException.class,
					() -> new PathService(nonDirectoryPath, outputDirectory));
			assertEquals(String.format("The provided path is not a directory. [path: %s]", nonDirectoryPath), illegalArgumentException.getMessage());
		}

		@Test
		@DisplayName("an input directory without the expected control component extracted election event payloads throws an IllegalStateException.")
		void instantiationWithInputDirectoryWithoutControlComponentExtractedElectionEventPayloadsThrows() {

			final int numberOfPayloadsToSave = randomService.genRandomInteger(ControlComponentNode.ids().size());

			// Save an unexpected number of payloads in the input directory.
			savePayloads(CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_NAME_FORMAT, inputDirectory, numberOfPayloadsToSave);
			savePayloads(CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_NAME_FORMAT, inputDirectory);

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> new PathService(inputDirectory, outputDirectory));

			assertEquals(String.format(
					"There must be exactly one control component extracted election event payload for each control component node. [expected: %s, found: %s]",
					ControlComponentNode.ids().size(), numberOfPayloadsToSave), illegalStateException.getMessage());
		}

		@Test
		@DisplayName("an input directory without the expected control component extracted verification cards payloads throws an IllegalStateException.")
		void instantiationWithInputDirectoryWithoutControlComponentExtractedVerificationCardsPayloadsThrows() {

			final int numberOfPayloadsToSave = randomService.genRandomInteger(ControlComponentNode.ids().size());

			// Save an unexpected number of payloads in the input directory.
			savePayloads(CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_NAME_FORMAT, inputDirectory);
			savePayloads(CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_NAME_FORMAT, inputDirectory, numberOfPayloadsToSave);

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> new PathService(inputDirectory, outputDirectory));

			assertEquals(String.format(
					"There must be exactly one control component extracted verification cards payload for each control component node. [expected: %s, found: %s]",
					ControlComponentNode.ids().size(), numberOfPayloadsToSave), illegalStateException.getMessage());
		}

		private static ImmutableList<Path> savePayloads(final String payloadNameFormat, final Path directory) {
			return savePayloads(payloadNameFormat, directory, ControlComponentNode.ids().size());
		}

		private static ImmutableList<Path> savePayloads(final String payloadNameFormat, final Path directory, final int numberOfPayloadsToSave) {
			return ControlComponentNode.ids().stream()
					.skip(ControlComponentNode.ids().size() - numberOfPayloadsToSave)
					.map(nodeId -> {
						final Path payloadPath = directory.resolve(String.format(payloadNameFormat, nodeId));
						try {
							Files.createFile(payloadPath);
							return payloadPath.toRealPath();
						} catch (final IOException e) {
							throw new UncheckedIOException(e);
						}
					})
					.collect(toImmutableList());
		}
	}

	@Nested
	@DisplayName("calling getNodeId with")
	class GetNodeIdTests {

		private Path tempDir;

		@BeforeEach
		void setUp(
				@TempDir
				final Path tempDir) {
			this.tempDir = tempDir;
		}

		@Test
		@DisplayName("a valid path returns the expected node id.")
		void getNodeIdFromPathHappyPath() {
			final int nodeId = randomService.genRandomInteger(ControlComponentNode.ids().size()) + 1;
			final Path path = createFile(String.format("file.%s.json", nodeId), tempDir);

			assertEquals(nodeId, getNodeId(path));
		}

		@Test
		@DisplayName("a null path throws a NullPointerException.")
		void getNodeIdWithNullPathThrows() {
			assertThrows(NullPointerException.class, () -> getNodeId(null));
		}

		@Test
		@DisplayName("an invalid path throws an IllegalArgumentException.")
		void getNodeIdWithPathWithoutNodeIdThrows() {
			final Path invalidPath = createFile("invalidFileName.json", tempDir);
			final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
					() -> getNodeId(invalidPath));

			assertEquals(String.format("The provided file name does not contain a node id. [path: %s, fileName: %s]", invalidPath,
							invalidPath.getFileName().toString()),
					illegalArgumentException.getMessage());
		}

		@Test
		@DisplayName("a path with invalid node id throws an IllegalArgumentException.")
		void getNodeIdWithPathWithInvalidNodeIdThrows() {
			final Path invalidPath = createFile("file.invalidNodeId.json", tempDir);
			final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
					() -> getNodeId(invalidPath));

			assertEquals(String.format("The provided file name does not contain a valid node id. [path: %s, fileName: %s]", invalidPath,
							invalidPath.getFileName().toString()),
					illegalArgumentException.getMessage());
		}

		@Test
		@DisplayName("a path with unknown node id throws an IllegalArgumentException.")
		void getNodeIdWithUnknownNodeIdThrows() {
			final int unknownNodeId = ControlComponentNode.ids().size() + 1;
			final Path path = createFile(String.format("file.%s.json", unknownNodeId), tempDir);
			final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
					() -> getNodeId(path));

			assertEquals(String.format("The provided file name contains an unknown node id. [path: %s, fileName: %s]", path,
							path.getFileName().toString()),
					illegalArgumentException.getMessage());
		}

		private static Path createFile(final String fileName, final Path directory) {
			final Path path = directory.resolve(fileName);
			try {
				Files.createFile(path);
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
			return path;
		}
	}
}
