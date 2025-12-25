/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationStep;
import ch.post.it.evoting.votingserver.process.voting.VerifyAuthenticationChallengeException;

public class VerificationCardStateValidator {

	private static final String ERROR_TEMPLATE = "The verification card state is not coherent with the current step. [step: %s, verificationCardState: %s]";

	private static final ImmutableList<VerificationCardState> ALLOWED_STATES = ImmutableList.of(
			VerificationCardState.INITIAL,
			VerificationCardState.SENT,
			VerificationCardState.CONFIRMING,
			VerificationCardState.CONFIRMED);

	private VerificationCardStateValidator() {
		// Intentionally blank.
	}

	public static void validateVerificationCardState(final AuthenticationStep authenticationStep, final VerificationCardState verificationCardState) {
		// Prepare error message in case of error.
		final String errorMessage = String.format(ERROR_TEMPLATE, authenticationStep, verificationCardState);

		// If the verification card is in an inactive state, return blocked.
		if (verificationCardState.isInactive()) {
			throw new VerifyAuthenticationChallengeException(VerifyAuthenticationChallengeStatus.VOTING_CARD_BLOCKED, errorMessage);
		}

		// Otherwise check state is coherent with current step.
		switch (authenticationStep) {
		case AUTHENTICATE_VOTER -> {
			if (!ALLOWED_STATES.contains(verificationCardState)) {
				throw new VerifyAuthenticationChallengeException(VerifyAuthenticationChallengeStatus.VOTING_CARD_BLOCKED, errorMessage);
			}
		}
		case SEND_VOTE -> {
			if (!VerificationCardState.INITIAL.equals(verificationCardState)) {
				throw new VerifyAuthenticationChallengeException(VerifyAuthenticationChallengeStatus.AUTHENTICATION_CHALLENGE_ERROR, errorMessage);
			}
		}
		case CONFIRM_VOTE -> {
			if (!VerificationCardState.SENT.equals(verificationCardState) && !VerificationCardState.CONFIRMING.equals(verificationCardState)) {
				throw new VerifyAuthenticationChallengeException(VerifyAuthenticationChallengeStatus.AUTHENTICATION_CHALLENGE_ERROR, errorMessage);
			}
		}
		}
	}
}
