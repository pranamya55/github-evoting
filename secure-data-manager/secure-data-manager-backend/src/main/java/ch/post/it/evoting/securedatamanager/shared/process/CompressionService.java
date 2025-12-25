/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.attribute.AclEntryFlag.DIRECTORY_INHERIT;
import static java.nio.file.attribute.AclEntryFlag.FILE_INHERIT;
import static java.nio.file.attribute.AclEntryPermission.APPEND_DATA;
import static java.nio.file.attribute.AclEntryPermission.DELETE;
import static java.nio.file.attribute.AclEntryPermission.EXECUTE;
import static java.nio.file.attribute.AclEntryPermission.READ_ACL;
import static java.nio.file.attribute.AclEntryPermission.READ_ATTRIBUTES;
import static java.nio.file.attribute.AclEntryPermission.READ_DATA;
import static java.nio.file.attribute.AclEntryPermission.READ_NAMED_ATTRS;
import static java.nio.file.attribute.AclEntryPermission.SYNCHRONIZE;
import static java.nio.file.attribute.AclEntryPermission.WRITE_ATTRIBUTES;
import static java.nio.file.attribute.AclEntryPermission.WRITE_DATA;
import static java.nio.file.attribute.AclEntryPermission.WRITE_NAMED_ATTRS;
import static java.nio.file.attribute.AclEntryType.ALLOW;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;

/**
 * Zip and unzip files.
 */
@Service
public class CompressionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CompressionService.class);

	/**
	 * Zip the content of a given directory in a given ByteArrayOutputStream.
	 *
	 * @param outputStream   the output stream where the zip-file should be written. Must be non-null.
	 * @param directoryToZip the path to the directory's content which must be zipped. Must be non-null.
	 */
	public void zipDirectory(final OutputStream outputStream, final Path directoryToZip) throws IOException {

		checkNotNull(outputStream);
		checkNotNull(directoryToZip);
		checkArgument(Files.isDirectory(directoryToZip), "directoryToZip is not a directory. [%s]", directoryToZip);

		secureDirectory(directoryToZip);

		final ZipParameters zipParameters = new ZipParameters();
		zipParameters.setCompressionLevel(CompressionLevel.NO_COMPRESSION);

		try (final Stream<Path> paths = Files.walk(directoryToZip);
				final ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {

			paths.forEach(path -> {
				if (!Files.isDirectory(path)) {
					final String zipEntryName = directoryToZip.relativize(path).toString();
					LOGGER.debug("Zipping entry... [path: {}]", path);
					try (final InputStream inputStream = Files.newInputStream(path)) {
						zipParameters.setFileNameInZip(zipEntryName);
						zipOutputStream.putNextEntry(zipParameters);
						inputStream.transferTo(zipOutputStream);
						zipOutputStream.closeEntry();
					} catch (final IOException e) {
						throw new UncheckedIOException(String.format("Failed to zip directory. [directoryToZip: %s]", directoryToZip), e);
					}
				}
			});
			LOGGER.info("Successfully zipped directory. [directoryToZip: {}]", directoryToZip);

			deleteDirectory(directoryToZip);
		}
		LOGGER.debug("Streaming response...");
	}

	/**
	 * Unzip a given zip file as byte array to a given destinationDirectory.
	 *
	 * @param inputStream          the byte[] of the zip file. Must be non-null.
	 * @param destinationDirectory a directory which exists and is empty
	 */
	public void unzipToDirectory(final InputStream inputStream, final Path destinationDirectory) throws IOException {
		checkNotNull(inputStream);
		checkNotNull(destinationDirectory);
		checkArgument(Files.isDirectory(destinationDirectory), "destination is not an existing directory. [destinationDirectory : %s]",
				destinationDirectory);
		checkArgument(isDirEmpty(destinationDirectory), "destination directory is not empty. [destinationDirectory : %s]", destinationDirectory);

		secureDirectory(destinationDirectory);

		try (final ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
			LocalFileHeader entry;
			while ((entry = zipInputStream.getNextEntry()) != null) {
				final String fileName = entry.getFileName();
				final Path fileLocation = destinationDirectory.resolve(fileName);

				final String canonicalDestinationDirPath = destinationDirectory.toFile().getCanonicalPath();
				final String canonicalFileLocation = fileLocation.toFile().getCanonicalPath();

				if (canonicalFileLocation.startsWith(canonicalDestinationDirPath + File.separator)) {
					final Path directoryLocation = fileLocation.getParent();
					if (!Files.exists(directoryLocation)) {
						Files.createDirectories(directoryLocation);
						LOGGER.info("Directories created.[path: {}]", directoryLocation);
					}

					try (final OutputStream fileOutputStream = Files.newOutputStream(fileLocation)) {
						zipInputStream.transferTo(fileOutputStream);
					}

					LOGGER.debug("File successfully unzipped. [file: {}]", fileName);

				} else {
					LOGGER.warn("The zip file contains an unexpected file. [canonicalFileLocation:{}]", canonicalFileLocation);
				}
			}
		}

		LOGGER.info("Zip successfully unzipped.");
	}

	private static boolean isDirEmpty(final Path directory) throws IOException {
		try (final DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
			return !dirStream.iterator().hasNext();
		}
	}

	private void secureDirectory(final Path path) throws IOException {
		final String POSIX_FILE_ATTRIBUTE_VIEW = "posix";
		final String ACL_FILE_ATTRIBUTE_VIEW = "acl";
		final String POSIX_USER_ONLY_PERMISSION = "rwx------";

		final ImmutableSet<String> supportedFileAttributeViews = ImmutableSet.from(path.getFileSystem().supportedFileAttributeViews());
		if (supportedFileAttributeViews.contains(POSIX_FILE_ATTRIBUTE_VIEW)) {
			LOGGER.debug("File system supports POSIX, setting permission");
			Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(POSIX_USER_ONLY_PERMISSION));
		} else if (supportedFileAttributeViews.contains(ACL_FILE_ATTRIBUTE_VIEW)) {
			LOGGER.debug("File system supports ACL, setting permission");
			final UserPrincipal fileOwner = Files.getOwner(path);

			final AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

			final AclEntry entry = AclEntry.newBuilder()
					.setType(ALLOW)
					.setPrincipal(fileOwner)
					.setFlags(DIRECTORY_INHERIT,
							FILE_INHERIT)
					.setPermissions(WRITE_NAMED_ATTRS,
							WRITE_ATTRIBUTES,
							DELETE,
							WRITE_DATA,
							READ_ACL,
							APPEND_DATA,
							READ_ATTRIBUTES,
							READ_DATA,
							EXECUTE,
							SYNCHRONIZE,
							READ_NAMED_ATTRS)
					.build();

			final List<AclEntry> acl = List.of(entry);
			view.setAcl(acl);
		}
	}

	private static void deleteDirectory(final Path directory) {
		try {
			FileSystemUtils.deleteRecursively(directory);
		} catch (final IOException e) {
			LOGGER.warn("Fail to remove directory. [directory: {}]", directory);
		}
	}
}
