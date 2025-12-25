/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BIRTH_DATE;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BIRTH_YEAR;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.hashing.Argon2;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Factory;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Profile;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ContestType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ExtendedAuthenticationKeysDefinitionType;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigService;

@DisplayName("Calling deriveExtendedAuthenticationChallenge")
class DeriveBaseAuthenticationChallengeAlgorithmTest {

	private static final Random random = RandomFactory.createRandom();
	private static final Hash hash = HashFactory.createHash();
	private static final Argon2 argon2 = Argon2Factory.createArgon2(Argon2Profile.TEST);
	private static final Base64 base64 = BaseEncodingFactory.createBase64();
	private static final int EXTENDED_AUTHENTICATION_FACTOR_LENGTH = 4;

	private static DeriveBaseAuthenticationChallengeAlgorithm algorithm;
	private static EvotingConfigService evotingConfigService;

	private String electionEventId;
	private String startVotingKey;
	private String extendedAuthenticationFactor;

	@BeforeAll
	static void setupAll() {
		evotingConfigService = mock(EvotingConfigService.class);
		algorithm = new DeriveBaseAuthenticationChallengeAlgorithm(hash, argon2, base64);
	}

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		startVotingKey = ElectionSetupUtils.genStartVotingKey();
		extendedAuthenticationFactor = random.genUniqueDecimalStrings(8, 1).get(0);
	}

	@Test
	@DisplayName("with null arguments throws a NullPointerException")
	void deriveBaseAuthenticationChallengeWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class,
				() -> algorithm.deriveBaseAuthenticationChallenge(null, EXTENDED_AUTHENTICATION_FACTOR_LENGTH, startVotingKey,
						extendedAuthenticationFactor
				));
		assertThrows(NullPointerException.class,
				() -> algorithm.deriveBaseAuthenticationChallenge(electionEventId, EXTENDED_AUTHENTICATION_FACTOR_LENGTH, null,
						extendedAuthenticationFactor
				));
		assertThrows(NullPointerException.class,
				() -> algorithm.deriveBaseAuthenticationChallenge(electionEventId, EXTENDED_AUTHENTICATION_FACTOR_LENGTH, startVotingKey, null));
	}

	@Test
	@DisplayName("with invalid election event id throws a FailedValidationException")
	void deriveBaseAuthenticationChallengeWithInvalidElectionEventIdThrows() {
		final String invalidElectionEventId = electionEventId + "$";
		assertThrows(FailedValidationException.class,
				() -> algorithm.deriveBaseAuthenticationChallenge(invalidElectionEventId, EXTENDED_AUTHENTICATION_FACTOR_LENGTH, startVotingKey,
						extendedAuthenticationFactor
				));
	}

	@Test
	@DisplayName("with invalid start voting key throws a FailedValidationException")
	void deriveBaseAuthenticationChallengeWithInvalidStartVotingKeyThrows() {
		final String invalidStartVotingKey = startVotingKey.toUpperCase(Locale.ENGLISH);
		assertThrows(FailedValidationException.class,
				() -> algorithm.deriveBaseAuthenticationChallenge(electionEventId, EXTENDED_AUTHENTICATION_FACTOR_LENGTH, invalidStartVotingKey,
						extendedAuthenticationFactor
				));
	}

	@Test
	@DisplayName("with invalid start voting key throws a FailedValidationException")
	void deriveBaseAuthenticationChallengeWithStartVotingKeyWrongSizeThrows() {
		final String tooLongStartVotingKey = startVotingKey + "a";
		assertThrows(FailedValidationException.class,
				() -> algorithm.deriveBaseAuthenticationChallenge(electionEventId, EXTENDED_AUTHENTICATION_FACTOR_LENGTH, tooLongStartVotingKey,
						extendedAuthenticationFactor
				));

		final String tooShortStartVotingKey = startVotingKey.substring(1);
		assertThrows(FailedValidationException.class,
				() -> algorithm.deriveBaseAuthenticationChallenge(electionEventId, EXTENDED_AUTHENTICATION_FACTOR_LENGTH, tooShortStartVotingKey,
						extendedAuthenticationFactor
				));
	}

	@Test
	@DisplayName("with extended authentication factor not a digit throws a FailedValidationException")
	void deriveBaseAuthenticationChallengeWithInvalidExtendedAuthenticationFactorThrows() {
		when(evotingConfigService.load()).thenReturn(getConfiguration(BIRTH_DATE));

		final String invalidExtendedAuthenticationFactor = "a" + extendedAuthenticationFactor.substring(1);
		final FailedValidationException exception = assertThrows(FailedValidationException.class,
				() -> algorithm.deriveBaseAuthenticationChallenge(electionEventId, EXTENDED_AUTHENTICATION_FACTOR_LENGTH, startVotingKey,
						invalidExtendedAuthenticationFactor
				));
		assertEquals("The extended authentication factor must be a digit of correct size.", exception.getMessage());
	}

	@Test
	@DisplayName("with extended authentication factor of bad size throws a FailedValidationException")
	void deriveBaseAuthenticationChallengeWithExtendedAuthenticationFactorBadSizeThrows() {
		when(evotingConfigService.load()).thenReturn(getConfiguration(BIRTH_DATE));

		final String invalidExtendedAuthenticationFactor = extendedAuthenticationFactor + "1";
		final FailedValidationException exception = assertThrows(FailedValidationException.class,
				() -> algorithm.deriveBaseAuthenticationChallenge(electionEventId, EXTENDED_AUTHENTICATION_FACTOR_LENGTH, startVotingKey,
						invalidExtendedAuthenticationFactor
				));
		assertEquals("The extended authentication factor must be a digit of correct size.", exception.getMessage());
	}

	@Test
	@DisplayName("with valid arguments does not throw")
	void deriveBaseAuthenticationChallengeWithValidArgumentsDoesNotThrow() {

		when(evotingConfigService.load())
				.thenReturn(getConfiguration(BIRTH_DATE))
				.thenReturn(getConfiguration(BIRTH_YEAR));

		final int longExtendedAuthenticationFactorLength = 8;
		assertDoesNotThrow(() -> algorithm.deriveBaseAuthenticationChallenge(electionEventId, longExtendedAuthenticationFactorLength, startVotingKey,
				extendedAuthenticationFactor
		));

		final DeriveBaseAuthenticationChallengeAlgorithm deriveBaseAuthenticationChallengeAlgorithm = new DeriveBaseAuthenticationChallengeAlgorithm(
				hash, argon2, base64);

		final String shortExtendedAuthenticationFactor = extendedAuthenticationFactor.substring(4);
		assertDoesNotThrow(() -> deriveBaseAuthenticationChallengeAlgorithm.deriveBaseAuthenticationChallenge(electionEventId,
				EXTENDED_AUTHENTICATION_FACTOR_LENGTH, startVotingKey,
				shortExtendedAuthenticationFactor));
	}

	private static Configuration getConfiguration(final String keyName) {
		return new Configuration().withContest(
				new ContestType().withExtendedAuthenticationKeys(new ExtendedAuthenticationKeysDefinitionType().withKeyName(keyName)));
	}
}
