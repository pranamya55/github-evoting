/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

/**
 * Regroups the context values needed by the VerifyAuthenticationChallenge algorithm.
 *
 * <ul>
 *     <li>ee, the election event id. Not null and valid UUID.</li>
 *     <li>credentialID<sub>id</sub>, the derived voter identifier. Non-null and a valid UUID.</li>
 * </ul>
 */
public record VerifyAuthenticationChallengeContext(String electionEventId, String credentialId) {

	public VerifyAuthenticationChallengeContext {
		validateUUID(electionEventId);
		validateUUID(credentialId);
	}

}
