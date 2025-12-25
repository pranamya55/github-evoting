/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Throwables;

import ch.post.it.evoting.controlcomponent.process.ExtractedElectionEventHashService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms.GetHashExtractedElectionEventService;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.generators.ExtractedElectionEventGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashExtractedElectionEventAlgorithm;

/**
 * Tests of PartialDecryptPCCAlgorithm.
 */
@DisplayName("PartialDecryptPCCAlgorithm")
public class PartialDecryptPCCAlgorithmTest extends TestGroupSetup {

	private static final Base64 base64 = BaseEncodingFactory.createBase64();
	private static final Hash hash = HashFactory.createHash();

	private static final int NODE_ID = 1;
	private static final int PSI = 5;
	private static final int DELTA = 1;

	private static final ZeroKnowledgeProof zeroKnowledgeProof = spy(ZeroKnowledgeProof.class);
	private static final VerificationCardStateService verificationCardStateService = mock(VerificationCardStateService.class);
	private static final ExtractedElectionEventHashService extractedElectionEventHashService = mock(ExtractedElectionEventHashService.class);

	private static PartialDecryptPCCAlgorithm partialDecryptPCCAlgorithm;

	@BeforeAll
	static void setUpAll() {
		final GetHashExtractedElectionEventService getHashExtractedElectionEventService = new GetHashExtractedElectionEventService(
				extractedElectionEventHashService);
		partialDecryptPCCAlgorithm = new PartialDecryptPCCAlgorithm(zeroKnowledgeProof, verificationCardStateService,
				getHashExtractedElectionEventService);
	}

	@Nested
	@SuppressWarnings("java:S117")
	@DisplayName("calling partialDecryptPCC with")
	class PartialDecryptPCCTest {

		private final Random random = RandomFactory.createRandom();
		private PartialDecryptPCCInput input;
		private DecryptPCCContext context;
		private String electionEventId;
		private String verificationCardId;
		private int psi;

		@BeforeEach
		void setUp() {
			final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
			electionEventId = uuidGenerator.generate();
			verificationCardId = uuidGenerator.generate();

			final ExtractedElectionEventGenerator generator = new ExtractedElectionEventGenerator(gqGroup);
			final ExtractedElectionEvent extractedElectionEvent = generator.generate(electionEventId);
			psi = 10;

			final PartialDecryptPCCInput.Builder builder = new PartialDecryptPCCInput.Builder();
			final ElGamalMultiRecipientCiphertext encryptedVote = elGamalGenerator.genRandomCiphertext(1);
			final ZqElement k_id = zqGroupGenerator.genRandomZqElementMember();
			final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote = encryptedVote.getCiphertextExponentiation(k_id);
			final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(psi);
			final ElGamalMultiRecipientKeyPair keyPair = ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, psi, random);

			input = builder.setEncryptedVote(encryptedVote)
					.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
					.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes)
					.setCcrjChoiceReturnCodesEncryptionSecretKey(keyPair.getPrivateKey())
					.setCcrjChoiceReturnCodesEncryptionPublicKey(keyPair.getPublicKey())
					.build();

			context = new DecryptPCCContext.Builder()
					.setNodeId(NODE_ID)
					.setVerificationCardId(verificationCardId)
					.setNumberOfSelections(psi)
					.setNumberOfWriteInsPlusOne(DELTA)
					.setEncryptionGroup(gqGroup)
					.setElectionEventId(electionEventId)
					.build();

