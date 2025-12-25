/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;

@DisplayName("GenCMTableInput constructed with")
class GenCMTableInputTest extends TestGroupSetup {

	private static final SecureRandom secureRandom = new SecureRandom();

	private static ElGamalMultiRecipientPrivateKey setupSecretKey;
	private static GroupVector<GqElement, GqGroup> preVoteCastReturnCodes;
	private static GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedPreChoiceReturnCodes;

	@BeforeEach
	void setUp() {
		initializeParameters();
	}

	@Test
	@DisplayName("any null parameter throws NullPointerException")
	void anyNullParamThrows() {
		assertThrows(NullPointerException.class, () -> new GenCMTableInput(null, encryptedPreChoiceReturnCodes, preVoteCastReturnCodes));
		assertThrows(NullPointerException.class, () -> new GenCMTableInput(setupSecretKey, null, preVoteCastReturnCodes));
		assertThrows(NullPointerException.class, () -> new GenCMTableInput(setupSecretKey, encryptedPreChoiceReturnCodes, null));
	}

	@ParameterizedTest
	@MethodSource("provideEmptyElementSize")
	@DisplayName("empty elements size throws IllegalArgumentException")
	void emptyElementSizeThrows(final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedPreChoiceReturnCodes,
			final GroupVector<GqElement, GqGroup> preVoteCastReturnCodes) {

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenCMTableInput(setupSecretKey, encryptedPreChoiceReturnCodes, preVoteCastReturnCodes));
		assertEquals("All inputs must not be empty.", Throwables.getRootCause(exception).getMessage());
	}

	@ParameterizedTest
	@MethodSource("provideIncorrectElementSize")
	@DisplayName("incorrect elements size throws IllegalArgumentException")
	void incorrectElementSizeThrows(final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedPreChoiceReturnCodes,
			final GroupVector<GqElement, GqGroup> preVoteCastReturnCodes) {

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenCMTableInput(setupSecretKey, encryptedPreChoiceReturnCodes, preVoteCastReturnCodes));
		assertEquals("All inputs sizes must be the same.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("incorrect group throws IllegalArgumentException")
	void incorrectGroupThrows() {
		final GroupVector<GqElement, GqGroup> otherPreVoteCastReturnCodes = otherGqGroupGenerator.genRandomGqElementVector(
				preVoteCastReturnCodes.size());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenCMTableInput(setupSecretKey, encryptedPreChoiceReturnCodes, otherPreVoteCastReturnCodes));
		assertEquals("All inputs must have the same Gq group.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("invalid setup secret key size throws IllegalArgumentException")
	void invalidSetupSecretKeySizeThrows() {
		final ElGamalMultiRecipientPrivateKey invalidSetupSecretKey = new ElGamalMultiRecipientPrivateKey(
				IntStream.range(0, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS + 1)
						.mapToObj(i -> zqGroupGenerator.genRandomZqElementMember())
						.collect(GroupVector.toGroupVector()));

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenCMTableInput(invalidSetupSecretKey, encryptedPreChoiceReturnCodes, preVoteCastReturnCodes));
		final String expected = String.format("The setup secret key must have at most n_sup elements. [n_sup: %s]",
				MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParamsDoesNotThrow() {
		final GenCMTableInput genCMTableInput = assertDoesNotThrow(
				() -> new GenCMTableInput(setupSecretKey, encryptedPreChoiceReturnCodes, preVoteCastReturnCodes));

		assertEquals(setupSecretKey, genCMTableInput.setupSecretKey());
		assertEquals(encryptedPreChoiceReturnCodes, genCMTableInput.encryptedPreChoiceReturnCodes());
		assertEquals(preVoteCastReturnCodes, genCMTableInput.preVoteCastReturnCodes());
		assertEquals(encryptedPreChoiceReturnCodes.getGroup(), genCMTableInput.getGroup());
	}

	private static void initializeParameters() {
		setupSecretKey = new ElGamalMultiRecipientPrivateKey(IntStream.range(0, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS)
				.mapToObj(i -> zqGroupGenerator.genRandomZqElementMember())
				.collect(GroupVector.toGroupVector()));

		final int N_e = secureRandom.nextInt(1, 10);
		final int n = secureRandom.nextInt(1, 5);
		encryptedPreChoiceReturnCodes = elGamalGenerator.genRandomCiphertextVector(N_e, n);

		preVoteCastReturnCodes = gqGroupGenerator.genRandomGqElementVector(N_e);
	}

	private static Stream<Arguments> provideEmptyElementSize() {
		initializeParameters();

		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> emptyVector = elGamalGenerator.genRandomCiphertextVector(0, 0);

		return Stream.of(
				Arguments.of(emptyVector, preVoteCastReturnCodes),
				Arguments.of(encryptedPreChoiceReturnCodes, emptyVector)
		);
	}

	private static Stream<Arguments> provideIncorrectElementSize() {
		initializeParameters();

		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> invalidEncryptedPreChoiceReturnCodes = elGamalGenerator.genRandomCiphertextVector(
				encryptedPreChoiceReturnCodes.size() + 1, encryptedPreChoiceReturnCodes.getElementSize());
		final GroupVector<GqElement, GqGroup> invalidPreVoteCastReturnCodes = gqGroupGenerator.genRandomGqElementVector(
				preVoteCastReturnCodes.size() + 1);

		return Stream.of(
				Arguments.of(invalidEncryptedPreChoiceReturnCodes, preVoteCastReturnCodes),
				Arguments.of(encryptedPreChoiceReturnCodes, invalidPreVoteCastReturnCodes)
		);
	}

}
