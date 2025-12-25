/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;

public record SuccessfulAuthenticationAttempts(ImmutableList<String> successfulChallenges) {

	public SuccessfulAuthenticationAttempts {
		checkNotNull(successfulChallenges);
		successfulChallenges.forEach(Validations::validateBase64Encoded);
	}
}
