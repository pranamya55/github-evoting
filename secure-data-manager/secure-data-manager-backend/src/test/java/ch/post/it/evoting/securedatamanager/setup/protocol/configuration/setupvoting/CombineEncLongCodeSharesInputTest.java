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

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupMatrix;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.domain.generators.ControlComponentCodeSharesPayloadGenerator;
import ch.post.it.evoting.domain.generators.SetupComponentVerificationDataPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationData;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;

@DisplayName("CombineEncLongCodeSharesInput with")
class CombineEncLongCodeSharesInputTest extends TestGroupSetup {

	private static int numberOfEligibleVoters;
	private static ElGamalMultiRecipientPrivateKey setupSecretKey;
	private static GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes;
	private static GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys;
	private static GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterChoiceReturnCodeGenerationPublicKeysVectors;
	private static GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterVoteCastReturnCodeGenerationPublicKeysVectors;
	private static GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix;
	private static GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectPartialChoiceReturnCodesExponentiation;
	private static GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKeysMatrix;
	private static GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectConfirmationKeysExponentiation;

	@BeforeAll
	static void setUpAll() {
		final SetupComponentVerificationDataPayloadGenerator setupComponentVerificationDataPayloadGenerator = new SetupComponentVerificationDataPayloadGenerator(
				gqGroup);
		final SetupComponentVerificationDataPayload setupComponentVerificationDataPayload = setupComponentVerificationDataPayloadGenerator.generate();
		final ImmutableList<SetupComponentVerificationData> setupComponentVerificationData = setupComponentVerificationDataPayload.getSetupComponentVerificationData();
		numberOfEligibleVoters = setupComponentVerificationData.size();
		final int numberOfVotingOptions = setupComponentVerificationData.getFirst().encryptedHashedSquaredPartialChoiceReturnCodes().size();

		final ControlComponentCodeSharesPayloadGenerator controlComponentCodeSharesPayloadGenerator = new ControlComponentCodeSharesPayloadGenerator(
				gqGroup);
		final ImmutableList<ControlComponentCodeSharesPayload> controlComponentCodeSharesPayloads = controlComponentCodeSharesPayloadGenerator.generate(
				setupComponentVerificationDataPayload.getElectionEventId(), setupComponentVerificationDataPayload.getVerificationCardSetId(),
				setupComponentVerificationDataPayload.getChunkId(), numberOfEligibleVoters, numberOfVotingOptions);

		setupSecretKey = elGamalGenerator.genRandomPrivateKey(10);

		encryptedHashedPartialChoiceReturnCodes = setupComponentVerificationData.stream()
				.map(SetupComponentVerificationData::encryptedHashedSquaredPartialChoiceReturnCodes).collect(toGroupVector());
		encryptedHashedConfirmationKeys = setupComponentVerificationData.stream()
				.map(SetupComponentVerificationData::encryptedHashedSquaredConfirmationKey).collect(toGroupVector());
		voterChoiceReturnCodeGenerationPublicKeysVectors = controlComponentCodeSharesPayloads.stream()
				.map(ControlComponentCodeSharesPayload::getControlComponentCodeShares)
				.map(share -> share.stream().map(ControlComponentCodeShare::voterChoiceReturnCodeGenerationPublicKey).collect(toGroupVector()))
				.collect(toGroupVector());
		voterVoteCastReturnCodeGenerationPublicKeysVectors = controlComponentCodeSharesPayloads.stream()
				.map(ControlComponentCodeSharesPayload::getControlComponentCodeShares)
				.map(share -> share.stream().map(ControlComponentCodeShare::voterVoteCastReturnCodeGenerationPublicKey).collect(toGroupVector()))
				.collect(toGroupVector());
		exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix = GroupMatrix.fromColumns(
				controlComponentCodeSharesPayloads.stream().map(ControlComponentCodeSharesPayload::getControlComponentCodeShares)
						.map(share -> share.stream().map(ControlComponentCodeShare::exponentiatedEncryptedPartialChoiceReturnCodes)
								.collect(toGroupVector())).collect(toGroupVector()));
		proofsOfCorrectPartialChoiceReturnCodesExponentiation = controlComponentCodeSharesPayloads.stream()
				.map(ControlComponentCodeSharesPayload::getControlComponentCodeShares)
				.map(share -> share.stream().map(ControlComponentCodeShare::encryptedPartialChoiceReturnCodeExponentiationProof)
						.collect(toGroupVector())).collect(toGroupVector());
		exponentiatedEncryptedHashedConfirmationKeysMatrix = GroupMatrix.fromColumns(
				controlComponentCodeSharesPayloads.stream().map(ControlComponentCodeSharesPayload::getControlComponentCodeShares)
						.map(share -> share.stream().map(ControlComponentCodeShare::exponentiatedEncryptedConfirmationKey).collect(toGroupVector()))
						.collect(toGroupVector()));
		proofsOfCorrectConfirmationKeysExponentiation = controlComponentCodeSharesPayloads.stream()
				.map(ControlComponentCodeSharesPayload::getControlComponentCodeShares)
				.map(share -> share.stream().map(ControlComponentCodeShare::encryptedConfirmationKeyExponentiationProof).collect(toGroupVector()))
				.collect(toGroupVector());
	}

