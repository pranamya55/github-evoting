/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.protocol;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.domain.tally.disputeresolver.ResolvedConfirmedVote;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCardSet;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.disputeresolver.ConfirmVoteAgreementAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.disputeresolver.ConfirmVoteAgreementContext;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.disputeresolver.ConfirmVoteAgreementInput;

@Service
public class CheckVoteConfirmationConsistencyAlgorithm {

	private final ConfirmVoteAgreementAlgorithm confirmVoteAgreementAlgorithm;

	public CheckVoteConfirmationConsistencyAlgorithm(final ConfirmVoteAgreementAlgorithm confirmVoteAgreementAlgorithm) {
		this.confirmVoteAgreementAlgorithm = confirmVoteAgreementAlgorithm;
	}

	/**
	 * Checks the consistency of the vote confirmations.
	 *
	 * @param context eee, the {@link ExtractedElectionEvent}. Must be non-null.
	 * @param input   (evc<sub>1</sub>, evc<sub>2</sub>, evc<sub>3</sub>, evc<sub>4</sub>), the CCR's list of {@link ExtractedVerificationCard}. Must
	 *                be non-null.
	 * @return rcv=(rcv<sub>0</sub>,...,rcv<sub>N_C-1</sub>), the dispute resolver's resolved confirmed votes.
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                   	<li>The number of extracted verification cards differs from the number of node ids.</li>
	 *                                   	<li>The context and input do not have the same group.</li>
	 *                                  </ul>
	 */
	@SuppressWarnings("java:S117")
	public ImmutableList<ResolvedConfirmedVote> checkVoteConfirmationConsistency(final ExtractedElectionEvent context,
			final ImmutableList<ImmutableList<ExtractedVerificationCard>> input) {
		checkNotNull(context);
		checkNotNull(input);
		checkArgument(input.size() == ControlComponentNode.ids().size(),
				"There must be as many CCR's extracted verification cards as node ids.");

		// Cross-group check.
		checkArgument(context.encryptionGroup().equals(input.getFirst().getFirst().encryptedVote().getGroup()),
				"The context and input must have the same encryption group.");

		// Context.
		final ExtractedElectionEvent eee = context;
		final String ee = eee.electionEventId();
		final ImmutableMap<String, ExtractedVerificationCardSet> evcs_vector = eee.extractedVerificationCardSets().stream()
				.collect(ImmutableMap.toImmutableMap(ExtractedVerificationCardSet::verificationCardSetId, Function.identity()));

		// Input.
		final ImmutableList<ImmutableList<ExtractedVerificationCard>> evc_vector = input;

		// Operation.
		final Map<String, ResolvedConfirmedVote> rcv = new HashMap<>();

		evc_vector.stream()
				// for j in [1, 4]
				.forEach(evc_j ->
						evc_j.stream()
								// for i in [0, N_S)
								.forEach(evc_j_i -> {
									final String vc_id_j_i = evc_j_i.verificationCardId();
									if (!rcv.containsKey(vc_id_j_i) && !evc_j_i.hashedLongVoteCastReturnCodeShares().isEmpty()) {

										final String vcs_j_i = evc_j_i.verificationCardSetId();
										final ImmutableList<String> hlVCC_vector_j_i = evc_j_i.hashedLongVoteCastReturnCodeShares();

										final ImmutableList<String> L_lVCC = evcs_vector.get(vcs_j_i).longVoteCastReturnCodesAllowList();
										final ConfirmVoteAgreementContext context_j_i = new ConfirmVoteAgreementContext.Builder()
												.setElectionEventId(ee)
												.setVerificationCardSetId(vcs_j_i)
												.setVerificationCardId(vc_id_j_i)
												.setLongVoteCastReturnCodesAllowList(L_lVCC)
												.build();
										final ConfirmVoteAgreementInput input_j_i = new ConfirmVoteAgreementInput(hlVCC_vector_j_i);
										final boolean Verif_j_i = confirmVoteAgreementAlgorithm.confirmVoteAgreement(context_j_i, input_j_i);

										if (Verif_j_i) {
											rcv.put(vc_id_j_i, new ResolvedConfirmedVote(vc_id_j_i, vcs_j_i, hlVCC_vector_j_i));
										}
									}
								})
				);

		return rcv.values().stream()
				.sorted(Comparator.comparing(ResolvedConfirmedVote::verificationCardId)) // rcv <- Order(rcv)
				.collect(toImmutableList());
	}
}