			final GetHashExtractedElectionEventAlgorithm getHashExtractedElectionEventAlgorithm = new GetHashExtractedElectionEventAlgorithm(base64,
					hash);
			final String extractedElectionEventHash = getHashExtractedElectionEventAlgorithm.getHashExtractedElectionEvent(extractedElectionEvent);
			when(extractedElectionEventHashService.getHashExtractedElectionEvent(electionEventId)).thenReturn(extractedElectionEventHash);
		}

		@Test
		@DisplayName("valid parameters does not throw")
		void validParams() {
			when(verificationCardStateService.isNotPartiallyDecrypted(verificationCardId)).thenReturn(true);

			final ExponentiationProof exponentiationProof = new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(),
					zqGroupGenerator.genRandomZqElementMember());
			doReturn(exponentiationProof).when(zeroKnowledgeProof).genExponentiationProof(any(), any(), any(), any());

			final PartialDecryptPCCOutput output = partialDecryptPCCAlgorithm.partialDecryptPCC(context, input);
			assertEquals(psi, output.exponentiatedGammas().size());
			assertEquals(psi, output.exponentiationProofs().size());
			assertEquals(gqGroup, output.exponentiatedGammas().getGroup());
			assertTrue(gqGroup.hasSameOrderAs(output.exponentiationProofs().getGroup()));
		}

		@Test
		@DisplayName("any null parameters throws NullPointerException")
		void nullParams() {
			assertAll(
					() -> assertThrows(NullPointerException.class, () -> partialDecryptPCCAlgorithm.partialDecryptPCC(context, null)),
					() -> assertThrows(NullPointerException.class, () -> partialDecryptPCCAlgorithm.partialDecryptPCC(null, input))
			);
		}

		@Test
		@DisplayName("context and input with different groups throws IllegalArgumentException")
		void differentGroupContextInput() {
			final DecryptPCCContext otherContext = new DecryptPCCContext.Builder()
					.setNodeId(NODE_ID)
					.setVerificationCardId(verificationCardId)
					.setNumberOfSelections(psi)
					.setNumberOfWriteInsPlusOne(DELTA)
					.setEncryptionGroup(otherGqGroup)
					.setElectionEventId(electionEventId)
					.build();

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> partialDecryptPCCAlgorithm.partialDecryptPCC(otherContext, input));
			assertEquals("The context and input must have the same group.", Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("wrong size encrypted partial Choice Return Codes")
		void wrongSizeEncryptedPartialChoiceReturnCodes() {
			final DecryptPCCContext otherContext = new DecryptPCCContext.Builder()
					.setNodeId(NODE_ID)
					.setVerificationCardId(verificationCardId)
					.setNumberOfSelections(psi + 1)
					.setNumberOfWriteInsPlusOne(DELTA)
					.setEncryptionGroup(gqGroup)
					.setElectionEventId(electionEventId)
					.build();

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> partialDecryptPCCAlgorithm.partialDecryptPCC(otherContext, input));
			assertEquals(String.format(
							"There must be psi encrypted partial Choice Return Codes. [psi: %s]", psi + 1),
					Throwables.getRootCause(exception).getMessage());

		}

		@Test
		@DisplayName("wrong size encrypted vote")
		void wrongSizeEncryptedVote() {
			final DecryptPCCContext otherContext = new DecryptPCCContext.Builder()
					.setNodeId(NODE_ID)
					.setVerificationCardId(verificationCardId)
					.setNumberOfSelections(psi)
					.setNumberOfWriteInsPlusOne(DELTA + 1)
					.setEncryptionGroup(gqGroup)
					.setElectionEventId(electionEventId)
					.build();

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> partialDecryptPCCAlgorithm.partialDecryptPCC(otherContext, input));
			assertEquals(
					String.format("There must be delta encrypted vote elements. [delta: %s]", 2),
					Throwables.getRootCause(exception).getMessage());

		}

		@Test
		@DisplayName("already partially decrypted pCC throws IllegalArgumentException")
		void alreadyPartiallyDecryptedPCC() {
			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> partialDecryptPCCAlgorithm.partialDecryptPCC(context, input));
			assertEquals("The partial Choice Return Codes have already been partially decrypted.", Throwables.getRootCause(exception).getMessage());
		}

	}

	@Nested
	@SuppressWarnings("java:S117")
	@DisplayName("PartialDecryptPCCInput.Builder with")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class BuilderTest {

		private final Random random = RandomFactory.createRandom();

		private ElGamalMultiRecipientCiphertext encryptedVote;
		private ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote;
		private ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes;
		private ElGamalMultiRecipientPrivateKey secretKey;
		private ElGamalMultiRecipientPublicKey publicKey;

		@BeforeEach
		void setUp() {
			encryptedVote = elGamalGenerator.genRandomCiphertext(1);
			final ZqElement k_id = zqGroupGenerator.genRandomZqElementMember();
			exponentiatedEncryptedVote = encryptedVote.getCiphertextExponentiation(k_id);
			encryptedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(PSI);

			final ElGamalMultiRecipientKeyPair keyPair = ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, PSI, random);
			secretKey = keyPair.getPrivateKey();
			publicKey = keyPair.getPublicKey();
		}

		@Test
		@DisplayName("valid parameters does not throw")
		void validParams() {
			final PartialDecryptPCCInput.Builder builder = new PartialDecryptPCCInput.Builder()
					.setEncryptedVote(encryptedVote)
					.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
					.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes)
					.setCcrjChoiceReturnCodesEncryptionSecretKey(secretKey)
					.setCcrjChoiceReturnCodesEncryptionPublicKey(publicKey);

			assertDoesNotThrow(builder::build);
		}

		private Stream<Arguments> nullArgumentProvider() {
			return Stream.of(
					Arguments.of(null, exponentiatedEncryptedVote, encryptedPartialChoiceReturnCodes, secretKey, publicKey),
					Arguments.of(encryptedVote, null, encryptedPartialChoiceReturnCodes, secretKey, publicKey),
					Arguments.of(encryptedVote, exponentiatedEncryptedVote, null, secretKey, publicKey),
					Arguments.of(encryptedVote, exponentiatedEncryptedVote, encryptedPartialChoiceReturnCodes, null, publicKey),
					Arguments.of(encryptedVote, exponentiatedEncryptedVote, encryptedPartialChoiceReturnCodes, secretKey, null)
			);
		}

		@ParameterizedTest
		@MethodSource("nullArgumentProvider")
		@DisplayName("any null parameter throws NullPointerException")
		void nullParams(final ElGamalMultiRecipientCiphertext encryptedVote,
				final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote,
				final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes, final ElGamalMultiRecipientPrivateKey secretKey,
				final ElGamalMultiRecipientPublicKey publicKey) {

			final PartialDecryptPCCInput.Builder builder = new PartialDecryptPCCInput.Builder()
					.setEncryptedVote(encryptedVote)
					.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
					.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes)
					.setCcrjChoiceReturnCodesEncryptionSecretKey(secretKey)
					.setCcrjChoiceReturnCodesEncryptionPublicKey(publicKey);

			assertThrows(NullPointerException.class, builder::build);
		}

		@Test
		@DisplayName("wrong size exponentiated encryptedVote vote throws IllegalArgumentException")
		void wrongSizeExponentiatedEncryptedVote() {
			final ElGamalMultiRecipientCiphertext wrongExponentiatedEncryptedVote =
					ElGamalMultiRecipientCiphertext.create(encryptedVote.getGamma(), gqGroupGenerator.genRandomGqElementVector(2));

			final PartialDecryptPCCInput.Builder builder = new PartialDecryptPCCInput.Builder()
					.setEncryptedVote(encryptedVote)
					.setExponentiatedEncryptedVote(wrongExponentiatedEncryptedVote)
					.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes)
					.setCcrjChoiceReturnCodesEncryptionSecretKey(secretKey)
					.setCcrjChoiceReturnCodesEncryptionPublicKey(publicKey);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
			assertEquals("The exponentiated encrypted vote must be of size 1.",
					Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("different secret and public keys size throws IllegalArgumentException")
		void differentSecretAndPublicKeySizes() {
			final ElGamalMultiRecipientKeyPair longKeyPair = ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, PSI + 1, random);
			final PartialDecryptPCCInput.Builder longBuilder = new PartialDecryptPCCInput.Builder()
					.setEncryptedVote(encryptedVote)
					.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
					.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes)
					.setCcrjChoiceReturnCodesEncryptionSecretKey(secretKey)
					.setCcrjChoiceReturnCodesEncryptionPublicKey(longKeyPair.getPublicKey());

			final IllegalArgumentException longException = assertThrows(IllegalArgumentException.class, longBuilder::build);
			assertEquals("CCRj Choice Return Codes encryption secret key and public key must have the same size.",
					Throwables.getRootCause(longException).getMessage());

			final ElGamalMultiRecipientKeyPair shortKeyPair = ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, PSI - 1, random);
			final PartialDecryptPCCInput.Builder shortBuilder = new PartialDecryptPCCInput.Builder()
					.setEncryptedVote(encryptedVote)
					.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
					.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes)
					.setCcrjChoiceReturnCodesEncryptionSecretKey(secretKey)
					.setCcrjChoiceReturnCodesEncryptionPublicKey(shortKeyPair.getPublicKey());

			final IllegalArgumentException shortException = assertThrows(IllegalArgumentException.class, shortBuilder::build);
			assertEquals("CCRj Choice Return Codes encryption secret key and public key must have the same size.",
					Throwables.getRootCause(shortException).getMessage());
		}

		private Stream<Arguments> differentGroupArgumentProvider() {
			final ElGamalMultiRecipientCiphertext otherEncryptedVote = otherGroupElGamalGenerator.genRandomCiphertext(1);
			final ZqElement k_id = otherZqGroupGenerator.genRandomZqElementMember();
			final ElGamalMultiRecipientCiphertext otherExponentiatedEncryptedVote = otherEncryptedVote.getCiphertextExponentiation(k_id);
			final ElGamalMultiRecipientCiphertext otherEncryptedPartialChoiceReturnCodes = otherGroupElGamalGenerator.genRandomCiphertext(PSI);
			final ElGamalMultiRecipientKeyPair otherKeyPair = ElGamalMultiRecipientKeyPair.genKeyPair(otherGqGroup, PSI, random);

			return Stream.of(
					Arguments.of(otherEncryptedVote, exponentiatedEncryptedVote, encryptedPartialChoiceReturnCodes, publicKey),
					Arguments.of(encryptedVote, otherExponentiatedEncryptedVote, encryptedPartialChoiceReturnCodes, publicKey),
					Arguments.of(encryptedVote, exponentiatedEncryptedVote, otherEncryptedPartialChoiceReturnCodes, publicKey),
					Arguments.of(encryptedVote, exponentiatedEncryptedVote, encryptedPartialChoiceReturnCodes, otherKeyPair.getPublicKey())
			);
		}

		@ParameterizedTest
		@MethodSource("differentGroupArgumentProvider")
		@DisplayName("different group parameters throws IllegalArgumentException")
		void differentGroup(final ElGamalMultiRecipientCiphertext encryptedVote, final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote,
				final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes, final ElGamalMultiRecipientPublicKey publicKey) {

			final PartialDecryptPCCInput.Builder builder = new PartialDecryptPCCInput.Builder()
					.setEncryptedVote(encryptedVote)
					.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
					.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes)
					.setCcrjChoiceReturnCodesEncryptionSecretKey(secretKey)
					.setCcrjChoiceReturnCodesEncryptionPublicKey(publicKey);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
			assertEquals("All input encryption groups must be the same.", Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("different group order secret key")
		void differentOrderSecretKey() {
			final ElGamalMultiRecipientKeyPair otherKeyPair = ElGamalMultiRecipientKeyPair.genKeyPair(otherGqGroup, PSI, random);
			final PartialDecryptPCCInput.Builder builder = new PartialDecryptPCCInput.Builder()
					.setEncryptedVote(encryptedVote)
					.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
					.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes)
					.setCcrjChoiceReturnCodesEncryptionSecretKey(otherKeyPair.getPrivateKey())
					.setCcrjChoiceReturnCodesEncryptionPublicKey(publicKey);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
			assertEquals("The private key and the generator must belong to groups of the same order.",
					Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("not matching keys")
		void notMatchingKeys() {
			final ElGamalMultiRecipientKeyPair keyPair = ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, PSI, random);
			final PartialDecryptPCCInput.Builder builder = new PartialDecryptPCCInput.Builder()
					.setEncryptedVote(encryptedVote)
					.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
					.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes)
					.setCcrjChoiceReturnCodesEncryptionSecretKey(keyPair.getPrivateKey())
					.setCcrjChoiceReturnCodesEncryptionPublicKey(publicKey);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
			assertEquals("The secret and public keys do not match.", Throwables.getRootCause(exception).getMessage());
		}
	}

	@Nested
	@DisplayName("PartialDecryptPCCOutput with")
	class PartialDecryptPCCOutputTest {

		private GroupVector<GqElement, GqGroup> exponentiatedGammas;
		private GroupVector<ExponentiationProof, ZqGroup> exponentiationProofs;

		@BeforeEach
		void setUp() {
			exponentiatedGammas = gqGroupGenerator.genRandomGqElementVector(PSI);

			final ZqElement e = zqGroupGenerator.genRandomZqElementMember();
			final ZqElement z = zqGroupGenerator.genRandomZqElementMember();
			exponentiationProofs = Stream.generate(() -> new ExponentiationProof(e, z)).limit(PSI).collect(GroupVector.toGroupVector());
		}

		@Test
		@DisplayName("valid parameters does not throw")
		void validParams() {
			assertDoesNotThrow(() -> new PartialDecryptPCCOutput(exponentiatedGammas, exponentiationProofs));
		}

		@Test
		@DisplayName("any null parameter throws NullPointerException")
		void nullParams() {
			assertThrows(NullPointerException.class, () -> new PartialDecryptPCCOutput(null, exponentiationProofs));
			assertThrows(NullPointerException.class, () -> new PartialDecryptPCCOutput(exponentiatedGammas, null));
		}

		@Test
		@DisplayName("exponentiated gammas and exponentiation proofs of different size throws IllegalArgumentException")
		void differentSizeGammasAndExponentiationProofs() {
			final GroupVector<GqElement, GqGroup> tooLongGammas = gqGroupGenerator.genRandomGqElementVector(PSI + 1);
			final IllegalArgumentException tooLongGammasException = assertThrows(IllegalArgumentException.class,
					() -> new PartialDecryptPCCOutput(tooLongGammas, exponentiationProofs));
			assertEquals("There must be as many exponentiated gammas as there are exponentiation proofs.",
					Throwables.getRootCause(tooLongGammasException).getMessage());

			final GroupVector<GqElement, GqGroup> tooShortGammas = gqGroupGenerator.genRandomGqElementVector(PSI - 1);
			final IllegalArgumentException tooShortGammasException = assertThrows(IllegalArgumentException.class,
					() -> new PartialDecryptPCCOutput(tooShortGammas, exponentiationProofs));
			assertEquals("There must be as many exponentiated gammas as there are exponentiation proofs.",
					Throwables.getRootCause(tooShortGammasException).getMessage());
		}

		@Test
		@DisplayName("different group order throws IllegalArgumentException")
		void differentGroupOrder() {
			final GroupVector<GqElement, GqGroup> otherGammas = otherGqGroupGenerator.genRandomGqElementVector(PSI);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> new PartialDecryptPCCOutput(otherGammas, exponentiationProofs));
			assertEquals("The exponentiated gammas and exponentiation proofs do not have the same group order.",
					Throwables.getRootCause(exception).getMessage());
		}

	}

}