	@ParameterizedTest
	@MethodSource("provideNullArguments")
	@DisplayName("null arguments throws NullPointerException")
	void nullArgumentsThrows(final ElGamalMultiRecipientPrivateKey setupSecretKey,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys,
			final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterChoiceReturnCodeGenerationPublicKeysVectors,
			final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterVoteCastReturnCodeGenerationPublicKeysVectors,
			final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
			final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectPartialChoiceReturnCodesExponentiation,
			final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKeysMatrix,
			final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectConfirmationKeysExponentiation) {

		final CombineEncLongCodeSharesInput.Builder builder = new CombineEncLongCodeSharesInput.Builder().setSetupSecretKey(setupSecretKey)
				.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
				.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeys)
				.setVoterChoiceReturnCodeGenerationPublicKeysVectors(voterChoiceReturnCodeGenerationPublicKeysVectors)
				.setVoterVoteCastReturnCodeGenerationPublicKeysVectors(voterVoteCastReturnCodeGenerationPublicKeysVectors)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix(exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix)
				.setProofsOfCorrectPartialChoiceReturnCodesExponentiation(proofsOfCorrectPartialChoiceReturnCodesExponentiation)
				.setExponentiatedEncryptedHashedConfirmationKeysMatrix(exponentiatedEncryptedHashedConfirmationKeysMatrix)
				.setProofsOfCorrectConfirmationKeysExponentiation(proofsOfCorrectConfirmationKeysExponentiation);

