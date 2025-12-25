/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting;

import java.util.Optional;

import ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus;

public class VerifyAuthenticationChallengeException extends RuntimeException {

	private final VerifyAuthenticationChallengeStatus errorStatus;
	private final Integer remainingAttempts;
	private final String errorMessage;

	public VerifyAuthenticationChallengeException(final VerifyAuthenticationChallengeStatus errorStatus, final String errorMessage) {
		this.errorStatus = errorStatus;
		this.remainingAttempts = null;
		this.errorMessage = errorMessage;
	}

	public VerifyAuthenticationChallengeException(final VerifyAuthenticationChallengeStatus errorStatus,
			final Integer remainingAttempts, final String errorMessage) {
		this.errorStatus = errorStatus;
		this.remainingAttempts = remainingAttempts;
		this.errorMessage = errorMessage;
	}

	public VerifyAuthenticationChallengeStatus getErrorStatus() {
		return errorStatus;
	}

	public Optional<Integer> getRemainingAttempts() {
		return Optional.ofNullable(remainingAttempts);
	}

	public String getErrorMessage() {
		return errorMessage;
	}

}
