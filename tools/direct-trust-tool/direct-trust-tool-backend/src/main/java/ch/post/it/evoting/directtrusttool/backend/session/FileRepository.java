/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.session;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;

@Repository
public class FileRepository {

	private final Path outputDirectory;

	public FileRepository(
			@Value("${app.directory.output}")
			final String outputDirectory) {
		this.outputDirectory = Path.of(outputDirectory);
	}

	public Optional<ImmutableByteArray> readFile(final Path fileName) {
		checkNotNull(fileName);

		final Path path = outputDirectory.resolve(fileName);
		if (Files.notExists(path)) {
			return Optional.empty();
		}
		try {
			return Optional.of(new ImmutableByteArray(Files.readAllBytes(path)));
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void writeFile(final Path fileName, final ImmutableByteArray content) {
		checkNotNull(fileName);
		checkNotNull(content);

		final Path path = ensurePathExists(outputDirectory.resolve(fileName));
		try {
			Files.write(path, content.elements());
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void createDirectory(final Path directory) {
		checkNotNull(directory);

		try {
			Files.createDirectories(outputDirectory.resolve(directory));
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void removeDirectory(final Path directory) {
		final Path path = outputDirectory.resolve(directory);
		if (Files.exists(path)) {
			// Traverse the directory from the bottom up and delete all files and directories
			try (final Stream<Path> paths = Files.walk(path).sorted(Comparator.reverseOrder())) {
				paths.forEach(streamedPath -> {
					try {
						Files.delete(streamedPath);
					} catch (final IOException e) {
						throw new UncheckedIOException(e);
					}
				});
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private Path ensurePathExists(final Path filePath) {
		final Path directoryPath = filePath.getParent();
		try {
			if (!Files.exists(directoryPath)) {
				Files.createDirectories(directoryPath);
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		return filePath;
	}
}
