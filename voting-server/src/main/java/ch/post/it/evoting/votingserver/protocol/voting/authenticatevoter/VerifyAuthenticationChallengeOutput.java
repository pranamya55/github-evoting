/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter;

import static ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus.AUTHENTICATION_ATTEMPTS_EXCEEDED;
import static ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus.AUTHENTICATION_CHALLENGE_ERROR;
import static ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus.EXTENDED_FACTOR_INVALID;
import static ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus.SUCCESS;

import java.util.Objects;

/**
 * Regroups the output values of the VerifyAuthenticationChallenge algorithm.
 *
 * <ul>
 *     <li>If the verification is successful, the status {@link VerifyAuthenticationChallengeStatus#SUCCESS}.</li>
 *     <li>Otherwise, the status {@link VerifyAuthenticationChallengeStatus}, the error message and the number of attempts left.</li>
 * </ul>
 */
public class VerifyAuthenticationChallengeOutput {

	private final VerifyAuthenticationChallengeStatus status;
	private final String errorMessage;
	private final Integer attemptsLeft;

	private VerifyAuthenticationChallengeOutput(final VerifyAuthenticationChallengeStatus status) {
		this.status = status;
		this.errorMessage = null;
		this.attemptsLeft = null;
	}

	private VerifyAuthenticationChallengeOutput(final VerifyAuthenticationChallengeStatus status,
			final String errorMessage) {
		this.status = status;
		this.errorMessage = errorMessage;
		this.attemptsLeft = null;
	}

	private VerifyAuthenticationChallengeOutput(final VerifyAuthenticationChallengeStatus status,
			final String errorMessage, final int attemptsLeft) {
		this.status = status;
		this.errorMessage = errorMessage;
		this.attemptsLeft = attemptsLeft;
	}

	public static VerifyAuthenticationChallengeOutput success() {
		return new VerifyAuthenticationChallengeOutput(SUCCESS);
	}

	public static VerifyAuthenticationChallengeOutput authenticationChallengeError(final String errorMessage) {
		return new VerifyAuthenticationChallengeOutput(AUTHENTICATION_CHALLENGE_ERROR, errorMessage);
	}

	public static VerifyAuthenticationChallengeOutput authenticationAttemptsExceeded(final String errorMessage) {
		return new VerifyAuthenticationChallengeOutput(AUTHENTICATION_ATTEMPTS_EXCEEDED, errorMessage, 0);
	}

	public static VerifyAuthenticationChallengeOutput invalidExtendedFactor(final String errorMessage, final int attemptsLeft) {
		return new VerifyAuthenticationChallengeOutput(EXTENDED_FACTOR_INVALID, errorMessage, attemptsLeft);
	}

	public VerifyAuthenticationChallengeStatus getStatus() {
		return status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Integer getAttemptsLeft() {
		return attemptsLeft;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final VerifyAuthenticationChallengeOutput output = (VerifyAuthenticationChallengeOutput) o;
		return status == output.status && Objects.equals(errorMessage, output.errorMessage) && Objects.equals(attemptsLeft,
				output.attemptsLeft);
	}

	@Override
	public int hashCode() {
		return Objects.hash(status, errorMessage, attemptsLeft);
	}

	public enum VerifyAuthenticationChallengeStatus {
		SUCCESS,
		VOTING_CARD_BLOCKED,
		BALLOT_BOX_NOT_STARTED,
		BALLOT_BOX_ENDED,
		AUTHENTICATION_CHALLENGE_ERROR,
		EXTENDED_FACTOR_INVALID,
		AUTHENTICATION_ATTEMPTS_EXCEEDED
	}

}
