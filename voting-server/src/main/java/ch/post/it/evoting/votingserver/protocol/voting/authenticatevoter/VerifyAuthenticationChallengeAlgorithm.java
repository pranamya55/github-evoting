/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray.concat;
import static ch.post.it.evoting.cryptoprimitives.utils.ByteArrays.cutToBitLength;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToByteArray;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.stringToByteArray;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.votingserver.process.VerificationCardService;

/**
 * Implements the VerifyAuthenticationChallenge algorithm.
 */
@Service
public class VerifyAuthenticationChallengeAlgorithm {

	private final Hash hash;
	private final Argon2 argon2;
	private final Base64 base64;
	private final VerificationCardService verificationCardService;

	public VerifyAuthenticationChallengeAlgorithm(
			final Hash hash,
			final Argon2 argon2,
			final Base64 base64,
			final VerificationCardService verificationCardService) {
		this.hash = hash;
		this.argon2 = argon2;
		this.base64 = base64;
		this.verificationCardService = verificationCardService;
	}

	/**
	 * Verifies the authentication challenge of a given verification card.
	 * <p>
	 * The verification is successful if all of the following are fulfilled:
	 *     <ul>
	 *         <li>The number of failed authentication attempts for the given verification card must be strictly smaller than 5.</li>
	 *         <li>The derived authentication challenge must not be in the map of used successful authentication challenges per credential ID L<sub>authChallenges</sub>.</li>
	 *         <li>The provided derived authentication challenge must correspond to the one calculated from the server's time stamps (T<sub>1</sub>, T<sub>0</sub>, T<sub>2</sub>).</li>
	 *     </ul>
	 * </p>
	 *
	 * @param context the {@link VerifyAuthenticationChallengeContext} containing the election event id and the credential id. Must be non-null.
	 * @param input   the {@link VerifyAuthenticationChallengeInput} containing the authentication step, the challenges, and the nonce. Must be
	 *                non-null.
	 * @return {@link VerifyAuthenticationChallengeOutput} containing the result of the verification.
	 * @throws NullPointerException if the context or the input is null.
	 */
	@SuppressWarnings("java:S117")
	@Transactional(propagation = Propagation.REQUIRES_NEW) // Ensure the attempt counter is updated at the end of the transaction.
	public VerifyAuthenticationChallengeOutput verifyAuthenticationChallenge(final VerifyAuthenticationChallengeContext context,
			final VerifyAuthenticationChallengeInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Context.
		final String ee = context.electionEventId();
		final String credentialID_id = context.credentialId();

		// Input.
		final String authStep = input.authenticationStep().getName();
		final String hhAuth_id = input.derivedAuthenticationChallenge();
		final String hAuth_id = input.baseAuthenticationChallenge();
		final BigInteger nonce = input.authenticationNonce();

		// Operation.
		final long TS = getTimeStamp();
		final long T_1 = Math.floorDiv(TS, 300);
		final long T_0 = T_1 - 1;
		final long T_2 = T_1 + 1;
		// Corresponds to attempts_id ← L_authAttempts(credentialID_id)
		final int attempts_id = verificationCardService.getAuthenticationAttempts(credentialID_id);

		if (attempts_id >= 5) {
			final String errorMessage = String.format(
					"The credentialId %s already used the maximum number of authentication attempts and is no longer allowed to authenticate.",
					credentialID_id);
			return VerifyAuthenticationChallengeOutput.authenticationAttemptsExceeded(errorMessage);
		}
		// Corresponds to if hhAuth_id ∈ L_authChallenge(credentialID_id)
		if (verificationCardService.getSuccessfulAuthenticationChallenges(credentialID_id).contains(hhAuth_id)) {
			final String errorMessage = String.format(
					"The derivedAuthenticationChallenge %s for the credentialId %s was already used and is no longer allowed to authenticate.",
					hhAuth_id, credentialID_id);
			return VerifyAuthenticationChallengeOutput.authenticationChallengeError(errorMessage);
		}

		final ImmutableByteArray salt_id = cutToBitLength(hash.recursiveHash(HashableString.from(ee),
						HashableString.from(credentialID_id),
						HashableString.from("dAuth"),
						HashableString.from(authStep),
						HashableBigInteger.from(nonce)),
				128);

		for (final long T_i : ImmutableList.of(T_1, T_0, T_2)) {
			final ImmutableByteArray k = concat(stringToByteArray(hAuth_id), stringToByteArray("Auth"), integerToByteArray(BigInteger.valueOf(T_i)));
			final ImmutableByteArray bhhAuth_id_i_prime = argon2.getArgon2id(k, salt_id);
			final String hhAuth_id_i_prime = base64.base64Encode(bhhAuth_id_i_prime);

			if (hhAuth_id_i_prime.equals(hhAuth_id)) {
				// Corresponds to L_authChallenge(credentialID_id) ← L_authChallenge(credentialID_id) || hhAuth_id
				verificationCardService.setLastTimeStepAndSuccessfulAuthenticationChallenge(credentialID_id, T_i, hhAuth_id);
				return VerifyAuthenticationChallengeOutput.success();
			}
		}

		// Corresponds to L_authAttempts(credentialID_id) ← attempts_id + 1
		verificationCardService.incrementAuthenticationAttempts(credentialID_id);
		final String errorMessage = String.format(
				"The Authentication attempt (%d) for credentialID %s failed. Presumably, the voter entered a wrong extended authentication factor. The voter has %d authentication attempts left.",
				attempts_id + 1, credentialID_id, 5 - (attempts_id + 1));
		return VerifyAuthenticationChallengeOutput.invalidExtendedFactor(errorMessage, 5 - (attempts_id + 1));
	}

	private long getTimeStamp() {
		return Instant.now().getEpochSecond();
	}

}