		assertThrows(NullPointerException.class, builder::build);
	}

	private static Stream<Arguments> provideNullArguments() {
		return Stream.of(Arguments.of(null, encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys,
						voterChoiceReturnCodeGenerationPublicKeysVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors,
						exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix, proofsOfCorrectPartialChoiceReturnCodesExponentiation,
						exponentiatedEncryptedHashedConfirmationKeysMatrix, proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(setupSecretKey, null, encryptedHashedConfirmationKeys, voterChoiceReturnCodeGenerationPublicKeysVectors,
						voterVoteCastReturnCodeGenerationPublicKeysVectors, exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
						proofsOfCorrectPartialChoiceReturnCodesExponentiation, exponentiatedEncryptedHashedConfirmationKeysMatrix,
						proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(setupSecretKey, encryptedHashedPartialChoiceReturnCodes, null, voterChoiceReturnCodeGenerationPublicKeysVectors,
						voterVoteCastReturnCodeGenerationPublicKeysVectors, exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
						proofsOfCorrectPartialChoiceReturnCodesExponentiation, exponentiatedEncryptedHashedConfirmationKeysMatrix,
						proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(setupSecretKey, encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys, null,
						voterVoteCastReturnCodeGenerationPublicKeysVectors, exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
						proofsOfCorrectPartialChoiceReturnCodesExponentiation, exponentiatedEncryptedHashedConfirmationKeysMatrix,
						proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(setupSecretKey, encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys,
						voterChoiceReturnCodeGenerationPublicKeysVectors, null, exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
						proofsOfCorrectPartialChoiceReturnCodesExponentiation, exponentiatedEncryptedHashedConfirmationKeysMatrix,
						proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(setupSecretKey, encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys,
						voterChoiceReturnCodeGenerationPublicKeysVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors, null,
						proofsOfCorrectPartialChoiceReturnCodesExponentiation, exponentiatedEncryptedHashedConfirmationKeysMatrix,
						proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(setupSecretKey, encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys,
						voterChoiceReturnCodeGenerationPublicKeysVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors,
						exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix, null, exponentiatedEncryptedHashedConfirmationKeysMatrix,
						proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(setupSecretKey, encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys,
						voterChoiceReturnCodeGenerationPublicKeysVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors,
						exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix, proofsOfCorrectPartialChoiceReturnCodesExponentiation, null,
						proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(setupSecretKey, encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys,
						voterChoiceReturnCodeGenerationPublicKeysVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors,
						exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix, proofsOfCorrectPartialChoiceReturnCodesExponentiation,
						exponentiatedEncryptedHashedConfirmationKeysMatrix, null)

		);
	}

	@ParameterizedTest
	@MethodSource("provideOtherGroupArguments")
	@DisplayName("different encryption group throws IllegalArgumentException")
	void differentEncryptionGroupThrows(final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys,
			final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterChoiceReturnCodeGenerationPublicKeysVectors,
			final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterVoteCastReturnCodeGenerationPublicKeysVectors,
			final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
			final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKeysMatrix) {

		final CombineEncLongCodeSharesInput.Builder builder = new CombineEncLongCodeSharesInput.Builder().setSetupSecretKey(setupSecretKey)
				.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
				.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeys)
				.setVoterChoiceReturnCodeGenerationPublicKeysVectors(voterChoiceReturnCodeGenerationPublicKeysVectors)
				.setVoterVoteCastReturnCodeGenerationPublicKeysVectors(voterVoteCastReturnCodeGenerationPublicKeysVectors)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix(exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix)
				.setProofsOfCorrectPartialChoiceReturnCodesExponentiation(proofsOfCorrectPartialChoiceReturnCodesExponentiation)
				.setExponentiatedEncryptedHashedConfirmationKeysMatrix(exponentiatedEncryptedHashedConfirmationKeysMatrix)
				.setProofsOfCorrectConfirmationKeysExponentiation(proofsOfCorrectConfirmationKeysExponentiation);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = "All input elements must have the same encryption group.";
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	private static Stream<Arguments> provideOtherGroupArguments() {
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> otherGroupCiphertexts = otherGroupElGamalGenerator.genRandomCiphertextVector(3,
				3);
		final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> otherGroupPublicKeyVectors = GroupVector.of(
				GroupVector.of(otherGroupElGamalGenerator.genRandomPublicKey(3)));
		final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> otherGroupMatrix = otherGroupElGamalGenerator.genRandomCiphertextMatrix(2, 2, 3);

		return Stream.of(Arguments.of(otherGroupCiphertexts, encryptedHashedConfirmationKeys, voterChoiceReturnCodeGenerationPublicKeysVectors,
						voterVoteCastReturnCodeGenerationPublicKeysVectors, exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
						exponentiatedEncryptedHashedConfirmationKeysMatrix),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, otherGroupCiphertexts, voterChoiceReturnCodeGenerationPublicKeysVectors,
						voterVoteCastReturnCodeGenerationPublicKeysVectors, exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
						exponentiatedEncryptedHashedConfirmationKeysMatrix),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys, otherGroupPublicKeyVectors,
						voterVoteCastReturnCodeGenerationPublicKeysVectors, exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
						exponentiatedEncryptedHashedConfirmationKeysMatrix),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys,
						voterChoiceReturnCodeGenerationPublicKeysVectors, otherGroupPublicKeyVectors,
						exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix, exponentiatedEncryptedHashedConfirmationKeysMatrix),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys,
						voterChoiceReturnCodeGenerationPublicKeysVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors, otherGroupMatrix,
						exponentiatedEncryptedHashedConfirmationKeysMatrix),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys,
						voterChoiceReturnCodeGenerationPublicKeysVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors,
						exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix, otherGroupMatrix));
	}

	@ParameterizedTest
	@MethodSource("provideOtherZqGroupArguments")
	@DisplayName("different ZqGroup throws IllegalArgumentException")
	void differentZqGroupThrows(final ElGamalMultiRecipientPrivateKey setupSecretKey,
			final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectPartialChoiceReturnCodesExponentiation,
			final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectConfirmationKeysExponentiation) {

		final CombineEncLongCodeSharesInput.Builder builder = new CombineEncLongCodeSharesInput.Builder().setSetupSecretKey(setupSecretKey)
				.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
				.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeys)
				.setVoterChoiceReturnCodeGenerationPublicKeysVectors(voterChoiceReturnCodeGenerationPublicKeysVectors)
				.setVoterVoteCastReturnCodeGenerationPublicKeysVectors(voterVoteCastReturnCodeGenerationPublicKeysVectors)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix(exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix)
				.setProofsOfCorrectPartialChoiceReturnCodesExponentiation(proofsOfCorrectPartialChoiceReturnCodesExponentiation)
				.setExponentiatedEncryptedHashedConfirmationKeysMatrix(exponentiatedEncryptedHashedConfirmationKeysMatrix)
				.setProofsOfCorrectConfirmationKeysExponentiation(proofsOfCorrectConfirmationKeysExponentiation);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = "All input elements must have the same ZqGroup.";
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	private static Stream<Arguments> provideOtherZqGroupArguments() {
		final ElGamalMultiRecipientPrivateKey otherZqGroupPrivateKey = new ElGamalMultiRecipientPrivateKey(
				otherZqGroupGenerator.genRandomZqElementVector(2));
		final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> otherZqGroupProofs = GroupVector.of(GroupVector.of(
				new ExponentiationProof(otherZqGroupGenerator.genRandomZqElementMember(), otherZqGroupGenerator.genRandomZqElementMember())));

		return Stream.of(Arguments.of(otherZqGroupPrivateKey, proofsOfCorrectPartialChoiceReturnCodesExponentiation,
						proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(setupSecretKey, otherZqGroupProofs, proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(setupSecretKey, proofsOfCorrectPartialChoiceReturnCodesExponentiation, otherZqGroupProofs));
	}

	@Test
	@DisplayName("ZqGroup not same order as encryption group throws IllegalArgumentException")
	void zqGroupNotSameOrderAsEncryptionGroupThrows() {
		final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> otherZqGroupProofs = GroupVector.of(GroupVector.of(
				new ExponentiationProof(otherZqGroupGenerator.genRandomZqElementMember(), otherZqGroupGenerator.genRandomZqElementMember())));
		final ElGamalMultiRecipientPrivateKey otherZqGroupPrivateKey = new ElGamalMultiRecipientPrivateKey(
				GroupVector.of(otherZqGroupGenerator.genRandomZqElementMember()));

		final CombineEncLongCodeSharesInput.Builder builder = new CombineEncLongCodeSharesInput.Builder().setSetupSecretKey(otherZqGroupPrivateKey)
				.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
				.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeys)
				.setVoterChoiceReturnCodeGenerationPublicKeysVectors(voterChoiceReturnCodeGenerationPublicKeysVectors)
				.setVoterVoteCastReturnCodeGenerationPublicKeysVectors(voterVoteCastReturnCodeGenerationPublicKeysVectors)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix(exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix)
				.setProofsOfCorrectPartialChoiceReturnCodesExponentiation(otherZqGroupProofs)
				.setExponentiatedEncryptedHashedConfirmationKeysMatrix(exponentiatedEncryptedHashedConfirmationKeysMatrix)
				.setProofsOfCorrectConfirmationKeysExponentiation(otherZqGroupProofs);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = "The input's ZqGroup must have the same order as the input's encryption group.";
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("too big setup secret keys throws IllegalArgumentException")
	void tooBigSetupSecretKeyThrows() {
		final ElGamalMultiRecipientPrivateKey tooBigSetupSecretKey = new ElGamalMultiRecipientPrivateKey(
				Stream.generate(() -> zqGroupGenerator.genRandomZqElementMember()).limit(MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS + 1)
						.collect(toGroupVector()));

		final CombineEncLongCodeSharesInput.Builder builder = new CombineEncLongCodeSharesInput.Builder().setSetupSecretKey(tooBigSetupSecretKey)
				.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
				.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeys)
				.setVoterChoiceReturnCodeGenerationPublicKeysVectors(voterChoiceReturnCodeGenerationPublicKeysVectors)
				.setVoterVoteCastReturnCodeGenerationPublicKeysVectors(voterVoteCastReturnCodeGenerationPublicKeysVectors)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix(exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix)
				.setProofsOfCorrectPartialChoiceReturnCodesExponentiation(proofsOfCorrectPartialChoiceReturnCodesExponentiation)
				.setExponentiatedEncryptedHashedConfirmationKeysMatrix(exponentiatedEncryptedHashedConfirmationKeysMatrix)
				.setProofsOfCorrectConfirmationKeysExponentiation(proofsOfCorrectConfirmationKeysExponentiation);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = "The size of the setup secret key must be smaller or equal to the maximum supported number of voting options.";
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@ParameterizedTest
	@MethodSource("provideOtherSizeArguments")
	@DisplayName("different size arguments throws IllegalArgumentException")
	void differentSizeArgumentsThrows(final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys,
			final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterChoiceReturnCodeGenerationPublicKeysVectors,
			final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterVoteCastReturnCodeGenerationPublicKeysVectors,
			final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
			final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectPartialChoiceReturnCodesExponentiation,
			final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKeysMatrix,
			final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectConfirmationKeysExponentiation) {

		final CombineEncLongCodeSharesInput.Builder builder = new CombineEncLongCodeSharesInput.Builder().setSetupSecretKey(setupSecretKey)
				.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
				.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeys)
				.setVoterChoiceReturnCodeGenerationPublicKeysVectors(voterChoiceReturnCodeGenerationPublicKeysVectors)
				.setVoterVoteCastReturnCodeGenerationPublicKeysVectors(voterVoteCastReturnCodeGenerationPublicKeysVectors)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix(exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix)
				.setProofsOfCorrectPartialChoiceReturnCodesExponentiation(proofsOfCorrectPartialChoiceReturnCodesExponentiation)
				.setExponentiatedEncryptedHashedConfirmationKeysMatrix(exponentiatedEncryptedHashedConfirmationKeysMatrix)
				.setProofsOfCorrectConfirmationKeysExponentiation(proofsOfCorrectConfirmationKeysExponentiation);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = "All input elements must have the same size.";
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	private static Stream<Arguments> provideOtherSizeArguments() {
		final int otherSize = encryptedHashedPartialChoiceReturnCodes.size() + 1;
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> otherSizeCiphertexts = elGamalGenerator.genRandomCiphertextVector(otherSize, 3);
		final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> otherSizePublicKeyVectors = GroupVector.of(
				GroupVector.of(elGamalGenerator.genRandomPublicKey(otherSize)));
		final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> otherSizeMatrix = elGamalGenerator.genRandomCiphertextMatrix(otherSize, 2, 3);
		final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> otherSizeProofs = GroupVector.of(Stream.generate(
						() -> new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
				.limit(otherSize).collect(toGroupVector()));

		return Stream.of(Arguments.of(otherSizeCiphertexts, encryptedHashedConfirmationKeys, voterChoiceReturnCodeGenerationPublicKeysVectors,
						voterVoteCastReturnCodeGenerationPublicKeysVectors, exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
						proofsOfCorrectPartialChoiceReturnCodesExponentiation, exponentiatedEncryptedHashedConfirmationKeysMatrix,
						proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, otherSizeCiphertexts, voterChoiceReturnCodeGenerationPublicKeysVectors,
						voterVoteCastReturnCodeGenerationPublicKeysVectors, exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
						proofsOfCorrectPartialChoiceReturnCodesExponentiation, exponentiatedEncryptedHashedConfirmationKeysMatrix,
						proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys, otherSizePublicKeyVectors,
						voterVoteCastReturnCodeGenerationPublicKeysVectors, exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
						proofsOfCorrectPartialChoiceReturnCodesExponentiation, exponentiatedEncryptedHashedConfirmationKeysMatrix,
						proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys,
						voterChoiceReturnCodeGenerationPublicKeysVectors, otherSizePublicKeyVectors,
						exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix, proofsOfCorrectPartialChoiceReturnCodesExponentiation,
						exponentiatedEncryptedHashedConfirmationKeysMatrix, proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys,
						voterChoiceReturnCodeGenerationPublicKeysVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors, otherSizeMatrix,
						proofsOfCorrectPartialChoiceReturnCodesExponentiation, exponentiatedEncryptedHashedConfirmationKeysMatrix,
						proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys,
						voterChoiceReturnCodeGenerationPublicKeysVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors,
						exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix, otherSizeProofs,
						exponentiatedEncryptedHashedConfirmationKeysMatrix, proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys,
						voterChoiceReturnCodeGenerationPublicKeysVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors,
						exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix, proofsOfCorrectPartialChoiceReturnCodesExponentiation,
						otherSizeMatrix, proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys,
						voterChoiceReturnCodeGenerationPublicKeysVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors,
						exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix, proofsOfCorrectPartialChoiceReturnCodesExponentiation,
						exponentiatedEncryptedHashedConfirmationKeysMatrix, otherSizeProofs)

		);
	}

	@ParameterizedTest
	@MethodSource("provideOtherCCSizeArguments")
	@DisplayName("different size arguments throws IllegalArgumentException")
	void differentSizeArgumentsThrows(
			final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterChoiceReturnCodeGenerationPublicKeysVectors,
			final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterVoteCastReturnCodeGenerationPublicKeysVectors,
			final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
			final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectPartialChoiceReturnCodesExponentiation,
			final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKeysMatrix,
			final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectConfirmationKeysExponentiation) {

		final CombineEncLongCodeSharesInput.Builder builder = new CombineEncLongCodeSharesInput.Builder().setSetupSecretKey(setupSecretKey)
				.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
				.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeys)
				.setVoterChoiceReturnCodeGenerationPublicKeysVectors(voterChoiceReturnCodeGenerationPublicKeysVectors)
				.setVoterVoteCastReturnCodeGenerationPublicKeysVectors(voterVoteCastReturnCodeGenerationPublicKeysVectors)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix(exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix)
				.setProofsOfCorrectPartialChoiceReturnCodesExponentiation(proofsOfCorrectPartialChoiceReturnCodesExponentiation)
				.setExponentiatedEncryptedHashedConfirmationKeysMatrix(exponentiatedEncryptedHashedConfirmationKeysMatrix)
				.setProofsOfCorrectConfirmationKeysExponentiation(proofsOfCorrectConfirmationKeysExponentiation);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = "The size of the control component's vectors must be equal to the number of columns of the matrices.";
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	private static Stream<Arguments> provideOtherCCSizeArguments() {
		final int otherSize = ControlComponentNode.ids().size() + 1;
		final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> otherSizePublicKeyVectors = Stream.generate(
						() -> Stream.generate(() -> elGamalGenerator.genRandomPublicKey(1))
								.limit(numberOfEligibleVoters)
								.collect(toGroupVector()))
				.limit(otherSize)
				.collect(toGroupVector());
		final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> otherSizeMatrix = elGamalGenerator.genRandomCiphertextMatrix(
				numberOfEligibleVoters, otherSize, 3);
		final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> otherSizeProofs = Stream.generate(() -> Stream.generate(
						() -> new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
				.limit(numberOfEligibleVoters).collect(toGroupVector())).limit(otherSize).collect(toGroupVector());

		return Stream.of(Arguments.of(otherSizePublicKeyVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors,
						exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix, proofsOfCorrectPartialChoiceReturnCodesExponentiation,
						exponentiatedEncryptedHashedConfirmationKeysMatrix, proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(voterChoiceReturnCodeGenerationPublicKeysVectors, otherSizePublicKeyVectors,
						exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix, proofsOfCorrectPartialChoiceReturnCodesExponentiation,
						exponentiatedEncryptedHashedConfirmationKeysMatrix, proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(voterChoiceReturnCodeGenerationPublicKeysVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors, otherSizeMatrix,
						proofsOfCorrectPartialChoiceReturnCodesExponentiation, exponentiatedEncryptedHashedConfirmationKeysMatrix,
						proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(voterChoiceReturnCodeGenerationPublicKeysVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors,
						exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix, otherSizeProofs,
						exponentiatedEncryptedHashedConfirmationKeysMatrix, proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(voterChoiceReturnCodeGenerationPublicKeysVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors,
						exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix, proofsOfCorrectPartialChoiceReturnCodesExponentiation,
						otherSizeMatrix, proofsOfCorrectConfirmationKeysExponentiation),
				Arguments.of(voterChoiceReturnCodeGenerationPublicKeysVectors, voterVoteCastReturnCodeGenerationPublicKeysVectors,
						exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix, proofsOfCorrectPartialChoiceReturnCodesExponentiation,
						exponentiatedEncryptedHashedConfirmationKeysMatrix, otherSizeProofs)

		);
	}

	@Test
	@DisplayName("valid arguments does not throw")
	void validArgumentDoesNotThrow() {
		final CombineEncLongCodeSharesInput.Builder builder = new CombineEncLongCodeSharesInput.Builder().setSetupSecretKey(setupSecretKey)
				.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
				.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeys)
				.setVoterChoiceReturnCodeGenerationPublicKeysVectors(voterChoiceReturnCodeGenerationPublicKeysVectors)
				.setVoterVoteCastReturnCodeGenerationPublicKeysVectors(voterVoteCastReturnCodeGenerationPublicKeysVectors)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix(exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix)
				.setProofsOfCorrectPartialChoiceReturnCodesExponentiation(proofsOfCorrectPartialChoiceReturnCodesExponentiation)
				.setExponentiatedEncryptedHashedConfirmationKeysMatrix(exponentiatedEncryptedHashedConfirmationKeysMatrix)
				.setProofsOfCorrectConfirmationKeysExponentiation(proofsOfCorrectConfirmationKeysExponentiation);

		assertDoesNotThrow(builder::build);
	}
}