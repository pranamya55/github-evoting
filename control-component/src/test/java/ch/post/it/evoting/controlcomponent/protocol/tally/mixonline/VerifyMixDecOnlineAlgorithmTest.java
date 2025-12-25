/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.mixonline;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.internal.utils.VerificationSuccess;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.mixnet.Mixnet;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.VerifiableShuffleGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.DecryptionProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptions;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.VerifyMixDecInput;

/**
 * Tests of VerifyMixDecOnlineAlgorithm.
 */
@DisplayName("VerifyMixDecOnlineAlgorithm calling verifyMixDecOnline with")
class VerifyMixDecOnlineAlgorithmTest extends TestGroupSetup {

	private static final int NODE_ID = 2;
	private static final int NUMBER_OF_WRITE_INS_PLUS_ONE = 1;
	private static final int N = 2; //	N = m*n
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static VerifyMixDecOnlineAlgorithm verifyMixDecOnlineAlgorithm;
	private final SetupComponentPublicKeysPayloadGenerator setupComponentPublicKeysPayloadGenerator = new SetupComponentPublicKeysPayloadGenerator(
			gqGroup);
	private final VerifiableShuffleGenerator verifiableShuffleGenerator = new VerifiableShuffleGenerator(gqGroup);
	private VerifyMixDecOnlineContext context;
	private VerifyMixDecInput input;

	@BeforeAll
	static void setUpAll() {
		final Mixnet mixnet = mock(Mixnet.class);
		when(mixnet.verifyShuffle(any(), any(), any(), any())).thenReturn(VerificationSuccess.INSTANCE);

		final ElGamal elGamal = ElGamalFactory.createElGamal();

		final ZeroKnowledgeProof zeroKnowledgeProof = mock(ZeroKnowledgeProof.class);
		when(zeroKnowledgeProof.verifyDecryptions(any(), any(), any(), any())).thenReturn(VerificationSuccess.INSTANCE);

		verifyMixDecOnlineAlgorithm = new VerifyMixDecOnlineAlgorithm(mixnet, elGamal, zeroKnowledgeProof);
	}

