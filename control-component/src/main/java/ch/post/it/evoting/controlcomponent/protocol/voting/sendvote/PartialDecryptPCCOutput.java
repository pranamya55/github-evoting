/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;

/**
 * Holds the output of the PartialDecryptPCC algorithm.
 * <ul>
 *     <li>d<sub>j</sub>, the exponentiated gamma elements.</li>
 *     <li>&pi;<sub>decPCC,j</sub>, the exponentiation proofs.</li>
 * </ul>
 */
public record PartialDecryptPCCOutput(GroupVector<GqElement, GqGroup> exponentiatedGammas,
									  GroupVector<ExponentiationProof, ZqGroup> exponentiationProofs) {

	public PartialDecryptPCCOutput {
		checkNotNull(exponentiatedGammas);
		checkNotNull(exponentiationProofs);

		// Size checks.
		checkArgument(exponentiatedGammas.size() == exponentiationProofs.size(),
				"There must be as many exponentiated gammas as there are exponentiation proofs.");

		// Cross-group checks.
		checkArgument(exponentiatedGammas.getGroup().hasSameOrderAs(exponentiationProofs.getGroup()),
				"The exponentiated gammas and exponentiation proofs do not have the same group order.");

	}

}
