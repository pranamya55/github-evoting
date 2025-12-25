/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("A VerificationCardKeystore")
class VerificationCardKeystoreTest {
	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String VERIFICATION_CARD_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_KEYSTORE = "%s=".formatted(random.genRandomString(571, base64Alphabet));

	@Test
	@DisplayName("created with a null verificationCardId, throws a NullPointerException.")
	void nullVerificationCardId() {
		assertThrows(NullPointerException.class, () -> new VerificationCardKeystore(null, VERIFICATION_CARD_KEYSTORE));
	}

	@Test
	@DisplayName("created with an invalid verificationCardId, throws a FailedValidationException.")
	void invalidVerificationCardId() {
		assertThrows(FailedValidationException.class, () -> new VerificationCardKeystore("verificationCardId", VERIFICATION_CARD_KEYSTORE));
	}

	@Test
	@DisplayName("created with a null verificationCardKeystore, throws a NullPointerException.")
	void nullVerificationCardKeystore() {
		assertThrows(NullPointerException.class, () -> new VerificationCardKeystore(VERIFICATION_CARD_ID, null));
	}

	@Test
	@DisplayName("created with a non-null values does not throw.")
	void withNonNullValuesDoesNotThrow() {
		assertDoesNotThrow(() -> new VerificationCardKeystore(VERIFICATION_CARD_ID, VERIFICATION_CARD_KEYSTORE));
	}
}
