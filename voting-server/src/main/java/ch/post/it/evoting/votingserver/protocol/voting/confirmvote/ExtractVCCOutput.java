/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_VOTE_CAST_RETURN_CODE_LENGTH;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Regroups the output of the ExtractVCC algorithm.
 *
 * <ul>
 *     <li>VCC<sub>id</sub>, short Vote Cast Return Codes.</li>
 * </ul>
 */
public record ExtractVCCOutput(String shortVoteCastReturnCode) {

	private static final int L_VCC = SHORT_VOTE_CAST_RETURN_CODE_LENGTH;

	/**
	 * @param shortVoteCastReturnCode VCC<sub>id</sub> ∈ ((A<sub>10</sub>)<sup>L<sub>CC</sup></sup>)<sup>&#x1D713;</sup>, short Vote Cast Return
	 *                                Codes.
	 * @throws NullPointerException     if {@code shortVoteCastReturnCode} is null.
	 * @throws IllegalArgumentException if {@code shortVoteCastReturnCode} is not decimal and value length is not L_VCC.
	 */
	public ExtractVCCOutput {
		checkNotNull(shortVoteCastReturnCode);
		checkArgument(shortVoteCastReturnCode.matches("^[0-9]{" + L_VCC + "}$"),
				"Short Vote Cast Return Code value must be only digits and have a length of " + L_VCC);

	}

}
