/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.dataexchange;

import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CANNOT_READ_MANIFEST_FILE_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CANNOT_UNZIP_FILE_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.DATE_TIME_FORMAT_PATTERN;
import static ch.post.it.evoting.securedatamanager.shared.Constants.IMPORT_CONTENT_NOT_MATCH_CURRENT_ELECTION_EVENT_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.IMPORT_CONTENT_NOT_MATCH_CURRENT_ELECTION_EVENT_SEED_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.IMPORT_CONTENT_NOT_MATCH_CURRENT_IMPORT_STEP_MESSAGE;
import static ch.post.it.evoting.securedatamanager.shared.Constants.IMPORT_STEP_NOT_EXIST_MESSAGE;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.validations.PasswordValidation;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.channelsecurity.StreamableSymmetricEncryptionDecryptionService;
import ch.post.it.evoting.evotinglibraries.toolbox.OutputToInputStreamConverter;
import ch.post.it.evoting.securedatamanager.shared.Constants;
import ch.post.it.evoting.securedatamanager.shared.process.CompressionService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.process.ImportExportFileSystemService;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep;

@Service
public class ImportExportService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportExportService.class);
	private static final ImmutableByteArray ASSOCIATED_DATA = ImmutableByteArray.EMPTY;
	private static final String FILE_NAME_PATTERN = "export-[%s]-%s-%s.sdm"; // export-[StepNumber]-{Seed}-{timestamp].sdm
	private static final String FILE_NAME_PARTIAL_PATTERN = "export-partial-[%s]-%s-%s.sdm"; // export-partial-[StepNumber]-{Seed}-{timestamp].sdm

	private final String electionEventSeed;
	private final ObjectMapper objectMapper;
	private final PathResolver pathResolver;
	private final CompressionService compressionService;
	private final ElectionEventService electionEventService;
	private final ImportExportDatabaseService importExportDatabaseService;
	private final ImportExportFileSystemService importExportFileSystemService;
	private final StreamableSymmetricEncryptionDecryptionService symmetricEncryptionDecryptionService;
	private final char[] importExportZipPassword;

	public ImportExportService(
			final ObjectMapper objectMapper,
			final PathResolver pathResolver,
			final CompressionService compressionService,
			final ElectionEventService electionEventService,
			final ImportExportDatabaseService importExportDatabaseService,
			final ImportExportFileSystemService importExportFileSystemService,
			final StreamableSymmetricEncryptionDecryptionService symmetricEncryptionDecryptionService,
			@Value("${sdm.election-event-seed}")
			final String electionEventSeed,
			@Value("${sdm.process.data-exchange.zip-password}")
			final char[] importExportZipPassword) {
		this.objectMapper = objectMapper;
		this.pathResolver = pathResolver;
		this.compressionService = compressionService;
		this.electionEventService = electionEventService;
		this.importExportDatabaseService = importExportDatabaseService;
		this.importExportFileSystemService = importExportFileSystemService;
		this.symmetricEncryptionDecryptionService = symmetricEncryptionDecryptionService;

		this.electionEventSeed = validateSeed(electionEventSeed);
		this.importExportZipPassword = PasswordValidation.validate(importExportZipPassword, "import export zip", StandardCharsets.ISO_8859_1);
	}

	public void importElectionEventData(final int exchangeIndex, final Path importPath) {
		checkNotNull(importPath);
		checkArgument(Files.exists(importPath), "The import file does not exist.");

		try (final InputStream inputStream = Files.newInputStream(importPath);
				final InputStream decryptedStream = symmetricEncryptionDecryptionService.getStreamPlaintext(inputStream, importExportZipPassword,
						ASSOCIATED_DATA)) {
			final Path unzipDirectory = unzip(decryptedStream);

			final ImportExportManifest manifest = readManifest(unzipDirectory);
			LOGGER.debug("Manifest read. [electionEventId: {}, exchangeIndex: {}]", manifest.electionEventId(), manifest.exchangeIndex());

			// Check if the import step exists
			final WorkflowStep importStepFromManifest = WorkflowStep.getImportStep(manifest.exchangeIndex());
			checkNotNull(importStepFromManifest, IMPORT_STEP_NOT_EXIST_MESSAGE + " [manifestExchangeIndex: %s] " + manifest.exchangeIndex());

			// Check if the import step matches the current import step
			checkState(exchangeIndex == manifest.exchangeIndex(),
					IMPORT_CONTENT_NOT_MATCH_CURRENT_IMPORT_STEP_MESSAGE + " [currentImportStep: %s, manifestImportStep: %s]",
					WorkflowStep.getImportStep(exchangeIndex), importStepFromManifest);

			// Check if the election event matches the current election event
			final String electionEventId = this.electionEventService.findElectionEventId();
			if (electionEventId != null) {
				checkState(electionEventId.equals(manifest.electionEventId()),
						IMPORT_CONTENT_NOT_MATCH_CURRENT_ELECTION_EVENT_MESSAGE + ". [electionEventId: %s, manifestElectionEventId: %s]",
						electionEventId, manifest.electionEventId());
			}

			// Check if the import matches the current seed.
			checkState(electionEventSeed.equals(manifest.electionEventSeed()),
					IMPORT_CONTENT_NOT_MATCH_CURRENT_ELECTION_EVENT_SEED_MESSAGE + ". [electionEventSeed: %s, manifestElectionEventSeed: %s]",
					electionEventSeed, manifest.electionEventSeed());

			importDatabase(unzipDirectory);
			importExportFileSystemService.importFileSystem(unzipDirectory);

			deleteDirectory(unzipDirectory);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void exportElectionEventData(final String electionEventId, final int exchangeIndex) {
		validateUUID(electionEventId);

		final Path zipDirectory = createTemporaryDirectory();

		final int convertedExchangeIndex = convertExchangeIndex(exchangeIndex);

		createManifest(electionEventId, convertedExchangeIndex, zipDirectory);
		LOGGER.debug("Manifest created. [electionEventId: {}, exchangeIndex: {}]", electionEventId, convertedExchangeIndex);

		try {
			exportDatabase(zipDirectory, electionEventId);
			importExportFileSystemService.exportFileSystem(electionEventId, zipDirectory, exchangeIndex);

			final String electionEventExportFilename = getExportFilename(exchangeIndex, true);
			final Path filePath = pathResolver.resolveOutputPath().resolve(electionEventExportFilename);

			try (final OutputToInputStreamConverter converter = new OutputToInputStreamConverter();
					final InputStream zipInputStream = converter.convert(os -> {
						try {
							compressionService.zipDirectory(os, zipDirectory);
						} catch (final IOException e) {
							throw new UncheckedIOException(e);
						}
					});
					final OutputStream outputStream = new FileOutputStream(filePath.toString())) {
				symmetricEncryptionDecryptionService.genStreamCiphertext(outputStream, zipInputStream, importExportZipPassword, ASSOCIATED_DATA);
			}
		} catch (final IOException e) {
			throw new UncheckedIOException("Error during export.", e);
		}

		deleteDirectory(zipDirectory);
	}

	public String getExportFilename(final int exchangeIndex, final boolean withExportTime) {
		final String exportTime;
		if (withExportTime) {
			final LocalDateTime timestamp = LocalDateTimeUtils.now();
			final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN);
			exportTime = timestamp.format(timeFormatter);
		} else {
			exportTime = "*";
		}

		// The exchange index 50 is used for partial exports. It is converted to 5.
		if (exchangeIndex == 50) {
			final int convertedExchangeIndex = convertExchangeIndex(exchangeIndex);
			return String.format(FILE_NAME_PARTIAL_PATTERN, convertedExchangeIndex, electionEventSeed, exportTime);
		}
		return String.format(FILE_NAME_PATTERN, exchangeIndex, electionEventSeed, exportTime);
	}

	private void createManifest(final String electionEventId, final int exchangeIndex, final Path zipDirectory) {
		final ImportExportManifest manifest = new ImportExportManifest(electionEventId, electionEventSeed, exchangeIndex);
		try {
			Files.write(zipDirectory.resolve(Constants.IMPORT_EXPORT_MANIFEST), objectMapper.writeValueAsBytes(manifest));
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Cannot write the manifest file. [electionEventId: %s, exchangeIndex: %s]", electionEventId, exchangeIndex), e);
		}
	}

	private ImportExportManifest readManifest(final Path unzipDirectory) {
		try {
			return objectMapper.readValue(Files.readAllBytes(unzipDirectory.resolve(Constants.IMPORT_EXPORT_MANIFEST)), ImportExportManifest.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(CANNOT_READ_MANIFEST_FILE_MESSAGE, e);
		}
	}

	private void deleteDirectory(final Path directory) {
		try {
			FileSystemUtils.deleteRecursively(directory);
		} catch (final IOException e) {
			LOGGER.warn("Fail to remove directory. [directory: {}]", directory);
		}
	}

	private Path unzip(final InputStream inputStream) {
		try {
			final Path unzipDirectory = createTemporaryDirectory();
			compressionService.unzipToDirectory(inputStream, unzipDirectory);
			return unzipDirectory;
		} catch (final IOException e) {
			throw new UncheckedIOException(CANNOT_UNZIP_FILE_MESSAGE, e);
		}
	}

	private void importDatabase(final Path directory) {
		try {
			final Path dbDump = directory.resolve(Constants.DBDUMP_FILE_NAME);
			importExportDatabaseService.importDatabase(dbDump);
		} catch (final IOException e) {
			throw new UncheckedIOException("Cannot import database.", e);
		}
	}

	// The temporary directory is created with a random UUID, used immediately, and deleted after processing.
	@SuppressWarnings("java:S5443")
	private Path createTemporaryDirectory() {
		try {
			final Path temporaryDirectory = Files.createTempDirectory(UUID.randomUUID().toString()).toAbsolutePath();

			LOGGER.info("Temporary directory created. [path: {}]", temporaryDirectory);

			return temporaryDirectory;
		} catch (final IOException e) {
			throw new UncheckedIOException("Unable to create temporary directory", e);
		}
	}

	private void exportDatabase(final Path directory, final String electionEventId) {
		final Path dbDump = directory.resolve(Constants.DBDUMP_FILE_NAME);
		importExportDatabaseService.exportDatabase(dbDump, electionEventId);
	}

	private int convertExchangeIndex(final int exchangeIndex) {
		// The exchange index 50 is used for partial exports. It is converted to 5.
		if (exchangeIndex == 50) {
			return 5;
		}
		return exchangeIndex;
	}
}
