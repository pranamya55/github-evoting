/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
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

@DisplayName("VerifyPCCEncryptedExponentiationProofsInput with")
class VerifyEncryptedPCCExponentiationProofsInputTest extends TestGroupSetup {

	private static int numberOfEligibleVoters;
	private static int numberOfVotingOptions;
	private static GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes;
	private static GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> voterChoiceReturnCodeGenerationPublicKeys;
	private static GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodes;
	private static GroupVector<ExponentiationProof, ZqGroup> proofsOfCorrectExponentiation;

	@BeforeAll
	static void setUpAll() {
		numberOfEligibleVoters = 5;
		numberOfVotingOptions = 3;
		encryptedHashedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertextVector(numberOfEligibleVoters, numberOfVotingOptions);
		voterChoiceReturnCodeGenerationPublicKeys = Stream.generate(() -> elGamalGenerator.genRandomPublicKey(1))
				.limit(numberOfEligibleVoters)
				.collect(toGroupVector());
		exponentiatedEncryptedHashedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertextVector(numberOfEligibleVoters,
				numberOfVotingOptions);
		proofsOfCorrectExponentiation = Stream.generate(
						() -> new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
				.limit(numberOfEligibleVoters)
				.collect(toGroupVector());
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void nullParametersThrows(final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes,
			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> voterChoiceReturnCodeGenerationPublicKeys,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodes,
			final GroupVector<ExponentiationProof, ZqGroup> proofsOfCorrectExponentiation) {
		assertThrows(NullPointerException.class, () -> new VerifyEncryptedPCCExponentiationProofsInput(
				encryptedHashedPartialChoiceReturnCodes,
				voterChoiceReturnCodeGenerationPublicKeys,
				exponentiatedEncryptedHashedPartialChoiceReturnCodes,
				proofsOfCorrectExponentiation
		));
	}

	@ParameterizedTest
	@MethodSource("provideOtherGroupParameters")
	@DisplayName("other group parameters throws IllegalArgumentException")
	void otherGroupParametersThrows(final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes,
			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> voterChoiceReturnCodeGenerationPublicKeys,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodes) {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new VerifyEncryptedPCCExponentiationProofsInput(
				encryptedHashedPartialChoiceReturnCodes,
				voterChoiceReturnCodeGenerationPublicKeys,
				exponentiatedEncryptedHashedPartialChoiceReturnCodes,
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
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new VerifyEncryptedPCCExponentiationProofsInput(
				encryptedHashedPartialChoiceReturnCodes,
				voterChoiceReturnCodeGenerationPublicKeys,
				exponentiatedEncryptedHashedPartialChoiceReturnCodes,
				differentOrderGroup));

		final String expected = "The group of the proofs of correct exponentiation must have the same order as the input's encryption group.";
		assertEquals(expected, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("provideDifferentSizeParameters")
	@DisplayName("different size parameters throws NullPointerException")
	void differentSizeParametersThrows(final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes,
			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> voterChoiceReturnCodeGenerationPublicKeys,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodes,
			final GroupVector<ExponentiationProof, ZqGroup> proofsOfCorrectExponentiation) {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new VerifyEncryptedPCCExponentiationProofsInput(
				encryptedHashedPartialChoiceReturnCodes,
				voterChoiceReturnCodeGenerationPublicKeys,
				exponentiatedEncryptedHashedPartialChoiceReturnCodes,
				proofsOfCorrectExponentiation
		));

		final String expected = "All input elements must have the same size.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("different ciphertexts element size throws IllegalArgumentException")
	void differentCiphertextsElementSizeThrows() {
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> differentElementSizeCiphertexts = elGamalGenerator.genRandomCiphertextVector(
				numberOfEligibleVoters, numberOfVotingOptions + 1);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new VerifyEncryptedPCCExponentiationProofsInput(
				differentElementSizeCiphertexts,
				voterChoiceReturnCodeGenerationPublicKeys,
				exponentiatedEncryptedHashedPartialChoiceReturnCodes,
				proofsOfCorrectExponentiation));

		final String expected = "The encrypted, hashed partial Choice Return Codes must have the same size as the exponentiated, encrypted hashed partial Choice Return Codes.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("too big number of voting options throws IllegalArgumentException")
	void tooBigNumberOfVotingOptionsThrows() {
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> zeroNumberOfVotingOptionsCiphertexts = elGamalGenerator.genRandomCiphertextVector(
				numberOfEligibleVoters, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS + 1);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new VerifyEncryptedPCCExponentiationProofsInput(
				zeroNumberOfVotingOptionsCiphertexts,
				voterChoiceReturnCodeGenerationPublicKeys,
				zeroNumberOfVotingOptionsCiphertexts,
				proofsOfCorrectExponentiation));

		final String expected = String.format(
				"The number of voting options must be smaller or equal to the maximum supported number of voting options. [n: %s, n_sup: %s]",
				MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS + 1, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("too big public key size throws IllegalArgumentException")
	void tooBigPublicKeySizeThrows() {
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> tooBigPublicKeySize = Stream.generate(
						() -> elGamalGenerator.genRandomPublicKey(2))
				.limit(numberOfEligibleVoters)
				.collect(toGroupVector());
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new VerifyEncryptedPCCExponentiationProofsInput(
				encryptedHashedPartialChoiceReturnCodes,
				tooBigPublicKeySize,
				exponentiatedEncryptedHashedPartialChoiceReturnCodes,
				proofsOfCorrectExponentiation));

		final String expected = "The Voter Choice Return Code Generation public keys must have 1 element.";
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParametersDoesNotThrow() {
		assertDoesNotThrow(() -> new VerifyEncryptedPCCExponentiationProofsInput(encryptedHashedPartialChoiceReturnCodes,
				voterChoiceReturnCodeGenerationPublicKeys, exponentiatedEncryptedHashedPartialChoiceReturnCodes, proofsOfCorrectExponentiation));
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, voterChoiceReturnCodeGenerationPublicKeys, exponentiatedEncryptedHashedPartialChoiceReturnCodes,
						proofsOfCorrectExponentiation),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, null, exponentiatedEncryptedHashedPartialChoiceReturnCodes,
						proofsOfCorrectExponentiation),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, voterChoiceReturnCodeGenerationPublicKeys, null, proofsOfCorrectExponentiation),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, voterChoiceReturnCodeGenerationPublicKeys,
						exponentiatedEncryptedHashedPartialChoiceReturnCodes, null)
		);
	}

