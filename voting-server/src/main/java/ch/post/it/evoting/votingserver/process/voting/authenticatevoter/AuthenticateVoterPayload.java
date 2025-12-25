/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.authenticatevoter;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationChallenge;

/**
 * Authenticate voter request payload sent by the voting-client.
 *
 * @param electionEventId         the election event id. Must be a valid uuid.
 * @param authenticationChallenge the authentication challenge. Must be non-null.
 */
@JsonDeserialize(using = AuthenticateVoterPayloadDeserializer.class)
public record AuthenticateVoterPayload(String electionEventId, AuthenticationChallenge authenticationChallenge) {

	/**
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if {@code electionEventId} is not valid.
	 */
	public AuthenticateVoterPayload {
		validateUUID(electionEventId);
		checkNotNull(authenticationChallenge);
	}
}