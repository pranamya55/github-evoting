/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("Calling getElectionEventEncryptionParameters with")
class GetElectionEventEncryptionParametersAlgorithmTest {

	private static GetElectionEventEncryptionParametersAlgorithm encryptionParametersAlgorithm;

	@BeforeAll
	static void setupAll() {
		encryptionParametersAlgorithm = new GetElectionEventEncryptionParametersAlgorithm(ElGamalFactory.createElGamal());
	}

	@Test
	@DisplayName("null seed throws an Exception")
	void getElectionEventEncryptionParametersWithInvalidArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> encryptionParametersAlgorithm.getElectionEventEncryptionParameters(null));
	}

	@Test
	@DisplayName("invalid seed throws FailedValidationException")
	void getElectionEventEncryptionParametersWithInvalidSeedThrows() {
		assertThrows(FailedValidationException.class, () -> encryptionParametersAlgorithm.getElectionEventEncryptionParameters("123"));
	}

	@Test
	@DisplayName("valid arguments generates output")
	void getElectionEventEncryptionParametersWithValidArgumentsSucceeds() {
		assertDoesNotThrow(() -> encryptionParametersAlgorithm.getElectionEventEncryptionParameters("NE_20231117_PP01"));
	}

}
