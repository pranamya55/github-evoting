/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.protocol;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;
import ch.post.it.evoting.tools.disputeresolver.process.input.DisputeResolverInput;

@Service
public class CheckVoteConsistencyService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CheckVoteConsistencyService.class);

	private final CheckVoteConsistencyAlgorithm checkVoteConsistencyAlgorithm;

	public CheckVoteConsistencyService(final CheckVoteConsistencyAlgorithm checkVoteConsistencyAlgorithm) {
		this.checkVoteConsistencyAlgorithm = checkVoteConsistencyAlgorithm;
	}

	/**
	 * Invokes the CheckVoteConsistency algorithm.
	 *
	 * @param disputeResolverInput the {@link DisputeResolverInput} containing the control component extracted verification cards payloads. Must be
	 *                             non-null.
	 * @return true if the CheckVoteConsistency algorithm returns true, false otherwise.
	 * @throws NullPointerException if the dispute resolver input is null.
	 */
	public boolean checkVoteConsistency(final DisputeResolverInput disputeResolverInput) {
		checkNotNull(disputeResolverInput);

		// Prepare input.
		final ImmutableList<ImmutableList<ExtractedVerificationCard>> extractedVerificationCards = disputeResolverInput.controlComponentExtractedVerificationCardsPayloads()
				.stream()
				.map(ControlComponentExtractedVerificationCardsPayload::getExtractedVerificationCards)
				.collect(toImmutableList());

		LOGGER.debug("Performing CheckVoteConsistency algorithm...");

		// Call the algorithm.
		return checkVoteConsistencyAlgorithm.checkVoteConsistency(extractedVerificationCards);
	}
}
