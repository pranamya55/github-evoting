/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;

@Service
public class FileService {
	//Keystore
	public ImmutableMap<String, String> getAllFilesContentAsString(final Path directoryPath) {
		checkNotNull(directoryPath);
		checkArgument(Files.isDirectory(directoryPath), "The given path is not a directory. [path: %s]", directoryPath);

		final Map<String, String> fileContents = new HashMap<>();
		try (final Stream<Path> paths = Files.walk(directoryPath)) {
			paths.filter(Files::isRegularFile)
					.forEach(filePath -> {
						try {
							final String content = Files.readString(filePath, StandardCharsets.UTF_8);
							fileContents.put(filePath.getFileName().toString(), content);
						} catch (final IOException e) {
							throw new UncheckedIOException("Cannot read the files.", e);
						}
					});
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		return ImmutableMap.from(fileContents);
	}

	public void saveByteArrayAsZip(final ImmutableByteArray zipContent, final Path outputPath) {
		checkNotNull(zipContent);
		checkNotNull(outputPath);

		try {
			Files.createDirectories(outputPath.getParent());
			Files.write(outputPath, zipContent.elements());
		} catch (final IOException e) {
			throw new UncheckedIOException("Error while saving zip.", e);
		}
	}
}
