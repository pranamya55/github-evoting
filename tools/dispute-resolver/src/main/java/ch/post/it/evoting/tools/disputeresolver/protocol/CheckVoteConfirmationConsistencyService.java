/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.protocol;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet.toImmutableSet;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ResolvedConfirmedVote;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCardSet;
import ch.post.it.evoting.tools.disputeresolver.process.input.DisputeResolverInput;

@Service
public class CheckVoteConfirmationConsistencyService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CheckVoteConfirmationConsistencyService.class);

	private final CheckVoteConfirmationConsistencyAlgorithm checkVoteConfirmationConsistencyAlgorithm;

	public CheckVoteConfirmationConsistencyService(final CheckVoteConfirmationConsistencyAlgorithm checkVoteConfirmationConsistencyAlgorithm) {
		this.checkVoteConfirmationConsistencyAlgorithm = checkVoteConfirmationConsistencyAlgorithm;
	}

	/**
	 * Invokes the CheckVoteConfirmationConsistency algorithm.
	 *
	 * @param disputeResolverInput the {@link DisputeResolverInput} containing the control component extracted verification cards payloads and the
	 *                             control component extracted election event payloads. Must be non-null.
	 * @return the dispute resolver’s resolved confirmed votes.
	 * @throws NullPointerException     if the dispute resolver input is null.
	 * @throws IllegalArgumentException if any verification card set id in the control component extracted verification cards payloads is not present
	 *                                  in the control component extracted election event payloads.
	 */
	public ImmutableList<ResolvedConfirmedVote> checkVoteConfirmationConsistency(final DisputeResolverInput disputeResolverInput) {
		checkNotNull(disputeResolverInput);

		// Prepare context.
		final ExtractedElectionEvent extractedElectionEvent = disputeResolverInput.controlComponentExtractedElectionEventPayloads().getFirst()
				.getExtractedElectionEvent();

		// Prepare input.
		final ImmutableList<ControlComponentExtractedVerificationCardsPayload> controlComponentExtractedVerificationCardsPayloads = disputeResolverInput.controlComponentExtractedVerificationCardsPayloads();
		final ImmutableList<ImmutableList<ExtractedVerificationCard>> extractedVerificationCards = controlComponentExtractedVerificationCardsPayloads
				.stream()
				.map(ControlComponentExtractedVerificationCardsPayload::getExtractedVerificationCards)
				.collect(toImmutableList());

		checkArgument(
				extractedElectionEvent.extractedVerificationCardSets().stream()
						.map(ExtractedVerificationCardSet::verificationCardSetId)
						.collect(toImmutableSet())
						.containsAll(
								controlComponentExtractedVerificationCardsPayloads.getFirst().getExtractedVerificationCards().stream()
										.map(ExtractedVerificationCard::verificationCardSetId)
										.collect(toImmutableSet())),
				"All verification card set ids in the control component extracted verification cards payloads must be in the control component extracted election event payloads."
		);

		LOGGER.debug("Performing checkVoteConfirmationConsistency algorithm...");

		// Call the algorithm.
		return checkVoteConfirmationConsistencyAlgorithm.checkVoteConfirmationConsistency(extractedElectionEvent, extractedVerificationCards);
	}
}
