/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray.concat;
import static ch.post.it.evoting.cryptoprimitives.utils.ByteArrays.cutToBitLength;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToByteArray;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.stringToByteArray;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.votingserver.process.Constants.TWO_POW_256;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Instant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Factory;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Profile;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.votingserver.process.VerificationCardService;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationStep;

@DisplayName("VerifyAuthenticationChallenge with")
class VerifyAuthenticationChallengeAlgorithmTest {

	private static final ImmutableList<AuthenticationStep> VALID_AUTHENTICATION_STEPS = ImmutableList.of(AuthenticationStep.values());
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final Random random = RandomFactory.createRandom();
	private static final Hash hash = HashFactory.createHash();
	private static final Argon2 argon2 = Argon2Factory.createArgon2(Argon2Profile.TEST);
	private static final Base64 base64 = BaseEncodingFactory.createBase64();

	private static VerificationCardService verificationCardService;
	private static VerifyAuthenticationChallengeAlgorithm algorithm;

	private VerifyAuthenticationChallengeContext context;
	private VerifyAuthenticationChallengeInput input;

	@BeforeAll
	static void setupAll() {
		verificationCardService = mock(VerificationCardService.class);
		algorithm = new VerifyAuthenticationChallengeAlgorithm(hash, argon2, base64, verificationCardService);
	}

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		final String credentialId = uuidGenerator.generate();
		context = new VerifyAuthenticationChallengeContext(electionEventId, credentialId);

		final AuthenticationStep authenticationStep = VALID_AUTHENTICATION_STEPS.get(SECURE_RANDOM.nextInt(VALID_AUTHENTICATION_STEPS.size()));
		final String derivedAuthenticationChallenge = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		final String baseAuthenticationChallenge = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		input = new VerifyAuthenticationChallengeInput(authenticationStep, derivedAuthenticationChallenge, baseAuthenticationChallenge,
				BigInteger.ONE);
	}

	@Test
	@DisplayName("null arguments throws a NullPointerException")
	void verifyAuthenticationChallengeWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> algorithm.verifyAuthenticationChallenge(context, null));
		assertThrows(NullPointerException.class, () -> algorithm.verifyAuthenticationChallenge(null, input));
	}

	@Test
	@DisplayName("with verifiable authentication challenge returns true")
	void verifyAuthenticationChallengeWithCorrectInputReturnsTrue() {
		when(verificationCardService.getAuthenticationAttempts(context.credentialId())).thenReturn(4);
		when(verificationCardService.getLastTimeStep(context.credentialId())).thenReturn(getTimeStep() - 1);
		when(verificationCardService.getSuccessfulAuthenticationChallenges(context.credentialId())).thenReturn(
				ImmutableList.of(random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet)));
		final VerifyAuthenticationChallengeInput verifyingInput = getAuthenticationChallenge(context.electionEventId(), context.credentialId(),
				input.authenticationStep(), input.baseAuthenticationChallenge());
		final VerifyAuthenticationChallengeOutput output = assertDoesNotThrow(() -> algorithm.verifyAuthenticationChallenge(context, verifyingInput));
		assertEquals(VerifyAuthenticationChallengeOutput.success(), output);
	}

	@Test
	@DisplayName("with authentication challenge not verifiable returns false")
	void verifyAuthenticationChallengeWithCorrectInputReturnsFalse() {
		when(verificationCardService.getAuthenticationAttempts(context.credentialId())).thenReturn(4);
		when(verificationCardService.getLastTimeStep(context.credentialId())).thenReturn(getTimeStep() - 1);
		when(verificationCardService.getSuccessfulAuthenticationChallenges(context.credentialId())).thenReturn(
				ImmutableList.of(random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet)));
		final VerifyAuthenticationChallengeOutput output = assertDoesNotThrow(() -> algorithm.verifyAuthenticationChallenge(context, input));
		assertNotEquals(VerifyAuthenticationChallengeOutput.success(), output);

		verify(verificationCardService, Mockito.times(1)).incrementAuthenticationAttempts(context.credentialId());
	}

	@Test
	@DisplayName("derived authentication challenge in list of successful attempts returns false")
	void verifyAuthenticationChallengeWithAuthenticationChallengeAlreadyInListReturnsFalse() {
		when(verificationCardService.getAuthenticationAttempts(context.credentialId())).thenReturn(4);
		when(verificationCardService.getLastTimeStep(context.credentialId())).thenReturn(getTimeStep() - 1);
		when(verificationCardService.getSuccessfulAuthenticationChallenges(context.credentialId())).thenReturn(
				ImmutableList.of(input.derivedAuthenticationChallenge()));
		final String errorMessage = String.format(
				"The derivedAuthenticationChallenge %s for the credentialId %s was already used and is no longer allowed to authenticate.",
				input.derivedAuthenticationChallenge(), context.credentialId());

		final VerifyAuthenticationChallengeOutput output = assertDoesNotThrow(() -> algorithm.verifyAuthenticationChallenge(context, input));
		assertEquals(VerifyAuthenticationChallengeOutput.authenticationChallengeError(errorMessage), output);

		verify(verificationCardService, Mockito.times(0)).incrementAuthenticationAttempts(context.credentialId());
	}

	@Test
	@DisplayName("number of attempts equals 5 or greater returns false")
	void verifyAuthenticationChallengeWithNumberOfAttemptsEqualsFiveReturnsFalse() {
		when(verificationCardService.getAuthenticationAttempts(context.credentialId())).thenReturn(5);
		when(verificationCardService.getLastTimeStep(context.credentialId())).thenReturn(getTimeStep() - 1);
		when(verificationCardService.getSuccessfulAuthenticationChallenges(context.credentialId())).thenReturn(
				ImmutableList.of(random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet)));
		final String errorMessage = String.format(
				"The credentialId %s already used the maximum number of authentication attempts and is no longer allowed to authenticate.",
				context.credentialId());

		final VerifyAuthenticationChallengeOutput output = assertDoesNotThrow(() -> algorithm.verifyAuthenticationChallenge(context, input));
		assertEquals(VerifyAuthenticationChallengeOutput.authenticationAttemptsExceeded(errorMessage), output);

		verify(verificationCardService, Mockito.times(0)).incrementAuthenticationAttempts(context.credentialId());
	}

	private long getTimeStep() {
		return Instant.now().getEpochSecond() / 300;
	}

	private VerifyAuthenticationChallengeInput getAuthenticationChallenge(final String electionEventId, final String credentialId,
			final AuthenticationStep authenticationStep, final String baseAuthenticationChallenge) {
		final BigInteger nonce = random.genRandomInteger(TWO_POW_256);
		final ImmutableByteArray salt = cutToBitLength(
				hash.recursiveHash(HashableString.from(electionEventId), HashableString.from(credentialId), HashableString.from("dAuth"),
						HashableString.from(authenticationStep.getName()), HashableBigInteger.from(nonce)), 128);
		final BigInteger timeStep = BigInteger.valueOf(getTimeStep());
		final ImmutableByteArray k = concat(stringToByteArray(baseAuthenticationChallenge), stringToByteArray("Auth"), integerToByteArray(timeStep));
		final ImmutableByteArray authenticationBytes = argon2.getArgon2id(k, salt);
		final String derivedAuthenticationChallenge = base64.base64Encode(authenticationBytes);

		return new VerifyAuthenticationChallengeInput(authenticationStep, derivedAuthenticationChallenge, baseAuthenticationChallenge, nonce);
	}
}
