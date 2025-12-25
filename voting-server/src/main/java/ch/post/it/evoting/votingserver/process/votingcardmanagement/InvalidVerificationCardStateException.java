/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.votingcardmanagement;

import java.io.Serial;

import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;

public class InvalidVerificationCardStateException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1;

	public InvalidVerificationCardStateException(final String votingCardId, final VerificationCardState state) {
		super(String.format(
				"Verification card not blocked. The current state does not allow to block it. [votingCardId: %s, verificationCardState: %s]",
				votingCardId, state));
	}
}
