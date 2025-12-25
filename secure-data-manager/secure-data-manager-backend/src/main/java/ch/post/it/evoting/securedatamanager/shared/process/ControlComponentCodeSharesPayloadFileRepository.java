/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.file.Files.newDirectoryStream;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.Constants;

/**
 * Allows performing operations with the control component code shares payloads.
 */
@Repository
public class ControlComponentCodeSharesPayloadFileRepository {

	private static final Pattern FILE_PATTERN = Pattern.compile(
			String.format("^%s\\.([\\d]+)\\%s$", Constants.CONFIG_FILE_CONTROL_COMPONENT_CODE_SHARES_PAYLOAD, Constants.JSON));
	private static final ToIntFunction<Path> EXTRACT_CHUNK_ID_FROM_PATH = chunkFilePath -> {
		final Matcher matcher = FILE_PATTERN.matcher(chunkFilePath.getFileName().toString());
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}
		throw new IllegalStateException(String.format("No chunk id found. [path: %s]", chunkFilePath));
	};

	private final ObjectMapper objectMapper;
	private final PathResolver pathResolver;

	public ControlComponentCodeSharesPayloadFileRepository(
			final ObjectMapper objectMapper,
			final PathResolver pathResolver) {
		this.objectMapper = objectMapper;
		this.pathResolver = pathResolver;
	}

	/**
	 * Retrieves all control component code shares payload's chunk paths corresponding to the given election event id and verification card set id.
	 *
	 * @param electionEventId       the payload's election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the payload's verification card set id. Must be non-null and a valid UUID.
	 * @return all control component code shares payload's chunk paths.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is invalid.
	 * @throws UncheckedIOException      if the deserialization of the payload fails.
	 */
	public ImmutableList<Path> findAllPathsOrderedByChunkId(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final Path verificationCardSetPath = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId);
		final Predicate<String> patternPredicate = FILE_PATTERN.asPredicate();

		try (final Stream<Path> paths = Files.walk(verificationCardSetPath, 1)) {
			return paths.filter(path -> patternPredicate.test(path.getFileName().toString()))
					.sorted(Comparator.comparingInt(EXTRACT_CHUNK_ID_FROM_PATH))
					.collect(toImmutableList());
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to walk verification card set directory. [electionEventId: %s, verificationCardSetId: %s, path: %s]",
							electionEventId, verificationCardSetId, verificationCardSetPath), e);
		}
	}

	/**
	 * @param path the control component code shares payload chunk's path. Must be non-null.
	 * @return the chunk id in the given path.
	 */
	public int getChunkId(final Path path) {
		checkNotNull(path);
		return EXTRACT_CHUNK_ID_FROM_PATH.applyAsInt(path);
	}

	/**
	 * Loads a list of control component code shares payloads.
	 *
	 * @param path the control component code shares payload chunk's path. Must be non-null.
	 * @return a list of control component code shares payloads.
	 */
	public ImmutableList<ControlComponentCodeSharesPayload> load(final Path path) {
		checkNotNull(path);

		try {
			final ImmutableList<ControlComponentCodeSharesPayload> controlComponentCodeSharesPayloads = objectMapper.readValue(
					path.toFile(), new TypeReference<>() {
					});
			checkState(getChunkId(path) == controlComponentCodeSharesPayloads.get(0).getChunkId());
			return controlComponentCodeSharesPayloads.stream()
					.sorted(Comparator.comparingInt(ControlComponentCodeSharesPayload::getNodeId))
					.collect(toImmutableList());
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Failed to deserialize the ControlComponentCodeShares payloads. [path: %s]", path), e);
		}
	}

	/**
	 * Saves the control component code shares payloads.
	 *
	 * @param electionEventId                    the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId              the verification card set id. Must be non-null and a valid UUID.
	 * @param chunkId                            the chunk id. Must be strictly positive.
	 * @param controlComponentCodeSharesPayloads the payloads to save. Must be non-null.
	 */
	public void save(final String electionEventId, final String verificationCardSetId, final int chunkId,
			final ImmutableList<ControlComponentCodeSharesPayload> controlComponentCodeSharesPayloads) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkArgument(chunkId >= 0);
		checkNotNull(controlComponentCodeSharesPayloads);

		final String fileName = Constants.CONFIG_FILE_CONTROL_COMPONENT_CODE_SHARES_PAYLOAD + "." + chunkId + Constants.JSON;
		final Path path = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId).resolve(fileName);

		try {
			final byte[] bytes = objectMapper.writeValueAsBytes(controlComponentCodeSharesPayloads);
			Files.write(path, bytes);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format(
							"Failed to save the ControlComponentCodeShares payloads. [electionEventId: %s, verificationCardSetId: %s, chunkId: %s]",
							electionEventId, verificationCardSetId, chunkId), e);
		}
	}

	/**
	 * Checks if the control component code shares payloads exist for the given election event id, verification card set id and chunk id.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @param chunkId               the chunk id. Must be strictly positive.
	 * @return true if the payload exists, false otherwise.
	 */
	public boolean exists(final String electionEventId, final String verificationCardSetId, final int chunkId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkArgument(chunkId >= 0);

		final String fileName = Constants.CONFIG_FILE_CONTROL_COMPONENT_CODE_SHARES_PAYLOAD + "." + chunkId + Constants.JSON;
		final Path path = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId).resolve(fileName);

		return Files.exists(path);
	}

	/**
	 * Deletes the control component code shares payloads for the given election event id and verification card set id.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 */
	public void delete(final String electionEventId, final String verificationCardSetId) throws IOException {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final Path folder = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId);
		final DirectoryStream.Filter<Path> filter = this::isControlComponentCodeSharesPayload;
		try (final DirectoryStream<Path> files = newDirectoryStream(folder, filter)) {
			for (final Path file : files) {
				Files.delete(file);
			}
		}
	}

	private boolean isControlComponentCodeSharesPayload(final Path path) {
		return FILE_PATTERN.matcher(path.getFileName().toString()).matches();
	}
}