	private static Stream<Arguments> provideOtherGroupParameters() {
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> otherGroupCiphertexts = otherGroupElGamalGenerator.genRandomCiphertextVector(
				numberOfEligibleVoters, numberOfVotingOptions);
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> otherGroupPublicKeys = Stream.generate(
						() -> otherGroupElGamalGenerator.genRandomPublicKey(1))
				.limit(numberOfEligibleVoters)
				.collect(toGroupVector());
		return Stream.of(
				Arguments.of(otherGroupCiphertexts, voterChoiceReturnCodeGenerationPublicKeys, exponentiatedEncryptedHashedPartialChoiceReturnCodes),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, otherGroupPublicKeys, exponentiatedEncryptedHashedPartialChoiceReturnCodes),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, voterChoiceReturnCodeGenerationPublicKeys, otherGroupCiphertexts)
		);
	}

	private static Stream<Arguments> provideDifferentSizeParameters() {
		final int differentSizeNumberOfEligibleVoters = numberOfEligibleVoters + 1;
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> differentSizeCiphertexts = elGamalGenerator.genRandomCiphertextVector(
				differentSizeNumberOfEligibleVoters, numberOfVotingOptions);
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> differentSizePublicKeys = Stream.generate(
						() -> elGamalGenerator.genRandomPublicKey(1))
				.limit(differentSizeNumberOfEligibleVoters)
				.collect(toGroupVector());
		final GroupVector<ExponentiationProof, ZqGroup> differentSizeProofs = Stream.generate(
						() -> new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
				.limit(differentSizeNumberOfEligibleVoters)
				.collect(toGroupVector());
		return Stream.of(
				Arguments.of(differentSizeCiphertexts, voterChoiceReturnCodeGenerationPublicKeys,
						exponentiatedEncryptedHashedPartialChoiceReturnCodes, proofsOfCorrectExponentiation),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, differentSizePublicKeys, exponentiatedEncryptedHashedPartialChoiceReturnCodes,
						proofsOfCorrectExponentiation),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, voterChoiceReturnCodeGenerationPublicKeys, differentSizeCiphertexts,
						proofsOfCorrectExponentiation),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, voterChoiceReturnCodeGenerationPublicKeys,
						exponentiatedEncryptedHashedPartialChoiceReturnCodes, differentSizeProofs)
		);
	}
}