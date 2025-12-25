/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static ch.post.it.evoting.votingserver.process.Constants.TWO_POW_256;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;

import ch.post.it.evoting.votingserver.process.voting.AuthenticationStep;

/**
 * Regroups the input values needed by the VerifyAuthenticationChallenge algorithm.
 *
 * <ul>
 *     <li>authStep, the authentication step. Not null.</li>
 *     <li>hhAuth<sub>id</sub>, the derived authentication challenge. Not null.</li>
 *     <li>hAuth<sub>id</sub>, the base authentication challenge. Not null.</li>
 *     <li>nonce, the authentication nonce. Not null and in range [0, 2<sup>256</sup>).</li>
 * </ul>
 */
public record VerifyAuthenticationChallengeInput(AuthenticationStep authenticationStep, String derivedAuthenticationChallenge,
												 String baseAuthenticationChallenge, BigInteger authenticationNonce) {

	public VerifyAuthenticationChallengeInput {
		checkNotNull(authenticationStep);
		validateBase64Encoded(derivedAuthenticationChallenge);
		validateBase64Encoded(baseAuthenticationChallenge);
		checkNotNull(authenticationNonce);

		checkArgument(authenticationNonce.signum() >= 0, "The authentication nonce must be positive.");
		checkArgument(authenticationNonce.compareTo(TWO_POW_256) < 0, "The authentication nonce must be at most %s.", TWO_POW_256);

		checkArgument(derivedAuthenticationChallenge.length() == BASE64_ENCODED_HASH_OUTPUT_LENGTH,
				"The length of the derived authentication challenge is not equal to l_HB64.");
		checkArgument(baseAuthenticationChallenge.length() == BASE64_ENCODED_HASH_OUTPUT_LENGTH,
				"The length of the base authentication challenge is not equal to l_HB64.");
	}
}
