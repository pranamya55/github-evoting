/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting;

public class CredentialIdNotFoundException extends RuntimeException {

	public CredentialIdNotFoundException(final String message) {
		super(message);
	}

}
