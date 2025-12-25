/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.USB_DIRECTORY_NOT_A_DIRECTORY_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.process.WhiteListService.MAX_DEPTH;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

/**
 * This class import and export the SDM files needed to other SDM instances.
 * <p/>
 * The import will load all elections present on the given path, while the export will only export a single election.
 * <p/>
 * The needed files are defined by the {@link WhiteListService} class.
 */
@Service
public class ImportExportFileSystemService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportExportFileSystemService.class);
	private static final CopyOption[] COPY_OPTIONS = { LinkOption.NOFOLLOW_LINKS, StandardCopyOption.REPLACE_EXISTING,
			StandardCopyOption.COPY_ATTRIBUTES };

	private final PathResolver pathResolver;
	private final WhiteListService whiteListService;
	private final String electionEventSeed;

	public ImportExportFileSystemService(
			final PathResolver pathResolver,
			final WhiteListService whiteListService,
			@Value("${sdm.election-event-seed}")
			final String electionEventSeed) {
		this.pathResolver = pathResolver;
		this.whiteListService = whiteListService;
		this.electionEventSeed = validateSeed(electionEventSeed);
	}

	/**
	 * Import the SDM file system according the whitelist.
	 *
	 * @param usbDirectory to import.
	 */
	public void importFileSystem(final Path usbDirectory) {
		checkNotNull(usbDirectory);
		checkArgument(Files.isDirectory(usbDirectory), USB_DIRECTORY_NOT_A_DIRECTORY_MESSAGE + "[%s]", usbDirectory);

		final Path localSdmDirectory = pathResolver.resolveWorkspacePath();

		checkArgument(Files.isDirectory(localSdmDirectory), "localSdmDirectory is not a directory. [%s]", localSdmDirectory);

		whiteListService.getImportList().stream()
				.flatMap(pattern -> getEligibleFiles(usbDirectory, pattern).stream())
				.forEach(file -> copyFileIfNotExist().accept(usbDirectory.resolve(file), localSdmDirectory.resolve(file)));
	}

	private static ImmutableList<Path> getEligibleFiles(final Path directory, final Pattern pattern) {
		checkNotNull(directory);
		checkNotNull(pattern);

		LOGGER.info("Eligible files. [pattern: {}]", pattern);

		try (final Stream<Path> paths = Files.find(directory, MAX_DEPTH,
				(path, basicFileAttributes) -> pattern.matcher(separatorsToUnix(directory.relativize(path).toString())).matches())) {

			return paths
					.map(directory::relativize)
					.collect(toImmutableList());

		} catch (final IOException e) {
			throw new UncheckedIOException("Cannot retrieve the list of file to import/export.", e);
		}
	}

	private static String separatorsToUnix(final String path) {
		return path == null ? null : path.replace('\\', '/');
	}

	/**
	 * Export the SDM file system according the whitelist.
	 *
	 * @param electionEventId to export.
	 * @param usbDirectory    where export.
	 */
	public void exportFileSystem(final String electionEventId, final Path usbDirectory, final int exchangeIndex) {
		validateUUID(electionEventId);
		checkNotNull(usbDirectory);
		checkArgument(Files.isDirectory(usbDirectory), "usbDirectory is not a directory. [%s]", usbDirectory);

		final Path workspace = pathResolver.resolveWorkspacePath();

		whiteListService.getExportList(electionEventId, exchangeIndex).stream()
				.flatMap(pattern -> getEligibleFiles(workspace, pattern).stream())
				.forEach(file -> copyFile().accept(workspace.resolve(file), usbDirectory.resolve(file)));
	}

	public void collectForVerifier(final VerifierExportType verifierExportType, final String electionEventId, final Path zipDirectory) {
		checkNotNull(verifierExportType);
		validateUUID(electionEventId);
		checkNotNull(zipDirectory);

		final Path workspacePath = pathResolver.resolveWorkspacePath();

		record FileEntry(Path root, Path file, VerifierWhiteList.VerifierEntry verifierEntry) {
		}

		VerifierWhiteList.getList(verifierExportType, electionEventId, electionEventSeed).stream()
				.flatMap(verifierEntry -> getEligibleFiles(workspacePath, verifierEntry.pattern()).stream()
						.map(file -> new FileEntry(workspacePath, file, verifierEntry)))
				.parallel()
				.forEach(fileEntry -> {
					final Path file = fileEntry.file();
					final int nameCount = file.getNameCount();
					final boolean extendWithParentFolder = fileEntry.verifierEntry().extendWithParentFolder();

					final Path destinationFinalPath = extendWithParentFolder ? file.subpath(nameCount - 2, nameCount) : file.getFileName();

					copyFile().accept(fileEntry.root().resolve(file),
							zipDirectory.resolve(fileEntry.verifierEntry().destinationPath()).resolve(destinationFinalPath));
				});
	}

	private static BiConsumer<Path, Path> copyFileIfNotExist() {
		return (final Path source, final Path target) -> {
			if (Files.notExists(target)) {
				copyFile().accept(source, target);
			} else {
				LOGGER.debug("Do not copy file, the file already exists and override is disabled. [source:{}, target:{}]",
						Files.exists(source), Files.exists(target));
			}
		};
	}

	private static BiConsumer<Path, Path> copyFile() {
		return (final Path source, final Path target) -> {
			try {
				if (Files.isSymbolicLink(source)) {
					throw new IllegalStateException(format(
							"There is a symbolic link in the SDM database. Aborting copy. [symbolic-link:%s]", source));
				}
				Files.createDirectories(target.getParent());
				Files.copy(source, target, COPY_OPTIONS);
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		};
	}
}
