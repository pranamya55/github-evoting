/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CompressionServiceTest {

	private CompressionService compressionService;

	@TempDir
	private Path testDirectory;

	@BeforeEach
	void setUp() {
		compressionService = new CompressionService();
	}

	@Test
	void zipAndUnzip() throws IOException {

		// given
		final Path inputDirectory = testDirectory.resolve("input");
		final Path outputDirectory = testDirectory.resolve("output");
		final Path inputDirectoryCopy = testDirectory.resolve("inputCopy");

		Files.createDirectories(inputDirectory);
		Files.createDirectories(outputDirectory);

		createTestFile(inputDirectory.resolve("1.txt"));
		createTestFile(inputDirectory.resolve("2.txt"));
		createTestFile(inputDirectory.resolve("3/31.txt"));
		createTestFile(inputDirectory.resolve("3/32.txt"));
		createTestFile(inputDirectory.resolve("33/331.txt"));

		// Copy input because zipDirectory deletes the directory once it's done.
		copyFolder(inputDirectory, inputDirectoryCopy);

		// when
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		compressionService.zipDirectory(byteArrayOutputStream, inputDirectory);

		final InputStream targetStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		compressionService.unzipToDirectory(targetStream, outputDirectory);

		// then
		assertDirectoryContent(outputDirectory, inputDirectoryCopy);
		assertDirectoryContent(inputDirectoryCopy, outputDirectory);
	}

	private static void assertDirectoryContent(final Path expectedStructure, final Path actualStructure) throws IOException {
		try (final Stream<Path> files = Files.walk(expectedStructure)) {
			assertTrue(files.filter(Files::isRegularFile).allMatch(p -> Files.exists(actualStructure.resolve(expectedStructure.relativize(p)))));
		}
	}

	private void createTestFile(final Path file) throws IOException {
		Files.createDirectories(file.getParent());
		Files.createFile(file);
	}

	public void copyFolder(final Path src, final Path dest) throws IOException {
		try (final Stream<Path> stream = Files.walk(src)) {
			stream.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
		}
	}

	private void copy(final Path source, final Path dest) {
		try {
			Files.copy(source, dest, REPLACE_EXISTING);
		} catch (final Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
