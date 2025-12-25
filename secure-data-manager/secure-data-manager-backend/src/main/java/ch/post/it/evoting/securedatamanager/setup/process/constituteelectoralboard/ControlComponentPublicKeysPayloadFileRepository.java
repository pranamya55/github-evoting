/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.constituteelectoralboard;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

/**
 * Allows performing operations with the control component public keys payloads. The payloads are persisted/retrieved to/from the file system of the
 * SDM, in its workspace.
 */
@Repository
@ConditionalOnProperty("role.isSetup")
public class ControlComponentPublicKeysPayloadFileRepository {

	private static final Pattern PAYLOAD_FILE_PATTERN = Pattern.compile("^controlComponentPublicKeysPayload\\.[\\d]\\.json$");
	private static final Logger LOGGER = LoggerFactory.getLogger(ControlComponentPublicKeysPayloadFileRepository.class);

	private final ObjectMapper objectMapper;
	private final PathResolver pathResolver;

	public ControlComponentPublicKeysPayloadFileRepository(
			final ObjectMapper objectMapper, final PathResolver pathResolver) {
		this.objectMapper = objectMapper;
		this.pathResolver = pathResolver;
	}

	/**
	 * Retrieves all control component public keys payloads corresponding to the given election event id. The returned list is ordered by the
	 * {@link ControlComponentPublicKeys}.nodeId.
	 *
	 * @param electionEventId the election event id for which to retrieve the payloads.
	 * @return all payloads for {@code electionEventId} ordered by node id.
	 * @throws FailedValidationException if {@code electionEventId} is invalid.
	 * @throws UncheckedIOException      if the deserialization of the payload fails.
	 */
	public ImmutableList<ControlComponentPublicKeysPayload> findAllOrderByNodeId(final String electionEventId) {
		validateUUID(electionEventId);

		final Path electionEventPath = pathResolver.resolveElectionEventPath(electionEventId);
		final Predicate<String> patternPredicate = PAYLOAD_FILE_PATTERN.asPredicate();

		try (final Stream<Path> paths = Files.walk(electionEventPath, 1).parallel()) {
			return paths.filter(path -> patternPredicate.test(path.getFileName().toString()))
					.map(payloadPath -> {
						try {
							LOGGER.debug("Reading control component public keys payload... [electionEventId: {}, path: {}]", electionEventId,
									payloadPath);
							final ControlComponentPublicKeysPayload controlComponentPublicKeysPayload = objectMapper.readValue(payloadPath.toFile(),
									ControlComponentPublicKeysPayload.class);
							LOGGER.debug("Successfully read control component public keys payload. [electionEventId: {}, path: {}]", electionEventId,
									payloadPath);
							return controlComponentPublicKeysPayload;
						} catch (final IOException e) {
							throw new UncheckedIOException(
									String.format("Failed to deserialize control component public keys payload. [electionEventId: %s, path: %s]",
											electionEventId, payloadPath), e);
						}
					})
					.sorted(Comparator.comparingInt(e -> e.getControlComponentPublicKeys().nodeId()))
					.collect(toImmutableList());
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to walk election event directory. [electionEventId: %s, path: %s]", electionEventId, electionEventPath), e);
		}
	}

}
