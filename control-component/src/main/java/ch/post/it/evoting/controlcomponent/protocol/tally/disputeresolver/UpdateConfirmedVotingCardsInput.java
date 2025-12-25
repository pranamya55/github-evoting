/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.disputeresolver;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.hasNoDuplicates;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.tally.disputeresolver.ResolvedConfirmedVote;

/**
 * Encapsulates the input of the UpdateConfirmedVotingCards algorithm.
 *
 * @param resolvedConfirmedVotes rcv=(rcv<sub>0</sub>,...,rcv<sub>N_C-1</sub>), The dispute resolver’s resolved confirmed votes. Must be non-null.
 */
public record UpdateConfirmedVotingCardsInput(ImmutableList<ResolvedConfirmedVote> resolvedConfirmedVotes) {

	public UpdateConfirmedVotingCardsInput {
		checkNotNull(resolvedConfirmedVotes);
		final ImmutableList<String> verificationCardIds = resolvedConfirmedVotes.stream().map(ResolvedConfirmedVote::verificationCardId)
				.collect(ImmutableList.toImmutableList());
		checkArgument(hasNoDuplicates(verificationCardIds),
				"The list of resolved confirmed votes must not contain duplicate verification card ids.");
	}
}
