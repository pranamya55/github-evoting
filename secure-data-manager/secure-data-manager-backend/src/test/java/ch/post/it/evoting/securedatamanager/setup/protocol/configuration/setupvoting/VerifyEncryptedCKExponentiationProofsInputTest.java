/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;

@DisplayName("VerifyEncryptedCKExponentiationProofsInput with")
class VerifyEncryptedCKExponentiationProofsInputTest extends TestGroupSetup {

	private static int numberOfEligibleVoters;
	private static GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKey;
	private static GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> voterVoteCastReturnCodeGenerationPublicKeys;
	private static GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKey;
	private static GroupVector<ExponentiationProof, ZqGroup> proofsOfCorrectExponentiation;

	@BeforeAll
	static void setUpAll() {
		numberOfEligibleVoters = 5;
		encryptedHashedConfirmationKey = elGamalGenerator.genRandomCiphertextVector(numberOfEligibleVoters, 1);
		voterVoteCastReturnCodeGenerationPublicKeys = Stream.generate(() -> elGamalGenerator.genRandomPublicKey(1))
				.limit(numberOfEligibleVoters)
				.collect(toGroupVector());
		exponentiatedEncryptedHashedConfirmationKey = elGamalGenerator.genRandomCiphertextVector(numberOfEligibleVoters, 1);
		proofsOfCorrectExponentiation = Stream.generate(
						() -> new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
				.limit(numberOfEligibleVoters)
				.collect(toGroupVector());
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void nullParametersThrows(final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKey,
			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> voterVoteCastReturnCodeGenerationPublicKeys,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKey,
			final GroupVector<ExponentiationProof, ZqGroup> proofsOfCorrectExponentiation) {
		assertThrows(NullPointerException.class, () -> new VerifyEncryptedCKExponentiationProofsInput(
				encryptedHashedConfirmationKey,
				voterVoteCastReturnCodeGenerationPublicKeys,
				exponentiatedEncryptedHashedConfirmationKey,
				proofsOfCorrectExponentiation
		));
	}

	@ParameterizedTest
	@MethodSource("provideOtherGroupParameters")
	@DisplayName("other group parameters throws IllegalArgumentException")
	void otherGroupParametersThrows(final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKey,
			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> voterVoteCastReturnCodeGenerationPublicKeys,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKey) {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new VerifyEncryptedCKExponentiationProofsInput(
				encryptedHashedConfirmationKey,
				voterVoteCastReturnCodeGenerationPublicKeys,
				exponentiatedEncryptedHashedConfirmationKey,
				proofsOfCorrectExponentiation
		));

		final String expected = "All input elements must have the same encryption group.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("other group proofs throws IllegalArgumentException")
	void otherGroupProofsThrows() {
		final GroupVector<ExponentiationProof, ZqGroup> differentOrderGroup = Stream.generate(
						() -> new ExponentiationProof(otherZqGroupGenerator.genRandomZqElementMember(), otherZqGroupGenerator.genRandomZqElementMember()))
				.limit(numberOfEligibleVoters)
				.collect(toGroupVector());
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new VerifyEncryptedCKExponentiationProofsInput(
				encryptedHashedConfirmationKey,
				voterVoteCastReturnCodeGenerationPublicKeys,
				exponentiatedEncryptedHashedConfirmationKey,
				differentOrderGroup));

		final String expected = "The group of the proofs of correct exponentiation must have the same order as the input's encryption group.";
		assertEquals(expected, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("provideDifferentSizeParameters")
	@DisplayName("different size parameters throws NullPointerException")
	void differentSizeParametersThrows(final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKey,
			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> voterVoteCastReturnCodeGenerationPublicKeys,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKey,
			final GroupVector<ExponentiationProof, ZqGroup> proofsOfCorrectExponentiation) {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new VerifyEncryptedCKExponentiationProofsInput(
				encryptedHashedConfirmationKey,
				voterVoteCastReturnCodeGenerationPublicKeys,
				exponentiatedEncryptedHashedConfirmationKey,
				proofsOfCorrectExponentiation
		));

		final String expected = "All input elements must have the same size.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("different ciphertexts element size throws IllegalArgumentException")
	void differentCiphertextsElementSizeThrows() {
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> differentElementSizeCiphertexts = elGamalGenerator.genRandomCiphertextVector(
				numberOfEligibleVoters, 2);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new VerifyEncryptedCKExponentiationProofsInput(
				differentElementSizeCiphertexts,
				voterVoteCastReturnCodeGenerationPublicKeys,
				exponentiatedEncryptedHashedConfirmationKey,
				proofsOfCorrectExponentiation));

		final String expected = "The exponentiated, encrypted, hashed Confirmation Key must have the same size as the encrypted, hashed Confirmation Key.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("too big public key size throws IllegalArgumentException")
	void tooBigPublicKeySizeThrows() {
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> tooBigPublicKeySize = Stream.generate(
						() -> elGamalGenerator.genRandomPublicKey(2))
				.limit(numberOfEligibleVoters)
				.collect(toGroupVector());
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new VerifyEncryptedCKExponentiationProofsInput(
				encryptedHashedConfirmationKey,
				tooBigPublicKeySize,
				exponentiatedEncryptedHashedConfirmationKey,
				proofsOfCorrectExponentiation));

		final String expected = "The Voter Vote Cast Return Code Generation public keys must have 1 element.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParametersDoesNotThrow() {
		assertDoesNotThrow(() -> new VerifyEncryptedCKExponentiationProofsInput(encryptedHashedConfirmationKey,
				voterVoteCastReturnCodeGenerationPublicKeys, exponentiatedEncryptedHashedConfirmationKey, proofsOfCorrectExponentiation));
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, voterVoteCastReturnCodeGenerationPublicKeys, exponentiatedEncryptedHashedConfirmationKey,
						proofsOfCorrectExponentiation),
				Arguments.of(encryptedHashedConfirmationKey, null, exponentiatedEncryptedHashedConfirmationKey,
						proofsOfCorrectExponentiation),
				Arguments.of(encryptedHashedConfirmationKey, voterVoteCastReturnCodeGenerationPublicKeys, null, proofsOfCorrectExponentiation),
				Arguments.of(encryptedHashedConfirmationKey, voterVoteCastReturnCodeGenerationPublicKeys,
						exponentiatedEncryptedHashedConfirmationKey, null)
		);
	}

