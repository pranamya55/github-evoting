/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.Files.newDirectoryStream;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.configuration.SetupComponentCMTablePayload;
import ch.post.it.evoting.domain.configuration.SetupComponentCMTablePayloadChunks;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.Constants;

@Repository
public class SetupComponentCMTablePayloadFileRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentCMTablePayloadFileRepository.class);

	private final ObjectMapper objectMapper;
	private final PathResolver pathResolver;

	public SetupComponentCMTablePayloadFileRepository(
			final ObjectMapper objectMapper,
			final PathResolver pathResolver) {
		this.objectMapper = objectMapper;
		this.pathResolver = pathResolver;
	}

	/**
	 * Saves the setup component CMTable payload to the filesystem for the given election event and verification card set.
	 *
	 * @return the path of the saved file.
	 * @throws NullPointerException     if any of the inputs is null.
	 * @throws IllegalArgumentException if any of the inputs is not valid.
	 * @see PathResolver to get the resolved file Path.
	 */
	public Path save(final SetupComponentCMTablePayload payload) {
		checkNotNull(payload);

		final String electionEventId = validateUUID(payload.getElectionEventId());
		final String verificationCardSetId = validateUUID(payload.getVerificationCardSetId());
		final int chunkId = payload.getChunkId();

		final Path payloadPath = payloadPath(electionEventId, verificationCardSetId, chunkId);

		try {
			final byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);
			final Path filePath = Files.write(payloadPath, payloadBytes);

			LOGGER.debug("Successfully persisted setup component CMTable payload. [electionEventId: {}, verificationCardSetId: {}, chunkId {}]",
					electionEventId, verificationCardSetId, chunkId);

			return filePath;
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format(
							"Unable to write the setup component CMTable payload file. [electionEventId: %s, verificationCardSetId: %s, chunkId: %s]",
							electionEventId, verificationCardSetId, chunkId), e);
		}
	}

	/**
	 * Retrieves from the file system the setup component CMTable payloads by election event and verification card set id.
	 *
	 * @param electionEventId       the payloads' election event id.
	 * @param verificationCardSetId the payloads' verification card set id.
	 * @return all setup component CMTable payloads with the given ids or {@link Optional#empty} if not found.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} is invalid.
	 * @throws UncheckedIOException      if the deserialization of the payload fails.
	 */
	public Optional<SetupComponentCMTablePayloadChunks> findByElectionEventIdAndVerificationCardSetId(final String electionEventId,
			final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final Path verificationCardSetPath = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId);
		final DirectoryStream.Filter<? super Path> filter = SetupComponentCMTablePayloadFileRepository::isPayloadFile;

		try (final DirectoryStream<Path> payloadPaths = newDirectoryStream(verificationCardSetPath, filter)) {
			final ImmutableList<SetupComponentCMTablePayload> setupComponentCMTablePayloads = StreamSupport.stream(payloadPaths.spliterator(), true)
					.sorted(Comparator.comparing(SetupComponentCMTablePayloadFileRepository::getChunkId))
					.map(payloadPath -> {
						try {
							return objectMapper.readValue(payloadPath.toFile(), SetupComponentCMTablePayload.class);
						} catch (final IOException e) {
							throw new UncheckedIOException(
									String.format(
											"Failed to deserialize setup component CMTable payload. [electionEventId: %s, verificationCardSetId: %s]",
											electionEventId, verificationCardSetId), e);
						}
					})
					.collect(toImmutableList());
			if (setupComponentCMTablePayloads.isEmpty()) {
				LOGGER.warn("No setup component CMTable payloads found. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
						verificationCardSetId);
				return Optional.empty();
			}
			return Optional.of(new SetupComponentCMTablePayloadChunks(setupComponentCMTablePayloads));
		} catch (final IOException e) {
			LOGGER.warn("Failed to find the setup component CMTable payloads. [electionEventId: {}, verificationCardSetId: {}]",
					electionEventId, verificationCardSetId);
			return Optional.empty();
		}
	}

	private Path payloadPath(final String electionEventId, final String verificationCardSetId, final int chunkId) {
		final Path verificationCardSetPath = pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId);
		return verificationCardSetPath.resolve(Constants.CONFIG_FILE_NAME_PREFIX_SETUP_COMPONENT_CM_TABLE_PAYLOAD + chunkId + Constants.JSON);
	}

	private static boolean isPayloadFile(final Path file) {
		final String name = file.getFileName().toString();
		return name.startsWith(Constants.CONFIG_FILE_NAME_PREFIX_SETUP_COMPONENT_CM_TABLE_PAYLOAD) && name.endsWith(Constants.JSON);
	}

	private static int getChunkId(final Path file) {
		final String name = file.getFileName().toString();
		return Integer.parseInt(name.substring(name.indexOf(".") + 1, name.indexOf(Constants.JSON)));
	}
}
