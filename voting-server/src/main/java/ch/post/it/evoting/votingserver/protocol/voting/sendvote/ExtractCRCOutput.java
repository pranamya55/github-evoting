/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.sendvote;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_CHOICE_RETURN_CODE_LENGTH;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

/**
 * Regroups the output of the ExtractCRC algorithm.
 *
 * <ul>
 *     <li>CC<sub>id</sub>, Short Choice Return Codes. Not null.</li>
 * </ul>
 */
public record ExtractCRCOutput(ImmutableList<String> shortChoiceReturnCodes) {
	private static final int L_CC = SHORT_CHOICE_RETURN_CODE_LENGTH;

	/**
	 * @param shortChoiceReturnCodes CC<sub>id</sub> ∈ ((A<sub>10</sub>)<sup>l<sub>CC</sup></sup>)<sup>&psi;</sup>, Short Choice Return Codes.
	 * @throws NullPointerException     if {@code shortChoiceReturnCodes} is null.
	 * @throws IllegalArgumentException if the {@code shortChoiceReturnCodes} is not decimal and values length are not l<sub>CC</sub>.
	 */
	public ExtractCRCOutput {
		checkNotNull(shortChoiceReturnCodes);

		checkArgument(shortChoiceReturnCodes.stream().parallel().allMatch(cc -> cc.matches("^[0-9]{" + L_CC + "}$")),
				"Short Choice Return Codes values must be only digits and have a length of " + L_CC);

		checkArgument(!shortChoiceReturnCodes.isEmpty(), "There must be at least one Short Choice Return Code.");
		checkArgument(shortChoiceReturnCodes.size() <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
				"There must be at most psi_sup Short Choice Return Codes. [psi: %s, psi_sup: %s]", shortChoiceReturnCodes.size(),
				MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);
	}
}
