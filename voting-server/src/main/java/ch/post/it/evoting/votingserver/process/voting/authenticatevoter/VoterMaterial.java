/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.authenticatevoter;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_CHOICE_RETURN_CODE_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_VOTE_CAST_RETURN_CODE_LENGTH;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonInclude;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.ElectionTexts;
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.VoteTexts;

/**
 * Holds the information needed by the voting client during authentication. The presence of fields depends on if it is (and when) a re-login.
 * <p>
 * <ul>
 *     <li>first authentication: only {@code votesTexts} and/or {@code electionsTexts} are present.</li>
 *     <li>authentication after vote has been sent, before confirmed: {@code votesTexts}, {@code electionsTexts} and {@code shortChoiceReturnCodes} are present.</li>
 *     <li>authentication after vote has been confirmed: only {@code shortVoteCastReturnCode} is present.</li>
 * </ul>
 *
 * @param votesTexts              the votes' texts. Non-null during first authentication.
 * @param electionsTexts          the elections' texts as a json string. Non-null during first authentication.
 * @param shortChoiceReturnCodes  the short Choice Return Codes. Non-null after vote has been sent but before cast. Must be non-empty.
 * @param shortVoteCastReturnCode the short Vote Cast Return Code. Non-null after vote has been cast.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VoterMaterial(ImmutableList<VoteTexts> votesTexts, ImmutableList<ElectionTexts> electionsTexts,
							ImmutableList<String> shortChoiceReturnCodes,
							String shortVoteCastReturnCode) {

	/**
	 * @throws IllegalArgumentException if {@code shortChoiceReturnCodes} is non-null but empty.
	 */
	public VoterMaterial {
		if (shortChoiceReturnCodes != null) {
			checkArgument(!shortChoiceReturnCodes.isEmpty(), "There must be at least one short Choice Return Code.");
			checkArgument(shortChoiceReturnCodes.stream().parallel()
							.allMatch(shortChoiceReturnCode -> shortChoiceReturnCode.matches("^[0-9]{" + SHORT_CHOICE_RETURN_CODE_LENGTH + "}$")),
					"The short Choice Return Codes must be only digits and have a length of " + SHORT_CHOICE_RETURN_CODE_LENGTH);
		}
		if (shortVoteCastReturnCode != null) {
			checkArgument(shortVoteCastReturnCode.matches("^[0-9]{" + SHORT_VOTE_CAST_RETURN_CODE_LENGTH + "}$"),
					"The short Vote Cast Return Code must be only digits and have a length of " + SHORT_VOTE_CAST_RETURN_CODE_LENGTH);
		}
	}

	/**
	 * First authentication case.
	 *
	 * @throws NullPointerException if any parameter is null.
	 */
	public VoterMaterial(final ImmutableList<VoteTexts> votesTexts, final ImmutableList<ElectionTexts> electionsTexts) {
		this(checkNotNull(votesTexts), checkNotNull(electionsTexts), null, null);
		checkArgument(!votesTexts.isEmpty() || !electionsTexts.isEmpty(), "There must be at least one vote texts or election texts.");
	}

	/**
	 * Authentication after vote has been sent.
	 *
	 * @throws NullPointerException if any parameter is null.
	 */
	public VoterMaterial(final ImmutableList<VoteTexts> votesTexts, final ImmutableList<ElectionTexts> electionsTexts,
			final ImmutableList<String> shortChoiceReturnCodes) {
		this(checkNotNull(votesTexts), checkNotNull(electionsTexts), checkNotNull(shortChoiceReturnCodes), null);
		checkArgument(!votesTexts.isEmpty() || !electionsTexts.isEmpty(), "There must be at least one vote texts or election texts.");
	}

	/**
	 * Authentication after vote has been confirmed.
	 *
	 * @throws NullPointerException if {@code shortVoteCastReturnCode} is null.
	 */
	public VoterMaterial(final String shortVoteCastReturnCode) {
		this(ImmutableList.emptyList(), ImmutableList.emptyList(), null, checkNotNull(shortVoteCastReturnCode));
	}

}
