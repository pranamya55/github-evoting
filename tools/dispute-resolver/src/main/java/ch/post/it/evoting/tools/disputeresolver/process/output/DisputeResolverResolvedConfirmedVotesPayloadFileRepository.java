/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.output;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.tally.disputeresolver.DisputeResolverResolvedConfirmedVotesPayload;
import ch.post.it.evoting.tools.disputeresolver.process.PathService;

@Repository
public class DisputeResolverResolvedConfirmedVotesPayloadFileRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(DisputeResolverResolvedConfirmedVotesPayloadFileRepository.class);

	private final ObjectMapper objectMapper;
	private final PathService pathService;

	public DisputeResolverResolvedConfirmedVotesPayloadFileRepository(
			final ObjectMapper objectMapper,
			final PathService pathService) {

		this.objectMapper = objectMapper;
		this.pathService = pathService;
	}

	/**
	 * Saves the dispute resolver resolved confirmed votes payload to the file system.
	 *
	 * @param disputeResolverResolvedConfirmedVotesPayload the dispute resolver resolved confirmed votes payload to save. Must not be null.
	 * @throws NullPointerException     if the payload is null or if the signature of the payload is null.
	 * @throws UncheckedIOException     if an I/O error occurs while writing the payload to the file system.
	 */
	public void save(final DisputeResolverResolvedConfirmedVotesPayload disputeResolverResolvedConfirmedVotesPayload) {
		checkNotNull(disputeResolverResolvedConfirmedVotesPayload);
		checkNotNull(disputeResolverResolvedConfirmedVotesPayload.getSignature(),
				"The signature of the dispute resolver resolved confirmed votes payload must not be null.");

		final Path disputeResolverResolvedConfirmedVotesPayloadPath = pathService.getDisputeResolverResolvedConfirmedVotesPayloadPath();

		LOGGER.debug("Saving dispute resolver resolved confirmed votes payload... [path: {}]", disputeResolverResolvedConfirmedVotesPayloadPath);

		try {
			final byte[] payloadBytes = objectMapper.writeValueAsBytes(disputeResolverResolvedConfirmedVotesPayload);
			Files.write(disputeResolverResolvedConfirmedVotesPayloadPath, payloadBytes, StandardOpenOption.CREATE_NEW);
		} catch (final IOException e) {
			throw new UncheckedIOException("Failed to write dispute resolver resolved confirmed votes payload to file system.", e);
		}

		LOGGER.debug("Dispute resolver resolved confirmed votes payload saved successfully. [path: {}]",
				disputeResolverResolvedConfirmedVotesPayloadPath);
	}

}
