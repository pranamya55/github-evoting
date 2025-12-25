/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process;

import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.tally.disputeresolver.DisputeResolverResolvedConfirmedVotesPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ResolvedConfirmedVote;
import ch.post.it.evoting.tools.disputeresolver.process.input.DisputeResolverInput;
import ch.post.it.evoting.tools.disputeresolver.process.input.DisputeResolverInputService;
import ch.post.it.evoting.tools.disputeresolver.process.output.DisputeResolverOutput;
import ch.post.it.evoting.tools.disputeresolver.process.output.DisputeResolverOutputService;
import ch.post.it.evoting.tools.disputeresolver.protocol.CheckExtractedElectionEventConsistencyService;
import ch.post.it.evoting.tools.disputeresolver.protocol.CheckVoteConfirmationConsistencyService;
import ch.post.it.evoting.tools.disputeresolver.protocol.CheckVoteConsistencyService;

@Service
public class DisputeResolverService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DisputeResolverService.class);

	private final DisputeResolverInputService disputeResolverInputService;
	private final CheckVoteConsistencyService checkVoteConsistencyService;
	private final DisputeResolverOutputService disputeResolverOutputService;
	private final CheckVoteConfirmationConsistencyService checkVoteConfirmationConsistencyService;
	private final CheckExtractedElectionEventConsistencyService checkExtractedElectionEventConsistencyService;

	public DisputeResolverService(
			final DisputeResolverInputService disputeResolverInputService,
			final CheckVoteConsistencyService checkVoteConsistencyService,
			final DisputeResolverOutputService disputeResolverOutputService,
			final CheckVoteConfirmationConsistencyService checkVoteConfirmationConsistencyService,
			final CheckExtractedElectionEventConsistencyService checkExtractedElectionEventConsistencyService) {
		this.disputeResolverInputService = disputeResolverInputService;
		this.checkVoteConsistencyService = checkVoteConsistencyService;
		this.disputeResolverOutputService = disputeResolverOutputService;
		this.checkVoteConfirmationConsistencyService = checkVoteConfirmationConsistencyService;
		this.checkExtractedElectionEventConsistencyService = checkExtractedElectionEventConsistencyService;
	}

	/**
	 * Runs the dispute resolution process. It reads the input, resolves the dispute and writes the output.
	 */
	public void run() {
		// Read the input.

		LOGGER.info("Reading dispute resolver input...");
		final DisputeResolverInput disputeResolverInput = disputeResolverInputService.read();
		final String electionEventId = disputeResolverInput.electionEventId();
		LOGGER.info("Dispute resolver input read successfully. Election event is {}.", electionEventId);

		// Resolve the dispute.

		LOGGER.info("Resolving dispute...");
		final DisputeResolverOutput disputeResolverOutput = resolve(disputeResolverInput);
		LOGGER.info("Dispute resolved successfully.");

		// Save the output.

		LOGGER.info("Saving dispute resolver output...");
		disputeResolverOutputService.save(disputeResolverOutput);
		LOGGER.info("Dispute resolver output saved successfully.");
	}

	private DisputeResolverOutput resolve(final DisputeResolverInput disputeResolverInput) {
		final String electionEventId = disputeResolverInput.electionEventId();

		// CheckExtractedElectionEventConsistency.
		LOGGER.debug("Checking extracted election event consistency... [electionEventId: {}]", electionEventId);
		final boolean checkExtractedElectionEventConsistencyResult =
				checkExtractedElectionEventConsistencyService.checkExtractedElectionEventConsistency(disputeResolverInput);
		checkState(checkExtractedElectionEventConsistencyResult, "The extracted election events are not consistent. [electionEventId: %s]",
				electionEventId);
		LOGGER.info("\t[1/3] Extracted election event consistency check passed.");

		// CheckVoteConsistency.
		LOGGER.debug("Checking vote consistency... [electionEventId: {}]", electionEventId);
		final boolean checkVoteConsistencyResult = checkVoteConsistencyService.checkVoteConsistency(disputeResolverInput);
		checkState(checkVoteConsistencyResult, "The votes are not consistent. [electionEventId: %s]", electionEventId);
		LOGGER.info("\t[2/3] Vote consistency check passed.");

		// CheckVoteConfirmationConsistency.
		LOGGER.debug("Checking vote confirmation consistency... [electionEventId: {}]", electionEventId);
		final ImmutableList<ResolvedConfirmedVote> resolvedConfirmedVotes = checkVoteConfirmationConsistencyService.checkVoteConfirmationConsistency(
				disputeResolverInput);
		checkState(resolvedConfirmedVotes.size() <= disputeResolverInput.controlComponentExtractedVerificationCardsPayloads().getFirst()
						.getExtractedVerificationCards().size(),
				"The resolved confirmed votes must not be larger than the extracted verification cards. [electionEventId: %s]", electionEventId);
		LOGGER.info("\t[3/3] Vote confirmation consistency check passed.");

		// Return the output of the dispute resolution process.
		return new DisputeResolverOutput(
				new DisputeResolverResolvedConfirmedVotesPayload(electionEventId, resolvedConfirmedVotes)
		);
	}

}
