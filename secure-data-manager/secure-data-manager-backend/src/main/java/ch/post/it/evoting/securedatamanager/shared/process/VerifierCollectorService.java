/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.evotinglibraries.xml.XsdConstants.TALLY_COMPONENT_ECH_0222_VERSION;
import static ch.post.it.evoting.securedatamanager.shared.Constants.DATE_TIME_FORMAT_PATTERN;
import static ch.post.it.evoting.securedatamanager.shared.Constants.TALLY_COMPONENT_ECH_0222_XML;
import static ch.post.it.evoting.securedatamanager.shared.Constants.VERIFIER_DATASET_MANIFEST;
import static com.google.common.base.Preconditions.checkNotNull;

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
import java.util.Locale;
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

@Service
public class VerifierCollectorService {
	private static final Logger LOGGER = LoggerFactory.getLogger(VerifierCollectorService.class);
	private static final ImmutableByteArray ASSOCIATED_DATA = ImmutableByteArray.EMPTY;
	private static final String FILE_NAME_PATTERN = "Dataset-%s-%s-%s.zip"; // Dataset-{$Type}-{Seed}-{timestamp}.zip ($Type is context or tally)

	private final ObjectMapper objectMapper;
	private final PathResolver pathResolver;
	private final CompressionService compressionService;
	private final ImportExportFileSystemService importExportFileSystemService;
	private final StreamableSymmetricEncryptionDecryptionService symmetricEncryptionDecryptionService;

	private final char[] verifierExportZipPassword;
	private final String electionEventSeed;
	private final String buildVersion;

	public VerifierCollectorService(
			final ObjectMapper objectMapper,
			final PathResolver pathResolver,
			final CompressionService compressionService,
			final ImportExportFileSystemService importExportFileSystemService,
			final StreamableSymmetricEncryptionDecryptionService symmetricEncryptionDecryptionService,
			@Value("${sdm.process.collect-data-verifier.zip-password}")
			final char[] verifierExportZipPassword,
			@Value("${sdm.election-event-seed}")
			final String electionEventSeed,
			@Value("${build.version}")
			final String buildVersion) {
		this.objectMapper = objectMapper;
		this.pathResolver = pathResolver;
		this.compressionService = compressionService;
		this.importExportFileSystemService = importExportFileSystemService;
		this.symmetricEncryptionDecryptionService = symmetricEncryptionDecryptionService;
		this.verifierExportZipPassword = PasswordValidation.validate(verifierExportZipPassword, "verifier export zip", StandardCharsets.ISO_8859_1);
		this.electionEventSeed = validateSeed(electionEventSeed);
		this.buildVersion = buildVersion;
	}

	public void collectDataset(final VerifierExportType verifierExportType, final String electionEventId) {
		checkNotNull(verifierExportType);
		validateUUID(electionEventId);

		final Path zipDirectory = createTemporaryDirectory();

		createManifest(electionEventId, verifierExportType, zipDirectory);
		LOGGER.info("Manifest created. [electionEventId: {}, verifierExportType: {}]", electionEventId, verifierExportType);

		try {
			importExportFileSystemService.collectForVerifier(verifierExportType, electionEventId, zipDirectory);

			final String datasetFilename = getExportFilename(verifierExportType, true);
			final Path filePath = pathResolver.resolveVerifierOutputPath().resolve(datasetFilename);

			try (final OutputToInputStreamConverter converter = new OutputToInputStreamConverter();
					final InputStream zipInputStream = converter.convert(os -> {
						try {
							compressionService.zipDirectory(os, zipDirectory);
						} catch (final IOException e) {
							throw new UncheckedIOException(e);
						}
					});
					final OutputStream outputStream = new FileOutputStream(filePath.toString())) {

				symmetricEncryptionDecryptionService.genStreamCiphertext(outputStream, zipInputStream, verifierExportZipPassword,
						ASSOCIATED_DATA);
			}
		} catch (final IOException e) {
			throw new UncheckedIOException("Error during export.", e);
		}

		deleteDirectory(zipDirectory);
	}

	public String getExportFilename(final VerifierExportType verifierExportType, final boolean withExportTime) {
		final String verifierExportTypeName = verifierExportType.toString().toLowerCase(Locale.ENGLISH);

		final String exportTime;
		if (withExportTime) {
			final LocalDateTime timestamp = LocalDateTimeUtils.now();
			final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN);
			exportTime = timestamp.format(timeFormatter);
		} else {
			exportTime = "*";
		}

		return String.format(FILE_NAME_PATTERN, verifierExportTypeName, electionEventSeed, exportTime);
	}

	public String getExportECH0222Filename(final VerifierExportType verifierExportType) {
		checkNotNull(verifierExportType);
		return String.format(TALLY_COMPONENT_ECH_0222_XML, TALLY_COMPONENT_ECH_0222_VERSION, electionEventSeed);
	}

	private void createManifest(final String electionEventId, final VerifierExportType exportType, final Path zipDirectory) {

		final VerifierDatasetManifest manifest = new VerifierDatasetManifest(electionEventId, buildVersion, exportType);
		try {
			final Path datasetPath = zipDirectory.resolve(exportType.rootPath());
			Files.createDirectories(datasetPath);
			Files.write(datasetPath.resolve(VERIFIER_DATASET_MANIFEST), objectMapper.writeValueAsBytes(manifest));
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Cannot write the manifest file. [electionEventId: %s, exportType: %s]", electionEventId, exportType), e);
		}
	}

	// The temporary directory is created with a random UUID, used immediately, and deleted after processing.
	@SuppressWarnings("java:S5443")
	private Path createTemporaryDirectory() {
		try {
			final Path temporaryDirectory = Files.createTempDirectory(UUID.randomUUID().toString()).toAbsolutePath();

			LOGGER.info("Temporary directory for verifier export created. [path: {}]", temporaryDirectory);

			return temporaryDirectory;
		} catch (final IOException e) {
			throw new UncheckedIOException("Unable to create temporary directory for verifier export.", e);
		}
	}

	private void deleteDirectory(final Path directory) {
		try {
			FileSystemUtils.deleteRecursively(directory);
		} catch (final IOException e) {
			LOGGER.warn("Fail to remove temporary directory for verifier export. [directory: {}]", directory);
		}
	}

}
