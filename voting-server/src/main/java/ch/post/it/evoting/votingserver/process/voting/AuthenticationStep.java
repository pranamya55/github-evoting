/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting;

public enum AuthenticationStep {

	AUTHENTICATE_VOTER("authenticateVoter"),
	SEND_VOTE("sendVote"),
	CONFIRM_VOTE("confirmVote");

	private final String stepName;

	AuthenticationStep(final String stepName) {
		this.stepName = stepName;
	}

	/**
	 * @return the camel case name corresponding to this enum value, as described in the specification.
	 */
	public String getName() {
		return this.stepName;
	}

}
