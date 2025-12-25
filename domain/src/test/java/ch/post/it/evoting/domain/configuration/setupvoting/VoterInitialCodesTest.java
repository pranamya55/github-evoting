/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BCK_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SVK_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.math.UsabilityBase32Alphabet;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class VoterInitialCodesTest {

	private static final Random RANDOM = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final Alphabet usabilityBase32Alphabet = UsabilityBase32Alphabet.getInstance();

	private String voterIdentification;
	private String votingCardId;
	private String verificationCardId;
	private String startVotingKey;
	private String extendedAuthenticationFactor;
	private String ballotCastingKey;

	@BeforeEach
	void setup() {
		voterIdentification = RANDOM.genRandomString(50, base64Alphabet);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		votingCardId = uuidGenerator.generate();
		verificationCardId = uuidGenerator.generate();

		startVotingKey = ElectionSetupUtils.genStartVotingKey();
		extendedAuthenticationFactor = String.join("", RANDOM.genUniqueDecimalStrings(4, 2));
		ballotCastingKey = RANDOM.genUniqueDecimalStrings(BCK_LENGTH, 1).get(0);
	}

	@Test
	void constructWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class,
				() -> new VoterInitialCodes(null, votingCardId, verificationCardId, startVotingKey, extendedAuthenticationFactor, ballotCastingKey));
		assertThrows(NullPointerException.class,
				() -> new VoterInitialCodes(voterIdentification, null, verificationCardId, startVotingKey, extendedAuthenticationFactor,
						ballotCastingKey));
		assertThrows(NullPointerException.class,
				() -> new VoterInitialCodes(voterIdentification, votingCardId, null, startVotingKey, extendedAuthenticationFactor, ballotCastingKey));
		assertThrows(NullPointerException.class,
				() -> new VoterInitialCodes(voterIdentification, votingCardId, verificationCardId, null, extendedAuthenticationFactor,
						ballotCastingKey));
		assertThrows(NullPointerException.class,
				() -> new VoterInitialCodes(voterIdentification, votingCardId, verificationCardId, startVotingKey, null, ballotCastingKey));
		assertThrows(NullPointerException.class,
				() -> new VoterInitialCodes(voterIdentification, votingCardId, verificationCardId, startVotingKey, extendedAuthenticationFactor,
						null));
	}

	@Test
	void constructWithNonUuidArgumentsThrows() {
		assertThrows(FailedValidationException.class,
				() -> new VoterInitialCodes(voterIdentification, "nonUUID", verificationCardId, startVotingKey, extendedAuthenticationFactor,
						ballotCastingKey));
		assertThrows(FailedValidationException.class,
				() -> new VoterInitialCodes(voterIdentification, votingCardId, "nonUUID", startVotingKey, extendedAuthenticationFactor,
						ballotCastingKey));
	}

	@Test
	void constructWithEmptyOrBlankVoterIdentificationThrows() {
		assertThrows(IllegalArgumentException.class,
				() -> new VoterInitialCodes("", votingCardId, verificationCardId, startVotingKey, extendedAuthenticationFactor, ballotCastingKey));
		assertThrows(IllegalArgumentException.class,
				() -> new VoterInitialCodes(" \t", votingCardId, verificationCardId, startVotingKey, extendedAuthenticationFactor, ballotCastingKey));
	}

	@Test
	void constructWithIncorrectExtendedAuthenticationFactorLengthThrows() {
		final String expectedErrorMessage = "The extended authentication factor does not have the correct format.";
		final IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class,
				() -> new VoterInitialCodes(voterIdentification, votingCardId, verificationCardId, startVotingKey, "123", ballotCastingKey));
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception1).getMessage());

		final IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
				() -> new VoterInitialCodes(voterIdentification, votingCardId, verificationCardId, startVotingKey, "12345", ballotCastingKey));
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception2).getMessage());

		final IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class,
				() -> new VoterInitialCodes(voterIdentification, votingCardId, verificationCardId, startVotingKey, "1234567", ballotCastingKey));
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception3).getMessage());

		final IllegalArgumentException exception4 = assertThrows(IllegalArgumentException.class,
				() -> new VoterInitialCodes(voterIdentification, votingCardId, verificationCardId, startVotingKey, "123456789", ballotCastingKey));
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception4).getMessage());
	}

	@Test
	void constructWithNonValidStartVotingKeyThrows() {
		final String tooShortStartVotingKey = RANDOM.genRandomString(SVK_LENGTH - 1, usabilityBase32Alphabet);
		assertThrows(FailedValidationException.class,
				() -> new VoterInitialCodes(voterIdentification, votingCardId, verificationCardId, tooShortStartVotingKey,
						extendedAuthenticationFactor, ballotCastingKey));

		final String tooLongStartVotingKey = RANDOM.genRandomString(SVK_LENGTH + 1, usabilityBase32Alphabet);
		assertThrows(FailedValidationException.class,
				() -> new VoterInitialCodes(voterIdentification, votingCardId, verificationCardId, tooLongStartVotingKey,
						extendedAuthenticationFactor, ballotCastingKey));

		final String base64StartVotingKey = RANDOM.genRandomString(SVK_LENGTH, base64Alphabet);
		assertThrows(FailedValidationException.class,
				() -> new VoterInitialCodes(voterIdentification, votingCardId, verificationCardId, base64StartVotingKey, extendedAuthenticationFactor,
						ballotCastingKey));
	}

	@Test
	void constructWithWrongSizeBallotCastingKeyThrow() {
		final String expectedErrorMessage = String.format("The ballot casting key should be a string of l_BCK decimal numbers. [l_BCK: %s]",
				BCK_LENGTH);
		final String tooShortBallotCastingKey = RANDOM.genUniqueDecimalStrings(BCK_LENGTH - 1, 1).get(0);
		final IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class,
				() -> new VoterInitialCodes(voterIdentification, votingCardId, verificationCardId, startVotingKey, extendedAuthenticationFactor,
						tooShortBallotCastingKey));
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception1).getMessage());
		final String tooLongBallotCastingKey = RANDOM.genUniqueDecimalStrings(BCK_LENGTH + 1, 1).get(0);
		final IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
				() -> new VoterInitialCodes(voterIdentification, votingCardId, verificationCardId, startVotingKey, extendedAuthenticationFactor,
						tooLongBallotCastingKey));
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception2).getMessage());
	}

	@Test
	void constructWithValidArgumentsCreatesInstance() {
		final VoterInitialCodes voterInitialCodes = assertDoesNotThrow(
				() -> new VoterInitialCodes(voterIdentification, votingCardId, verificationCardId, startVotingKey, extendedAuthenticationFactor,
						ballotCastingKey));
		assertEquals(voterIdentification, voterInitialCodes.voterIdentification());
		assertEquals(votingCardId, voterInitialCodes.votingCardId());
		assertEquals(verificationCardId, voterInitialCodes.verificationCardId());
		assertEquals(startVotingKey, voterInitialCodes.startVotingKey());
		assertEquals(extendedAuthenticationFactor, voterInitialCodes.extendedAuthenticationFactor());
		assertEquals(ballotCastingKey, voterInitialCodes.ballotCastingKey());
	}
}
