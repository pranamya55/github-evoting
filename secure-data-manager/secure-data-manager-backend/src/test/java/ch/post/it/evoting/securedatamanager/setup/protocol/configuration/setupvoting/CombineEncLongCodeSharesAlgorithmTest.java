/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupMatrix;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.domain.generators.ControlComponentCodeSharesPayloadGenerator;
import ch.post.it.evoting.domain.generators.SetupComponentVerificationDataPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationData;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;

@DisplayName("calling combineEncLongCodeShares with")
class CombineEncLongCodeSharesAlgorithmTest {

	private CombineEncLongCodeSharesContext context;
	private CombineEncLongCodeSharesInput input;
	private CombineEncLongCodeSharesAlgorithm combineEncLongCodeSharesAlgorithm;
	private VerifyEncryptedPCCExponentiationProofsAlgorithm verifyEncryptedPCCExponentiationProofsAlgorithm;
	private VerifyEncryptedCKExponentiationProofsAlgorithm verifyEncryptedCKExponentiationProofsAlgorithm;

	@BeforeEach
	void setUp() {
		final SetupComponentVerificationDataPayloadGenerator generator = new SetupComponentVerificationDataPayloadGenerator();
		final SetupComponentVerificationDataPayload setupComponentVerificationDataPayload = generator.generate();

		context = getCombineEncLongCodeSharesContext(setupComponentVerificationDataPayload);
		input = getCombineEncLongCodeSharesInput(setupComponentVerificationDataPayload, context);

		final ElGamal elGamal = ElGamalFactory.createElGamal();
		final Hash hash = HashFactory.createHash();
		final Base64 base64 = BaseEncodingFactory.createBase64();
		verifyEncryptedPCCExponentiationProofsAlgorithm = mock(VerifyEncryptedPCCExponentiationProofsAlgorithm.class);
		verifyEncryptedCKExponentiationProofsAlgorithm = mock(VerifyEncryptedCKExponentiationProofsAlgorithm.class);
		combineEncLongCodeSharesAlgorithm = new CombineEncLongCodeSharesAlgorithm(hash, elGamal, base64,
				verifyEncryptedPCCExponentiationProofsAlgorithm, verifyEncryptedCKExponentiationProofsAlgorithm);

		when(verifyEncryptedPCCExponentiationProofsAlgorithm.verifyEncryptedPCCExponentiationProofs(any(), any())).thenReturn(true);
		when(verifyEncryptedCKExponentiationProofsAlgorithm.verifyEncryptedCKExponentiationProofs(any(), any())).thenReturn(true);
	}

	@Test
	@DisplayName("null context throws NullPointerException")
	void nullContextThrows() {
		assertThrows(NullPointerException.class, () -> combineEncLongCodeSharesAlgorithm.combineEncLongCodeShares(null, input));
	}

	@Test
	@DisplayName("null input throws NullPointerException")
	void nullInputThrows() {
		assertThrows(NullPointerException.class, () -> combineEncLongCodeSharesAlgorithm.combineEncLongCodeShares(context, null));
	}

	@Test
	@DisplayName("different size verification card ids throws IllegalArgumentException")
	void differentSizeVerificationCardIdsThrows() {
		final ImmutableList<String> otherSizeVerificationCardIds = context.getVerificationCardIds().subList(0, 2);
		final CombineEncLongCodeSharesContext combineEncLongCodeSharesContext = new CombineEncLongCodeSharesContext.Builder().setEncryptionGroup(
						context.getEncryptionGroup()).setElectionEventId(context.getElectionEventId())
				.setVerificationCardSetId(context.getVerificationCardSetId()).setVerificationCardIds(otherSizeVerificationCardIds)
				.setNumberOfVotingOptions(context.getNumberOfVotingOptions())
				.setMaximumNumberOfVotingOptions(context.getMaximumNumberOfVotingOptions()).build();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> combineEncLongCodeSharesAlgorithm.combineEncLongCodeShares(combineEncLongCodeSharesContext, input));

		final String expectedMessage = "The number of rows of the matrices C_expPCC and C_expCK must be equal to the number of eligible voters.";
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("invalid pCC proofs throws IllegalStateException")
	void invalidPCCProofsThrows() {
		when(verifyEncryptedPCCExponentiationProofsAlgorithm.verifyEncryptedPCCExponentiationProofs(any(), any())).thenReturn(false);

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> combineEncLongCodeSharesAlgorithm.combineEncLongCodeShares(context, input));

		final String expectedMessage = String.format(
				"The proofs of correct exponentiation of the partial Choice Return Codes or of the Confirmation Keys are invalid. [j: %s, ee: %s, vcs: %s]",
				1, context.getElectionEventId(), context.getVerificationCardSetId());
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("invalid CK proofs throws IllegalStateException")
	void invalidCKProofsThrows() {
		when(verifyEncryptedCKExponentiationProofsAlgorithm.verifyEncryptedCKExponentiationProofs(any(), any())).thenReturn(false);

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> combineEncLongCodeSharesAlgorithm.combineEncLongCodeShares(context, input));

		final String expectedMessage = String.format(
				"The proofs of correct exponentiation of the partial Choice Return Codes or of the Confirmation Keys are invalid. [j: %s, ee: %s, vcs: %s]",
				1, context.getElectionEventId(), context.getVerificationCardSetId());
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("The context and input are not null")
	void testWithNonNullContextAndInput() {
		final CombineEncLongCodeSharesOutput output = combineEncLongCodeSharesAlgorithm.combineEncLongCodeShares(context, input);

		assertNotNull(output);

		final int N_E = input.getExponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix().numRows();

		final ImmutableList<Integer> sizes = ImmutableList.of(output.getEncryptedPreChoiceReturnCodesVector().size(),
				output.getPreVoteCastReturnCodesVector().size(), output.getLongVoteCastReturnCodesAllowList().size());

		assertTrue(sizes.stream().allMatch(size -> size == N_E));
	}

