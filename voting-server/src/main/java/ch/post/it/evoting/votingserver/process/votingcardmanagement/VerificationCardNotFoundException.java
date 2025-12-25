/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.votingcardmanagement;

import java.io.Serial;

public class VerificationCardNotFoundException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1;

	public VerificationCardNotFoundException(final String message) {
		super(message);
	}

}