	private static Stream<Arguments> provideOtherGroupParameters() {
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> otherGroupCiphertexts = otherGroupElGamalGenerator.genRandomCiphertextVector(
				numberOfEligibleVoters, 1);
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> otherGroupPublicKeys = Stream.generate(
						() -> otherGroupElGamalGenerator.genRandomPublicKey(1))
				.limit(numberOfEligibleVoters)
				.collect(toGroupVector());
		return Stream.of(
				Arguments.of(otherGroupCiphertexts, voterVoteCastReturnCodeGenerationPublicKeys, exponentiatedEncryptedHashedConfirmationKey),
				Arguments.of(encryptedHashedConfirmationKey, otherGroupPublicKeys, exponentiatedEncryptedHashedConfirmationKey),
				Arguments.of(encryptedHashedConfirmationKey, voterVoteCastReturnCodeGenerationPublicKeys, otherGroupCiphertexts)
		);
	}

	private static Stream<Arguments> provideDifferentSizeParameters() {
		final int differentSizeNumberOfEligibleVoters = numberOfEligibleVoters + 1;
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> differentSizeCiphertexts = elGamalGenerator.genRandomCiphertextVector(
				differentSizeNumberOfEligibleVoters, 1);
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> differentSizePublicKeys = Stream.generate(
						() -> elGamalGenerator.genRandomPublicKey(1))
				.limit(differentSizeNumberOfEligibleVoters)
				.collect(toGroupVector());
		final GroupVector<ExponentiationProof, ZqGroup> differentSizeProofs = Stream.generate(
						() -> new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
				.limit(differentSizeNumberOfEligibleVoters)
				.collect(toGroupVector());
		return Stream.of(
				Arguments.of(differentSizeCiphertexts, voterVoteCastReturnCodeGenerationPublicKeys,
						exponentiatedEncryptedHashedConfirmationKey, proofsOfCorrectExponentiation),
				Arguments.of(encryptedHashedConfirmationKey, differentSizePublicKeys, exponentiatedEncryptedHashedConfirmationKey,
						proofsOfCorrectExponentiation),
				Arguments.of(encryptedHashedConfirmationKey, voterVoteCastReturnCodeGenerationPublicKeys, differentSizeCiphertexts,
						proofsOfCorrectExponentiation),
				Arguments.of(encryptedHashedConfirmationKey, voterVoteCastReturnCodeGenerationPublicKeys,
						exponentiatedEncryptedHashedConfirmationKey, differentSizeProofs)
		);
	}
}