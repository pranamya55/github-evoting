/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.disputeresolver;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet.toImmutableSet;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.domain.tally.disputeresolver.DisputeResolverResolvedConfirmedVotesPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ResolvedConfirmedVote;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@Service
@Profile("dispute-resolution")
public class UpdateConfirmedVotingCardsService {

	private final int nodeId;
	private final UpdateConfirmedVotingCardsAlgorithm updateConfirmedVotingCardsAlgorithm;

	public UpdateConfirmedVotingCardsService(
			@Value("${nodeID}")
			final int nodeId,
			final UpdateConfirmedVotingCardsAlgorithm updateConfirmedVotingCardsAlgorithm) {
		this.nodeId = nodeId;
		this.updateConfirmedVotingCardsAlgorithm = updateConfirmedVotingCardsAlgorithm;
	}

	/**
	 * Call the UpdateConfirmedVotingCards algorithm.
	 *
	 * @param electionEventId                              the election event id. Must be non-null and a valid UUID.
	 * @param longVoteCastReturnCodesAllowLists            the map of long Vote Cast Return Codes allow list by verification card set id. Must be
	 *                                                     non-null.
	 * @param disputeResolverResolvedConfirmedVotesPayload the dispute resolver's list of {@link ResolvedConfirmedVote}. Must be non-null.
	 * @return the output of the UpdateConfirmedVotingCards algorithm.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if {@code electionEventId} or the keys of {@code longVoteCastReturnCodesAllowLists} are not a valid UUID.
	 * @throws IllegalArgumentException  if
	 *                                   <ul>
	 *                                    <li>The node id is not part of the known node ids.</li>
	 *                                    <li>The dispute resolver resolved confirmed votes payload does not correspond to the given election event id.</li>
	 *                                    <li>The dispute resolver resolved confirmed votes payload contains unknown verification card set ids.</li>
	 *                                   </ul>
	 */
	public boolean updateConfirmedVotingCards(final String electionEventId,
			final ImmutableMap<String, ImmutableList<String>> longVoteCastReturnCodesAllowLists,
			final DisputeResolverResolvedConfirmedVotesPayload disputeResolverResolvedConfirmedVotesPayload) {
		validateUUID(electionEventId);
		checkArgument(!checkNotNull(longVoteCastReturnCodesAllowLists).isEmpty(),
				"The long Vote Cast Return Codes allow lists must have at least one element.");
		checkArgument(checkNotNull(disputeResolverResolvedConfirmedVotesPayload).getElectionEventId().equals(electionEventId),
				"The dispute resolver resolved confirmed votes payload does not correspond to the given election event id.");

		final ImmutableList<ResolvedConfirmedVote> resolvedConfirmedVotes = disputeResolverResolvedConfirmedVotesPayload.getResolvedConfirmedVotes();
		checkArgument(longVoteCastReturnCodesAllowLists.keySet()
						.containsAll(resolvedConfirmedVotes.stream().map(ResolvedConfirmedVote::verificationCardSetId).collect(toImmutableSet())),
				"The dispute resolver resolved confirmed votes payload contains unknown verification card set ids.");

		final UpdateConfirmedVotingCardsContext context = new UpdateConfirmedVotingCardsContext(nodeId, electionEventId,
				longVoteCastReturnCodesAllowLists);

		final UpdateConfirmedVotingCardsInput input = new UpdateConfirmedVotingCardsInput(resolvedConfirmedVotes);

		return updateConfirmedVotingCardsAlgorithm.updateConfirmedVotingCards(context, input);
	}

}
