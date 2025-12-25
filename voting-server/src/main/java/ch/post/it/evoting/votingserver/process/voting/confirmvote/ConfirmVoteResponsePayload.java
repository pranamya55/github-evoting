/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_VOTE_CAST_RETURN_CODE_LENGTH;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Response to the voting-client containing the short Vote Cast Return Code.
 *
 * @param shortVoteCastReturnCode the short Vote Cast Return code. Must be non.null.
 */
public record ConfirmVoteResponsePayload(String shortVoteCastReturnCode) {

	/**
	 * @throws NullPointerException     if {@code shortVoteCastReturnCode} is null.
	 * @throws IllegalArgumentException if {@code shortVoteCastReturnCode} is not digits of length
	 *                                  {@link ch.post.it.evoting.evotinglibraries.domain.common.Constants#SHORT_VOTE_CAST_RETURN_CODE_LENGTH}.
	 */
	public ConfirmVoteResponsePayload {
		checkNotNull(shortVoteCastReturnCode);
		checkArgument(shortVoteCastReturnCode.matches("^[0-9]{" + SHORT_VOTE_CAST_RETURN_CODE_LENGTH + "}$"),
				"The short Vote Cast Return Code must be only digits and have a length of " + SHORT_VOTE_CAST_RETURN_CODE_LENGTH);
	}

}
