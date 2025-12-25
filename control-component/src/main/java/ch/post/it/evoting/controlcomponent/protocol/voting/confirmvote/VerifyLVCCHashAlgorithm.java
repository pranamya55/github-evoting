/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote;

import static ch.post.it.evoting.cryptoprimitives.hashing.HashableList.toHashableList;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.LongVoteCastReturnCodesAllowList;
import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Base64;

/**
 * Implements the VerifyLVCCHash algorithm.
 */
@Service
@SuppressWarnings("java:S117")
public class VerifyLVCCHashAlgorithm {

	private final Hash hash;
	private final Base64 base64;
	private final VerificationCardStateService verificationCardStateService;

	public VerifyLVCCHashAlgorithm(
			final Hash hash,
			final Base64 base64,
			final VerificationCardStateService verificationCardStateService) {
		this.hash = hash;
		this.base64 = base64;
		this.verificationCardStateService = verificationCardStateService;
	}

	/**
	 * Verifies the long Vote Cast Return Code hash.
	 * <p>
	 * By contract the context ids are verified prior to calling this method.
	 * </p>
	 *
	 * @param context the {@link LVCCHashContext}. Not null.
	 * @param input   the {@link VerifyLVCCHashInput}. Not null.
	 * @return true if the hash is valid, false otherwise.
	 * @throws NullPointerException     if any input parameter is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>The verification card is not in L<sub>sentVotes,j</sub>.</li>
	 *                                      <li>The verification card is in in L<sub>confirmedVotes, j</sub></li>
	 *                                  </ul>
	 */
	public boolean verifyLVCCHash(final LVCCHashContext context, final VerifyLVCCHashInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Context.
		final int j = context.nodeId();
		final String ee = context.electionEventId();
		final String vcs = context.verificationCardSetId();
		final String vc_id = context.verificationCardId();

		// Input.
		final LongVoteCastReturnCodesAllowList L_lVCC = input.getLongVoteCastReturnCodesAllowList();
		final String h_lVCC_j_id = input.getCcrjHashedLongVoteCastReturnCode();
		final ImmutableList<String> h_lVCC_j_id_hat = input.getOtherCCRsHashedLongVoteCastReturnCodes();
		final ImmutableList<String> hlVCC_id = getOrderedhlVCC(j, h_lVCC_j_id, h_lVCC_j_id_hat);

		//Require.
		// vc_id ∈ L_sentVotes,j.
		checkArgument(verificationCardStateService.isSentVote(vc_id),
				"The CCR_j did not compute the long Choice Return Code shares for the verification card. [vc_id: %s]", vc_id);
		// vc_id ∉ L_confirmedVotes,j
		checkArgument(verificationCardStateService.isNotConfirmedVote(vc_id),
				"The CCR_j did already confirm the long Choice Return Code shares for the verification card. [vc_id: %s]", vc_id);

		// Operation.
		final HashableList i_aux = Stream.of("VerifyLVCCHash", ee, vcs, vc_id)
				.map(HashableString::from)
				.collect(toHashableList());

		final HashableString hlVCC_id_1 = HashableString.from(hlVCC_id.get(0));
		final HashableString hlVCC_id_2 = HashableString.from(hlVCC_id.get(1));
		final HashableString hlVCC_id_3 = HashableString.from(hlVCC_id.get(2));
		final HashableString hlVCC_id_4 = HashableString.from(hlVCC_id.get(3));
		final String hhlVCC_id = base64.base64Encode(hash.recursiveHash(i_aux, hlVCC_id_1, hlVCC_id_2, hlVCC_id_3, hlVCC_id_4));

		if (L_lVCC.exists(hhlVCC_id)) {
			// L_confirmedVotes,j = L_confirmedVotes,j || (vc_id)
			verificationCardStateService.setConfirmedVote(vc_id);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Orders by node id the list containing all the hashed Long Vote Cast Return Codes.
	 */
	private ImmutableList<String> getOrderedhlVCC(final int j, final String h_lVCC_j_id, final ImmutableList<String> h_lVCC_j_id_hat) {
		final List<String> hlVCC = new ArrayList<>(h_lVCC_j_id_hat.asList());
		if (j > hlVCC.size()) {
			hlVCC.add(h_lVCC_j_id);
		} else {
			hlVCC.add(j - 1, h_lVCC_j_id);
		}
		return ImmutableList.from(hlVCC);
	}

}
