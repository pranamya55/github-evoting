/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.votingserver.process.Constants.TWO_POW_256;
import static com.google.common.base.Preconditions.checkArgument;

import java.math.BigInteger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Regroups the data of the authentication challenge.
 *
 * @param derivedVoterIdentifier         which corresponds to the credentialId. Must be a valid uuid.
 * @param derivedAuthenticationChallenge the derived authentication challenge. Must be a valid base64 encoded string.
 * @param authenticationNonce            the authentication nonce. Must be in range [0, 2<sup>256</sup>).
 */
public record AuthenticationChallenge(String derivedVoterIdentifier, String derivedAuthenticationChallenge,
									  @JsonSerialize(using = BigIntegerSerializer.class)
									  @JsonDeserialize(using = BigIntegerDeserializer.class)
									  BigInteger authenticationNonce) implements HashableList {

	private static final int l_HB64 = 44;

	/**
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if {@code derivedVoterIdentifier} is not a valid uuid or {@code derivedAuthenticationChallenge} is not a
	 *                                   valid base64 encoded string.
	 * @throws IllegalArgumentException  if {@code derivedAuthenticationChallenge} is not of length {@value l_HB64}.
	 * @throws IllegalArgumentException  if the {@code authenticationNonce} is smaller than 0 or greater than 2<sup>256</sup>.
	 */
	public AuthenticationChallenge {
		validateUUID(derivedVoterIdentifier);
		validateBase64Encoded(derivedAuthenticationChallenge);

		final int challengeLength = derivedAuthenticationChallenge.length();
		checkArgument(challengeLength == l_HB64, "The derived authentication challenge must be of length of l_HB64. [actual: {}, l_BH64: {}]",
				challengeLength, l_HB64);

		checkArgument(authenticationNonce.signum() >= 0);
		checkArgument(authenticationNonce.compareTo(TWO_POW_256) < 0);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				HashableString.from(derivedVoterIdentifier),
				HashableString.from(derivedAuthenticationChallenge),
				HashableBigInteger.from(authenticationNonce)
		);
	}
}
