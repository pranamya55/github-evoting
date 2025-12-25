/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_NAME_PREFIX_SETUP_COMPONENT_VERIFICATION_DATA_PAYLOAD;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.Constants;

/**
 * Manages the storage and retrieval of return code generation (for both choice return codes and vote cast return codes) request payloads stored on
 * the file system.
 */
@Repository
public class SetupComponentVerificationDataPayloadFileRepository {
	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentVerificationDataPayloadFileRepository.class);
	private static final Pattern FILE_PATTERN = Pattern.compile(
			"^" + CONFIG_FILE_NAME_PREFIX_SETUP_COMPONENT_VERIFICATION_DATA_PAYLOAD + "([\\d]+)\\.json$");

	private static final ToIntFunction<Path> EXTRACT_CHUNK_ID_FROM_PATH = chunkFilePath -> {
		final Matcher matcher = FILE_PATTERN.matcher(chunkFilePath.getFileName().toString());
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}
		throw new IllegalStateException(String.format("No chunk id found. [path: %s]", chunkFilePath));
	};

	private final ObjectMapper objectMapper;
	private final PathResolver pathResolver;

	@Autowired
	public SetupComponentVerificationDataPayloadFileRepository(final ObjectMapper objectMapper, final PathResolver pathResolver) {
		this.objectMapper = objectMapper;
		this.pathResolver = pathResolver;
	}

	/**
	 * Obtains the path where the pre-computed data is stored.
	 *
	 * @param electionEventId       the election event the payload belongs to
	 * @param verificationCardSetId the verification card set the payload was generated for
	 * @param chunkId               the chunk identifier
	 */
	private Path getStoragePath(final String electionEventId, final String verificationCardSetId, final int chunkId) {
		final String fileName = CONFIG_FILE_NAME_PREFIX_SETUP_COMPONENT_VERIFICATION_DATA_PAYLOAD + chunkId + Constants.JSON;
		return pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId).resolve(fileName);
	}

	private static boolean isPayloadFile(final Path file) {
		final String name = file.getFileName().toString();
		return name.startsWith(CONFIG_FILE_NAME_PREFIX_SETUP_COMPONENT_VERIFICATION_DATA_PAYLOAD) && name.endsWith(Constants.JSON);
	}

	/**
	 * Stores a setup component verification data payload.
	 *
	 * @param setupComponentVerificationDataPayload the payload to store.
	 * @throws UncheckedIOException if the storage did not succeed
	 */
	public void store(final SetupComponentVerificationDataPayload setupComponentVerificationDataPayload) throws UncheckedIOException {
		final String electionEventId = setupComponentVerificationDataPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentVerificationDataPayload.getVerificationCardSetId();
		final int chunkId = setupComponentVerificationDataPayload.getChunkId();

		LOGGER.debug("Storing setup component verification data payload... [electionEventId: {}, verificationCardSetId: {}, chunkId: {}]",
				electionEventId, verificationCardSetId, chunkId);

		final Path file = getStoragePath(electionEventId, verificationCardSetId, chunkId);

		try {
			if (!exists(file.getParent())) {
				createDirectories(file.getParent());
			}
			try (final OutputStream stream = newOutputStream(file)) {
				objectMapper.writeValue(stream, setupComponentVerificationDataPayload);
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}

		LOGGER.info(
				"Successfully stored setup component verification data payload. [electionEventId: {}, verificationCardSetId: {}, chunkId: {}, path: {}]",
				electionEventId, verificationCardSetId, chunkId, file.toAbsolutePath());
	}

	/**
	 * Retrieves a setup component verification data payload.
	 *
	 * @param electionEventId       the identifier of the election event the verification card set belongs to
	 * @param verificationCardSetId the identifier of the verification card set the payload is for
	 * @param chunkId               the chunk identifier
	 * @return the requested setup component verification data payload
	 */
	public SetupComponentVerificationDataPayload retrieve(final String electionEventId, final String verificationCardSetId, final int chunkId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkArgument(chunkId >= 0);

		final Path file = getStoragePath(electionEventId, verificationCardSetId, chunkId);

		LOGGER.debug("Retrieving choice code generation request payload... [electionEventId: {}, verificationCardSetId: {}, chunkId: {}, path: {}]",
				electionEventId, verificationCardSetId, chunkId, file.toAbsolutePath());

		final SetupComponentVerificationDataPayload payload;
		try (final InputStream stream = newInputStream(file)) {
			payload = objectMapper.readValue(stream, SetupComponentVerificationDataPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}

		LOGGER.info("Successfully retrieved Choice code generation request payload. [electionEventId: {}, verificationCardSetId: {}, chunkId: {}]",
				electionEventId, verificationCardSetId, chunkId);

		return payload;
	}

	/**
	 * Removes all the payloads for given election event and verification card set.
	 *
	 * @param electionEventId       the election event identifier
	 * @param verificationCardSetId the verification card set identifier
	 */
	public void remove(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		try (final DirectoryStream<Path> files = getPayloadFiles(electionEventId, verificationCardSetId)) {
			for (final Path file : files) {
				deleteIfExists(file);
			}
		} catch (final NoSuchFileException e) {
			LOGGER.debug("The verification card set folder does not exist.", e);
			// nothing to do, the verification card set folder does not exist.
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Returns the number of payloads for given election event and verification card set.
	 *
	 * @param electionEventId       the election event identifier
	 * @param verificationCardSetId the verification card set identifier
	 * @return the number of payloads
	 */
	public int getCount(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		int count = 0;
		try (final DirectoryStream<Path> files = getPayloadFiles(electionEventId, verificationCardSetId)) {
			for (
					@SuppressWarnings("unused")
					final Path file : files) {
				count++;
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		return count;
	}

	/**
	 * Retrieves all setup component verification data chunk paths corresponding to the given election event id and verification card set id.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return all setup component verification data chunk paths.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is invalid.
	 */
	public ImmutableList<Path> findAllPathsOrderByChunkId(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final Path verificationCardSetPath = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId);
		try (final Stream<Path> paths = Files.walk(verificationCardSetPath, 1)) {
			return paths.filter(SetupComponentVerificationDataPayloadFileRepository::isPayloadFile)
					.sorted(Comparator.comparingInt(EXTRACT_CHUNK_ID_FROM_PATH))
					.collect(toImmutableList());
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to walk verification card set directory. [electionEventId: %s, verificationCardSetId: %s, path: %s]",
							electionEventId, verificationCardSetId, verificationCardSetPath), e);
		}
	}

	public int getChunkId(final Path path) {
		return EXTRACT_CHUNK_ID_FROM_PATH.applyAsInt(path);
	}

	private DirectoryStream<Path> getPayloadFiles(final String electionEventId, final String verificationCardSetId) throws IOException {
		final Path folder = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId);
		final Filter<? super Path> filter = SetupComponentVerificationDataPayloadFileRepository::isPayloadFile;
		return newDirectoryStream(folder, filter);
	}

	/**
	 * Gets the payload chunk file size for a given verificationCardSetId. The result of this method is cached.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return the size of the file in bytes.
	 */
	@Cacheable("setupComponentVerificationDataPayloadSizes")
	public long getPayloadSize(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		LOGGER.debug("Calculating the SetupComponentVerificationDataPayload size. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);

		final Path path = getStoragePath(electionEventId, verificationCardSetId, 0);
		checkState(Files.exists(path), "The required SetupComponentVerificationDataPayload file does not exist. [path: {}]", path.toAbsolutePath());

		return path.toFile().length();
	}
}
