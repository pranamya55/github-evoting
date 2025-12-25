/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.sendvote;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_CHOICE_RETURN_CODE_LENGTH;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

/**
 * Response to the voting-client containing the short Choice Return Codes.
 *
 * @param shortChoiceReturnCodes the short Choice Return Codes. Must be non-null and non-empty.
 */
public record SendVoteResponsePayload(ImmutableList<String> shortChoiceReturnCodes) {

	/**
	 * @throws NullPointerException     if {@code shortChoiceReturnCodes} is null or contains any null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>{@code shortChoiceReturnCodes} is empty.</li>
	 *                                      <li>the short Choice Return Codes are not digits of length {@link ch.post.it.evoting.evotinglibraries.domain.common.Constants#SHORT_VOTE_CAST_RETURN_CODE_LENGTH}.</li>
	 *                                  </ul>
	 */
	public SendVoteResponsePayload {
		checkNotNull(shortChoiceReturnCodes);

		checkArgument(!shortChoiceReturnCodes.isEmpty(), "There must be at least one short Choice Return Code.");
		checkArgument(shortChoiceReturnCodes.stream().parallel()
						.allMatch(shortChoiceReturnCode -> shortChoiceReturnCode.matches("^[0-9]{" + SHORT_CHOICE_RETURN_CODE_LENGTH + "}$")),
				"The short Choice Return Codes must be only digits and have a length of " + SHORT_CHOICE_RETURN_CODE_LENGTH);
	}

}
