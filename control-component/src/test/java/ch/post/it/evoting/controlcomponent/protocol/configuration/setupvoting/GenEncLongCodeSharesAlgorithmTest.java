/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.google.common.base.Throwables;

import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivation;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;

/**
 * Tests of GenEncLongCodeSharesAlgorithm.
 */
@DisplayName("A GenEncLongCodeSharesAlgorithm with")
class GenEncLongCodeSharesAlgorithmTest extends TestGroupSetup {

	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final int NUM_KEY_ELEMENTS = 5;
	private static final int CONFIRMATION_KEY_SIZE = 1;

	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_SET_ID = uuidGenerator.generate();
	private static final ImmutableList<String> VERIFICATION_CARD_IDS = ImmutableList.of(
			uuidGenerator.generate(),
			uuidGenerator.generate(),
			uuidGenerator.generate());

	private static final int NODE_ID = 1;

	private static int ciphertextSize;
	private static ZqElement returnCodesGenerationSecretKey;
	private static GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> verificationCardPublicKeys;
	private static GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes;
	private static GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys;

	private final ZeroKnowledgeProof zeroKnowledgeProof = mock(ZeroKnowledgeProof.class);
	private final KeyDerivation keyDerivation = mock(KeyDerivation.class);
	private final VerificationCardService verificationCardService = mock(VerificationCardService.class);
	private final ElGamalMultiRecipientKeyPair ccmKeyPair = ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, NUM_KEY_ELEMENTS, random);

	private GenEncLongCodeSharesAlgorithm genEncLongCodeSharesAlgorithm;
	private GenEncLongCodeSharesContext context;
	private GenEncLongCodeSharesInput input;

	@BeforeAll
	static void setupAll() {
		ciphertextSize = new SecureRandom().nextInt(5) + 1;
	}

	@BeforeEach
	void setup() {
		genEncLongCodeSharesAlgorithm = new GenEncLongCodeSharesAlgorithm(keyDerivation, zeroKnowledgeProof, verificationCardService);

		returnCodesGenerationSecretKey = zqGroupGenerator.genRandomZqElementMember();
		verificationCardPublicKeys = GroupVector.of(ccmKeyPair.getPublicKey(), ccmKeyPair.getPublicKey(), ccmKeyPair.getPublicKey());
		encryptedHashedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertextVector(3, ciphertextSize);
		encryptedHashedConfirmationKeys = elGamalGenerator.genRandomCiphertextVector(3, CONFIRMATION_KEY_SIZE);

		context = new GenEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(gqGroup)
				.setNodeId(NODE_ID)
				.setElectionEventId(ELECTION_EVENT_ID)
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID)
				.setVerificationCardIds(VERIFICATION_CARD_IDS)
				.setNumberOfVotingOptions(ciphertextSize)
				.build();

		input = new GenEncLongCodeSharesInput.Builder()
				.setReturnCodesGenerationSecretKey(returnCodesGenerationSecretKey)
				.setVerificationCardPublicKeys(verificationCardPublicKeys)
				.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
				.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeys)
				.build();

		when(verificationCardService.existsNone(any())).thenReturn(true);
		when(keyDerivation.KDFToZq(any(), any(), any())).thenReturn(zqGroupGenerator.genRandomZqElementMember());
		when(zeroKnowledgeProof.genExponentiationProof(any(), any(), any(), any()))
				.thenReturn(new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()));
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParamDoesNotThrow() {
		assertDoesNotThrow(() -> genEncLongCodeSharesAlgorithm.genEncLongCodeShares(context, input));

		verify(verificationCardService, times(1)).existsNone(any());
		verify(verificationCardService, times(1)).saveAll(any());
	}

	@Test
	@DisplayName("null parameter throws NullPointerException")
	void nullParamThrows() {
		assertThrows(NullPointerException.class, () -> genEncLongCodeSharesAlgorithm.genEncLongCodeShares(null, null));
		assertThrows(NullPointerException.class, () -> genEncLongCodeSharesAlgorithm.genEncLongCodeShares(context, null));
		assertThrows(NullPointerException.class, () -> genEncLongCodeSharesAlgorithm.genEncLongCodeShares(null, input));
	}

	@Test
	@DisplayName("parameters already been generated voting cards IllegalArgumentException")
	void alreadyGeneratedVotingCardThrow() {
		when(verificationCardService.existsNone(any())).thenReturn(false);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genEncLongCodeSharesAlgorithm.genEncLongCodeShares(context, input));
		assertEquals("At least one of the voting cards has already been generated.", Throwables.getRootCause(exception).getMessage());

	}

	@Nested
	@DisplayName("using a GenEncLongCodeSharesInput built with")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class GenEncLongCodeSharesInputTest {

		@Test
		@DisplayName("any null parameter throws NullPointerException")
		void anyNullParameter() {
			final GenEncLongCodeSharesInput.Builder builderWithNullReturnCodesGenerationSecretKey = new GenEncLongCodeSharesInput.Builder()
					.setVerificationCardPublicKeys(verificationCardPublicKeys)
					.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
					.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeys);

			final GenEncLongCodeSharesInput.Builder builderWithNullVerificationCardPublicKeys = new GenEncLongCodeSharesInput.Builder()
					.setReturnCodesGenerationSecretKey(returnCodesGenerationSecretKey)
					.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
					.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeys);

			final GenEncLongCodeSharesInput.Builder builderWithNullEncryptedHashedPartialChoiceReturnCodes = new GenEncLongCodeSharesInput.Builder()
					.setReturnCodesGenerationSecretKey(returnCodesGenerationSecretKey)
					.setVerificationCardPublicKeys(verificationCardPublicKeys)
					.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeys);

			final GenEncLongCodeSharesInput.Builder builderWithNullEncryptedHashedConfirmationKeys = new GenEncLongCodeSharesInput.Builder()
					.setReturnCodesGenerationSecretKey(returnCodesGenerationSecretKey)
					.setVerificationCardPublicKeys(verificationCardPublicKeys)
					.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes);

			assertAll(
					() -> assertThrows(NullPointerException.class, builderWithNullReturnCodesGenerationSecretKey::build),
					() -> assertThrows(NullPointerException.class, builderWithNullVerificationCardPublicKeys::build),
					() -> assertThrows(NullPointerException.class, builderWithNullEncryptedHashedPartialChoiceReturnCodes::build),
					() -> assertThrows(NullPointerException.class, builderWithNullEncryptedHashedConfirmationKeys::build)
			);
		}

		@Test
		@DisplayName("failing size checks throws IllegalArgumentException")
		void failingSizeChecks() {
			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> verificationCardPublicKeysPlusOne =
					GroupVector.of(ccmKeyPair.getPublicKey(), ccmKeyPair.getPublicKey(), ccmKeyPair.getPublicKey(), ccmKeyPair.getPublicKey());

			GenEncLongCodeSharesInput.Builder builder =
					new GenEncLongCodeSharesInput.Builder()
							.setReturnCodesGenerationSecretKey(returnCodesGenerationSecretKey)
							.setVerificationCardPublicKeys(verificationCardPublicKeysPlusOne)
							.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
							.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeys);

			IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

			String message = String.format(
					"The vector encrypted, hashed partial Choice Return Codes is of incorrect size [size: expected: %s, actual: %s]",
					verificationCardPublicKeysPlusOne.size(), encryptedHashedPartialChoiceReturnCodes.size());
			assertEquals(message, Throwables.getRootCause(exception).getMessage());

			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodesPlusOne =
					elGamalGenerator.genRandomCiphertextVector(4, 1);

			builder = builder.setVerificationCardPublicKeys(verificationCardPublicKeys)
					.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodesPlusOne);

			exception = assertThrows(IllegalArgumentException.class, builder::build);

			message = String.format("The vector encrypted, hashed partial Choice Return Codes is of incorrect size [size: expected: %S, actual: %S]",
					verificationCardPublicKeys.size(), verificationCardPublicKeysPlusOne.size());
			assertEquals(message, Throwables.getRootCause(exception).getMessage());

			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeysPlusOne =
					elGamalGenerator.genRandomCiphertextVector(4, 1);

			builder = builder.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
					.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeysPlusOne);

			exception = assertThrows(IllegalArgumentException.class, builder::build);

			message = String.format("The vector encrypted, hashed Confirmation Keys is of incorrect size [size: expected: %S, actual: %S]",
					verificationCardPublicKeys.size(), encryptedHashedConfirmationKeysPlusOne.size());
			assertEquals(message, Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("invalid size confirmation key throws IllegalArgumentException")
		void invalidSizeConfirmationKey() {
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> tooManyElementsConfirmationKey = elGamalGenerator.genRandomCiphertextVector(3,
					2);

			final GenEncLongCodeSharesInput.Builder builder =
					new GenEncLongCodeSharesInput.Builder()
							.setReturnCodesGenerationSecretKey(returnCodesGenerationSecretKey)
							.setVerificationCardPublicKeys(verificationCardPublicKeys)
							.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
							.setEncryptedHashedConfirmationKeys(tooManyElementsConfirmationKey);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

			final String message = String.format("The encrypted hashed Confirmation keys must be of size 1. [actual phi: %s]", 2);
			assertEquals(message, Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("different group orders throws IllegalArgumentException")
		void differentGroupOrders() {
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeysOtherGroup =
					otherGroupElGamalGenerator.genRandomCiphertextVector(3, 1);

			final GenEncLongCodeSharesInput.Builder builder =
					new GenEncLongCodeSharesInput.Builder()
							.setReturnCodesGenerationSecretKey(returnCodesGenerationSecretKey)
							.setVerificationCardPublicKeys(verificationCardPublicKeys)
							.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
							.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeysOtherGroup);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

			final String message = "The Vector of exponentiated, encrypted, hashed partial Choice Return Codes and the Vector of exponentiated, "
					+ "encrypted, hashed Confirmation Keys do not have the same group order.";
			assertEquals(message, Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("different groups throws IllegalArgumentException")
		void differentGroups() {
			final ElGamalMultiRecipientKeyPair ccmKeyPairOther = ElGamalMultiRecipientKeyPair.genKeyPair(otherGqGroup, NUM_KEY_ELEMENTS, random);
			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> verificationCardPublicKeysOther = GroupVector.of(
					ccmKeyPairOther.getPublicKey(),
					ccmKeyPairOther.getPublicKey(), ccmKeyPairOther.getPublicKey());

			final GenEncLongCodeSharesInput.Builder builder =
					new GenEncLongCodeSharesInput.Builder()
							.setReturnCodesGenerationSecretKey(returnCodesGenerationSecretKey)
							.setVerificationCardPublicKeys(verificationCardPublicKeysOther)
							.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
							.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeys);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

			final String message = "The exponentiated, encrypted, hashed partial Choice Return Codes and the verification card public keys must have the same group.";
			assertEquals(message, Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("valid parameters gives expected input")
		void validParameters() {
			final GenEncLongCodeSharesInput.Builder builder =
					new GenEncLongCodeSharesInput.Builder()
							.setReturnCodesGenerationSecretKey(returnCodesGenerationSecretKey)
							.setVerificationCardPublicKeys(verificationCardPublicKeys)
							.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
							.setEncryptedHashedConfirmationKeys(encryptedHashedConfirmationKeys);

			assertDoesNotThrow(builder::build);
		}
	}
}
