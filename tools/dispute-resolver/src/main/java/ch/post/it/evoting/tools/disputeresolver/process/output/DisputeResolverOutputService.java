/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.output;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DisputeResolverOutputService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DisputeResolverOutputService.class);

	private final DisputeResolverResolvedConfirmedVotesPayloadService disputeResolverResolvedConfirmedVotesPayloadService;

	public DisputeResolverOutputService(
			final DisputeResolverResolvedConfirmedVotesPayloadService disputeResolverResolvedConfirmedVotesPayloadService) {
		this.disputeResolverResolvedConfirmedVotesPayloadService = disputeResolverResolvedConfirmedVotesPayloadService;
	}

	/**
	 * Saves the output of the dispute resolver to the file system.
	 *
	 * @param disputeResolverOutput the dispute resolver output containing the resulting payload. Must not be null.
	 * @throws NullPointerException if the dispute resolver output is null.
	 */
	public void save(final DisputeResolverOutput disputeResolverOutput) {
		checkNotNull(disputeResolverOutput);

		LOGGER.debug("Saving the output of the dispute resolver into the file system...");

		disputeResolverResolvedConfirmedVotesPayloadService.save(disputeResolverOutput.disputeResolverResolvedConfirmedVotesPayload());

		LOGGER.debug("Output files successfully saved into the file system.");
	}

}
