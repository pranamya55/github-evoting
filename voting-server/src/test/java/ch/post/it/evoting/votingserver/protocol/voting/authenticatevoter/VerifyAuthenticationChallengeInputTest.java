/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.votingserver.process.Constants.TWO_POW_256;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationStep;

class VerifyAuthenticationChallengeInputTest {

	private static final Random RANDOM = RandomFactory.createRandom();
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();

	private AuthenticationStep authenticationStep;
	private String derivedAuthenticationChallenge;
	private String baseAuthenticationChallenge;
	private BigInteger authenticationNonce;

	@BeforeEach
	void setup() {
		authenticationStep = AuthenticationStep.values()[SECURE_RANDOM.nextInt(AuthenticationStep.values().length)];
		derivedAuthenticationChallenge = RANDOM.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		baseAuthenticationChallenge = RANDOM.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		authenticationNonce = RANDOM.genRandomInteger(TWO_POW_256);
	}

	@Test
	void constructWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class,
				() -> new VerifyAuthenticationChallengeInput(null, derivedAuthenticationChallenge, baseAuthenticationChallenge, authenticationNonce));
		assertThrows(NullPointerException.class,
				() -> new VerifyAuthenticationChallengeInput(authenticationStep, null, baseAuthenticationChallenge, authenticationNonce));
		assertThrows(NullPointerException.class,
				() -> new VerifyAuthenticationChallengeInput(authenticationStep, derivedAuthenticationChallenge, null, authenticationNonce));
		assertThrows(NullPointerException.class,
				() -> new VerifyAuthenticationChallengeInput(authenticationStep, derivedAuthenticationChallenge, baseAuthenticationChallenge, null));
	}

	@Test
	void constructWithChallengesNotBase64Throws() {
		final String badDerivedAuthenticationChallenge = "invalid!";
		assertThrows(FailedValidationException.class,
				() -> new VerifyAuthenticationChallengeInput(authenticationStep, badDerivedAuthenticationChallenge, baseAuthenticationChallenge,
						authenticationNonce));
		final String badBaseAuthenticationChallenge = "invalid!";
		assertThrows(FailedValidationException.class,
				() -> new VerifyAuthenticationChallengeInput(authenticationStep, derivedAuthenticationChallenge, badBaseAuthenticationChallenge,
						authenticationNonce));
	}

	@Test
	void constructWithNegativeAuthenticationNonceThrows() {
		final BigInteger badAuthenticationNonce = BigInteger.valueOf(-1);
		assertThrows(IllegalArgumentException.class,
				() -> new VerifyAuthenticationChallengeInput(authenticationStep, derivedAuthenticationChallenge, baseAuthenticationChallenge,
						badAuthenticationNonce));
	}

	@Test
	void constructWithValidInputDoesNotThrow() {
		assertDoesNotThrow(
				() -> new VerifyAuthenticationChallengeInput(authenticationStep, derivedAuthenticationChallenge, baseAuthenticationChallenge,
						authenticationNonce));
	}
}
