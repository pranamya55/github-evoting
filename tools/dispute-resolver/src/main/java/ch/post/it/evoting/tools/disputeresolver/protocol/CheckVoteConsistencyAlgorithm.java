/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.protocol;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;

@Service
public class CheckVoteConsistencyAlgorithm {

	private final Base64 base64;
	private final Hash hash;

	public CheckVoteConsistencyAlgorithm(final Base64 base64, final Hash hash) {
		this.base64 = base64;
		this.hash = hash;
	}

	/**
	 * Checks the consistency of the submitted, encrypted votes.
	 *
	 * @param input (evc<sub>1</sub>, evc<sub>2</sub>, evc<sub>3</sub>, evc<sub>4</sub>), the CCR's list of {@link ExtractedVerificationCard}. Must be
	 *              non-null.
	 * @return true if the submitted, encrypted votes are consistent, false otherwise.
	 * @throws NullPointerException     if the input is null.
	 * @throws IllegalArgumentException if the number of extracted verification cards differs from the number of node ids.
	 */
	@SuppressWarnings("java:S117")
	public boolean checkVoteConsistency(final ImmutableList<ImmutableList<ExtractedVerificationCard>> input) {
		checkNotNull(input);
		checkArgument(input.size() == ControlComponentNode.ids().size(),
				"There must be as many CCR's extracted verification cards as node ids.");

		// Input.
		final ImmutableList<ImmutableList<ExtractedVerificationCard>> evc_vector = input;

		// Operation.
		return evc_vector.stream()
				// for j in [1, 4]
				.map(evc_j -> {
					final ImmutableList<HashableList> h_evc_j_vector = evc_j.stream()
							// for i in [0, N_S)
							.map(evc_j_i -> {
								final HashableString vc_id_j_i = HashableString.from(evc_j_i.verificationCardId());
								final HashableString vcs_j_i = HashableString.from(evc_j_i.verificationCardSetId());
								final HashableList h_E1_j_i = evc_j_i.encryptedVote();

								return HashableList.of(vc_id_j_i, vcs_j_i, h_E1_j_i);
							}).collect(toImmutableList());

					final HashableList h_j = HashableList.from(h_evc_j_vector);

					return base64.base64Encode(hash.recursiveHash(h_j));
				})
				// d_1 = d_2 = d_3 = d_4
				.distinct()
				.count() == 1;
	}
}