	@BeforeEach
	void setUp() {
		final String electionEventId = uuidGenerator.generate();
		final String ballotBoxId = uuidGenerator.generate();
		final SetupComponentPublicKeys setupComponentPublicKeys = setupComponentPublicKeysPayloadGenerator.generate(NUMBER_OF_WRITE_INS_PLUS_ONE,
				NUMBER_OF_WRITE_INS_PLUS_ONE).getSetupComponentPublicKeys();
		final ElGamalMultiRecipientPublicKey electionPublicKey = setupComponentPublicKeys.electionPublicKey();
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys = setupComponentPublicKeys.combinedControlComponentPublicKeys()
				.stream()
				.map(ControlComponentPublicKeys::ccmjElectionPublicKey)
				.collect(GroupVector.toGroupVector());
		final ElGamalMultiRecipientPublicKey electoralBoardPublicKey = setupComponentPublicKeys.electoralBoardPublicKey();
		context = new VerifyMixDecOnlineContext(gqGroup, NODE_ID, electionEventId, ballotBoxId, NUMBER_OF_WRITE_INS_PLUS_ONE,
				electionPublicKey, ccmElectionPublicKeys, electoralBoardPublicKey);

		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> initialCiphertexts = elGamalGenerator.genRandomCiphertextVector(N,
				NUMBER_OF_WRITE_INS_PLUS_ONE);
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> ciphertexts = elGamalGenerator.genRandomCiphertextVector(N,
				NUMBER_OF_WRITE_INS_PLUS_ONE);
		final VerifiableShuffle verifiableShuffle = new VerifiableShuffle(ciphertexts,
				verifiableShuffleGenerator.genVerifiableShuffle(N, NUMBER_OF_WRITE_INS_PLUS_ONE).shuffleArgument());
		final DecryptionProof decryptionProof = new DecryptionProof(zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementVector(NUMBER_OF_WRITE_INS_PLUS_ONE));
		final VerifiableDecryptions verifiableDecryptions = new VerifiableDecryptions(ciphertexts, GroupVector.of(decryptionProof, decryptionProof));

		input = new VerifyMixDecInput(initialCiphertexts, ImmutableList.of(verifiableShuffle), ImmutableList.of(verifiableDecryptions));
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParams() {
		assertDoesNotThrow(() -> verifyMixDecOnlineAlgorithm.verifyMixDecOnline(context, input));
	}

	@Test
	@DisplayName("valid parameters for node 3 does not throw")
	void validParamsNode3() {
		final String electionEventId = uuidGenerator.generate();
		final String ballotBoxId = uuidGenerator.generate();
		final SetupComponentPublicKeys setupComponentPublicKeys = setupComponentPublicKeysPayloadGenerator.generate(NUMBER_OF_WRITE_INS_PLUS_ONE,
				NUMBER_OF_WRITE_INS_PLUS_ONE).getSetupComponentPublicKeys();
		final ElGamalMultiRecipientPublicKey electionPublicKey = setupComponentPublicKeys.electionPublicKey();
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys = setupComponentPublicKeys.combinedControlComponentPublicKeys()
				.stream()
				.map(ControlComponentPublicKeys::ccmjElectionPublicKey)
				.collect(GroupVector.toGroupVector());
		final ElGamalMultiRecipientPublicKey electoralBoardPublicKey = setupComponentPublicKeys.electoralBoardPublicKey();
		final VerifyMixDecOnlineContext verifyMixDecOnlineContext = new VerifyMixDecOnlineContext(gqGroup, 3, electionEventId, ballotBoxId,
				NUMBER_OF_WRITE_INS_PLUS_ONE, electionPublicKey, ccmElectionPublicKeys, electoralBoardPublicKey);

		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> initialCiphertexts = elGamalGenerator.genRandomCiphertextVector(N,
				NUMBER_OF_WRITE_INS_PLUS_ONE);
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> ciphertexts = elGamalGenerator.genRandomCiphertextVector(N,
				NUMBER_OF_WRITE_INS_PLUS_ONE);
		final VerifiableShuffle verifiableShuffle = new VerifiableShuffle(ciphertexts,
				verifiableShuffleGenerator.genVerifiableShuffle(N, NUMBER_OF_WRITE_INS_PLUS_ONE).shuffleArgument());
		final DecryptionProof decryptionProof = new DecryptionProof(zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementVector(NUMBER_OF_WRITE_INS_PLUS_ONE));
		final VerifiableDecryptions verifiableDecryptions = new VerifiableDecryptions(ciphertexts, GroupVector.of(decryptionProof, decryptionProof));
		final VerifyMixDecInput verifyMixDecOnlineInput = new VerifyMixDecInput(initialCiphertexts,
				ImmutableList.of(verifiableShuffle, verifiableShuffle), ImmutableList.of(verifiableDecryptions, verifiableDecryptions));

		final Mixnet mixnet = mock(Mixnet.class);
		when(mixnet.verifyShuffle(any(), any(), any(), any())).thenReturn(VerificationSuccess.INSTANCE);

		final ElGamal elGamal = ElGamalFactory.createElGamal();

		final ZeroKnowledgeProof zeroKnowledgeProof = mock(ZeroKnowledgeProof.class);
		when(zeroKnowledgeProof.verifyDecryptions(any(), any(), any(), any())).thenReturn(VerificationSuccess.INSTANCE);
		final VerifyMixDecOnlineAlgorithm verifyMixDecOnlineAlgorithmNodeId3 = new VerifyMixDecOnlineAlgorithm(mixnet, elGamal,
				zeroKnowledgeProof);

		assertDoesNotThrow(() -> verifyMixDecOnlineAlgorithmNodeId3.verifyMixDecOnline(verifyMixDecOnlineContext, verifyMixDecOnlineInput));
	}

	@Test
	@DisplayName("any null parameter throws NullPointerException")
	void nullParamsThrows() {
		assertThrows(NullPointerException.class, () -> verifyMixDecOnlineAlgorithm.verifyMixDecOnline(null, input));
		assertThrows(NullPointerException.class, () -> verifyMixDecOnlineAlgorithm.verifyMixDecOnline(context, null));
	}

	@Test
	@DisplayName("not enough votes throws IllegalArgumentException")
	void notEnoughVotesThrows() {
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> initialCiphertexts = elGamalGenerator.genRandomCiphertextVector(1,
				NUMBER_OF_WRITE_INS_PLUS_ONE);

		final VerifyMixDecInput notEnoughVotesInput = spy(input);
		doReturn(initialCiphertexts).when(notEnoughVotesInput).initialCiphertexts();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> verifyMixDecOnlineAlgorithm.verifyMixDecOnline(context, notEnoughVotesInput));
		assertEquals("There must be at least two votes.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("ciphertext size not matching number of write ins throws IllegalArgumentException")
	void ciphertextSizeNotMatchingWriteInsThrows() {
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> notMatchingCiphertextSize = elGamalGenerator.genRandomCiphertextVector(N, 2);

		final VerifyMixDecInput notMatchingCiphertextSizeInput = spy(input);
		doReturn(notMatchingCiphertextSize).when(notMatchingCiphertextSizeInput).initialCiphertexts();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> verifyMixDecOnlineAlgorithm.verifyMixDecOnline(context, notMatchingCiphertextSizeInput));
		final String errorMessage = String.format(
				"The ciphertexts size must be the number of allowed write-ins + 1. [l: %s, delta: %s]", 2,
				NUMBER_OF_WRITE_INS_PLUS_ONE);
		assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("different group context and input throws IllegalArgumentException")
	void differentGroupThrows() {
		final VerifyMixDecOnlineContext otherGroupContext = spy(context);
		doReturn(otherGqGroup).when(otherGroupContext).encryptionGroup();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> verifyMixDecOnlineAlgorithm.verifyMixDecOnline(otherGroupContext, input));
		assertEquals("The context and input must have the same encryption group.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("wrong number of verifiable shuffles throws IllegalArgumentException")
	void wrongNumberOfVerifiableShufflesThrows() {
		final VerifiableDecryptions verifiableDecryptions = input.precedingVerifiableDecryptedVotes().get(0);
		final VerifiableShuffle verifiableShuffle = input.precedingVerifiableShuffledVotes().get(0);
		final VerifyMixDecInput wrongShufflesInput = new VerifyMixDecInput(
				input.initialCiphertexts(),
				ImmutableList.of(verifiableShuffle, verifiableShuffle),
				ImmutableList.of(verifiableDecryptions, verifiableDecryptions)
		);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> verifyMixDecOnlineAlgorithm.verifyMixDecOnline(context, wrongShufflesInput));
		final String errorMessage = String.format("Wrong number of verifiable shuffles. [expected: %s, actual: %s]", NODE_ID - 1, 2);
		assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
	}

}
