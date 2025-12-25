/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.disputeresolver;

import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.domain.tally.disputeresolver.ResolvedConfirmedVote;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.disputeresolver.ConfirmVoteAgreementAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.disputeresolver.ConfirmVoteAgreementContext;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.disputeresolver.ConfirmVoteAgreementInput;

@Service
@Profile("dispute-resolution")
public class UpdateConfirmedVotingCardsAlgorithm {

	private final VerificationCardStateService verificationCardStateService;
	private final ConfirmVoteAgreementAlgorithm confirmVoteAgreementAlgorithm;

	public UpdateConfirmedVotingCardsAlgorithm(final VerificationCardStateService verificationCardStateService,
			final ConfirmVoteAgreementAlgorithm confirmVoteAgreementAlgorithm) {
		this.verificationCardStateService = verificationCardStateService;
		this.confirmVoteAgreementAlgorithm = confirmVoteAgreementAlgorithm;
	}

	/**
	 * Updates the list of confirmed voting cards.
	 *
	 * @param context the {@link UpdateConfirmedVotingCardsContext}. Not null.
	 * @param input   the {@link UpdateConfirmedVotingCardsInput}. Not null.
	 * @return {@code true} if the update was successful,{@code false} otherwise.
	 */
	@SuppressWarnings("java:S117")
	boolean updateConfirmedVotingCards(final UpdateConfirmedVotingCardsContext context, final UpdateConfirmedVotingCardsInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Context.
		final String ee = context.electionEventId();
		final ImmutableMap<String, ImmutableList<String>> L_lVCC = context.longVoteCastReturnCodesAllowLists();

		// Input.
		final ImmutableList<ResolvedConfirmedVote> rcv = input.resolvedConfirmedVotes();

		// Operation.
		for (final ResolvedConfirmedVote rcv_i : rcv) {
			final String vc_id = rcv_i.verificationCardId();
			// vc_id ∉ L_sentVotes_
			if (verificationCardStateService.isNotSentVote(vc_id)) {
				return false;
			}
			// vc_id ∉ L_confirmedVotes_j
			if (verificationCardStateService.isNotConfirmedVote(vc_id)) {
				final String vcs = rcv_i.verificationCardSetId();
				final ConfirmVoteAgreementContext confirmVoteAgreementContext = new ConfirmVoteAgreementContext.Builder()
						.setElectionEventId(ee)
						.setVerificationCardSetId(vcs)
						.setVerificationCardId(vc_id)
						.setLongVoteCastReturnCodesAllowList(L_lVCC.get(vcs))
						.build();

				final ConfirmVoteAgreementInput hlVCC_vector = new ConfirmVoteAgreementInput(rcv_i.hashedLongVoteCastReturnCodeShares());
				final boolean verif = confirmVoteAgreementAlgorithm.confirmVoteAgreement(confirmVoteAgreementContext, hlVCC_vector);
				if (verif) {
					// L_confirmedVotes_j ← L_confirmedVotes_j || vc_id
					verificationCardStateService.setConfirmedVote(vc_id);
				} else {
					return false;
				}
			}
		}
		return true;
	}
}