	private CombineEncLongCodeSharesContext getCombineEncLongCodeSharesContext(final SetupComponentVerificationDataPayload payload) {
		final Random random = RandomFactory.createRandom();

		final GqGroup encryptionGroup = payload.getEncryptionGroup();
		final String electionEventId = payload.getElectionEventId();
		final String verificationCardSetId = payload.getVerificationCardSetId();

		final ImmutableList<SetupComponentVerificationData> setupComponentVerificationData = payload.getSetupComponentVerificationData();
		final ImmutableList<String> verificationCardIds = setupComponentVerificationData.stream()
				.map(SetupComponentVerificationData::verificationCardId).collect(ImmutableList.toImmutableList());

		final int numberOfVotingOptions = setupComponentVerificationData.getFirst().encryptedHashedSquaredPartialChoiceReturnCodes().size();
		final int maximumNumberOfVotingOptions = random.genRandomInteger(MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS - numberOfVotingOptions)
				+ numberOfVotingOptions; // in range [n,n_sup]

		return new CombineEncLongCodeSharesContext.Builder().setEncryptionGroup(encryptionGroup).setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId).setVerificationCardIds(verificationCardIds)
				.setNumberOfVotingOptions(numberOfVotingOptions).setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions).build();
	}

	private CombineEncLongCodeSharesInput getCombineEncLongCodeSharesInput(final SetupComponentVerificationDataPayload payload,
			final CombineEncLongCodeSharesContext context) {
		final int numberOfEligibleVoters = context.getVerificationCardIds().size();

		final GqGroup encryptionGroup = context.getEncryptionGroup();
		final ControlComponentCodeSharesPayloadGenerator generator = new ControlComponentCodeSharesPayloadGenerator(encryptionGroup);
		final ImmutableList<ControlComponentCodeSharesPayload> controlComponentCodeSharesPayloads = generator.generate(payload.getElectionEventId(),
				payload.getVerificationCardSetId(), payload.getChunkId(), numberOfEligibleVoters, context.getNumberOfVotingOptions());

		final ElGamalMultiRecipientPrivateKey setupSecretKey = new ElGamalGenerator(encryptionGroup)
				.genRandomPrivateKey(context.getMaximumNumberOfVotingOptions());

		final ImmutableList<SetupComponentVerificationData> setupComponentVerificationData = payload.getSetupComponentVerificationData();

		return new CombineEncLongCodeSharesInput.Builder().setSetupSecretKey(setupSecretKey).setEncryptedHashedPartialChoiceReturnCodes(
						setupComponentVerificationData.stream().map(SetupComponentVerificationData::encryptedHashedSquaredPartialChoiceReturnCodes)
								.collect(toGroupVector())).setEncryptedHashedConfirmationKeys(
						setupComponentVerificationData.stream().map(SetupComponentVerificationData::encryptedHashedSquaredConfirmationKey)
								.collect(toGroupVector())).setVoterChoiceReturnCodeGenerationPublicKeysVectors(
						controlComponentCodeSharesPayloads.stream().map(ControlComponentCodeSharesPayload::getControlComponentCodeShares)
								.map(share -> share.stream().map(ControlComponentCodeShare::voterChoiceReturnCodeGenerationPublicKey)
										.collect(toGroupVector())).collect(toGroupVector())).setVoterVoteCastReturnCodeGenerationPublicKeysVectors(
						controlComponentCodeSharesPayloads.stream().map(ControlComponentCodeSharesPayload::getControlComponentCodeShares)
								.map(share -> share.stream().map(ControlComponentCodeShare::voterVoteCastReturnCodeGenerationPublicKey)
										.collect(toGroupVector())).collect(toGroupVector())).setExponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix(
						GroupMatrix.fromColumns(
								controlComponentCodeSharesPayloads.stream().map(ControlComponentCodeSharesPayload::getControlComponentCodeShares)
										.map(share -> share.stream().map(ControlComponentCodeShare::exponentiatedEncryptedPartialChoiceReturnCodes)
												.collect(toGroupVector())).collect(toGroupVector())))
				.setProofsOfCorrectPartialChoiceReturnCodesExponentiation(
						controlComponentCodeSharesPayloads.stream().map(ControlComponentCodeSharesPayload::getControlComponentCodeShares)
								.map(share -> share.stream().map(ControlComponentCodeShare::encryptedPartialChoiceReturnCodeExponentiationProof)
										.collect(toGroupVector())).collect(toGroupVector())).setExponentiatedEncryptedHashedConfirmationKeysMatrix(
						GroupMatrix.fromColumns(
								controlComponentCodeSharesPayloads.stream().map(ControlComponentCodeSharesPayload::getControlComponentCodeShares)
										.map(share -> share.stream().map(ControlComponentCodeShare::exponentiatedEncryptedConfirmationKey)
												.collect(toGroupVector())).collect(toGroupVector())))
				.setProofsOfCorrectConfirmationKeysExponentiation(
						controlComponentCodeSharesPayloads.stream().map(ControlComponentCodeSharesPayload::getControlComponentCodeShares)
								.map(share -> share.stream().map(ControlComponentCodeShare::encryptedConfirmationKeyExponentiationProof)
										.collect(toGroupVector())).collect(toGroupVector())).build();

	}
}
