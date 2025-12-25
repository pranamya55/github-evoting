/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Factory;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Profile;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Tests of DeriveCredentialIdAlgorithm.
 */
@DisplayName("DeriveCredentialIdAlgorithm")
class DeriveCredentialIdAlgorithmTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private final String electionEventId = uuidGenerator.generate();
	private final String startVotingKey = ElectionSetupUtils.genStartVotingKey();

	private final DeriveCredentialIdAlgorithm deriveCredentialIdAlgorithm = new DeriveCredentialIdAlgorithm(HashFactory.createHash(),
			BaseEncodingFactory.createBase16(), Argon2Factory.createArgon2(Argon2Profile.TEST));

	@Test
	@DisplayName("calling deriveCredentialId with null parameters throws a NullPointerException.")
	void deriveCredentialIdWithNullParametersThrows() {
		assertThrows(NullPointerException.class, () -> deriveCredentialIdAlgorithm.deriveCredentialId(null, startVotingKey));
		assertThrows(NullPointerException.class, () -> deriveCredentialIdAlgorithm.deriveCredentialId(electionEventId, null));
	}

	@Test
	@DisplayName("calling deriveCredentialId with invalid parameters throws a FailedValidationException.")
	void deriveCredentialIdWithInvalidParametersThrows() {
		assertThrows(FailedValidationException.class, () -> deriveCredentialIdAlgorithm.deriveCredentialId("not UUID", startVotingKey));
		assertThrows(FailedValidationException.class,
				() -> deriveCredentialIdAlgorithm.deriveCredentialId(electionEventId, "not base32 of length 24."));

		final String base32WithPadAlphabet = "4d65ej2adb4ia6ghhzb52k==";
		assertThrows(FailedValidationException.class, () -> deriveCredentialIdAlgorithm.deriveCredentialId(electionEventId, base32WithPadAlphabet));
	}

	@Test
	@DisplayName("calling deriveCredentialId with invalid SVK_id length throws a FailedValidationException.")
	void deriveCredentialIdWithInvalidSVKLengthThrows() {
		final String tooSmallSVK = startVotingKey.substring(1);
		assertThrows(FailedValidationException.class, () -> deriveCredentialIdAlgorithm.deriveCredentialId(electionEventId, tooSmallSVK));
	}

	@Test
	@DisplayName("calling deriveCredentialId with correct parameters does not throw.")
	void deriveCredentialId() {
		assertDoesNotThrow(() -> deriveCredentialIdAlgorithm.deriveCredentialId(electionEventId, startVotingKey));
	}

	@Test
	@DisplayName("calling deriveCredentialId gives expected output.")
	void deriveCredentialIdExpectedOutput() {
		final String expectedCredentialId = "9EF993547C912D099A13F5F9B55B81F5";
		final String givenElectionEventId = "0AD226BDFBE84A32BC8808234D83E7B4";
		final String givenStartVotingKey = "fsfkkn4x7js5xbzeyfyd3qpu";

		assertEquals(expectedCredentialId, deriveCredentialIdAlgorithm.deriveCredentialId(givenElectionEventId, givenStartVotingKey));
	}
}
