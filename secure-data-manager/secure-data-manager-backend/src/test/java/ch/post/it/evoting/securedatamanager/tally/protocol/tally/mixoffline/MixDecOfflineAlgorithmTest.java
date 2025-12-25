/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.mixnet.MixnetFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;

/**
 * Tests of MixDecOfflineAlgorithm.
 */
@DisplayName("MixDecOfflineAlgorithm calling mixDecOffline with")
class MixDecOfflineAlgorithmTest {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final GqGroup GQ_GROUP = GroupTestData.getLargeGqGroup();

	private static MixDecOfflineAlgorithm mixDecOfflineAlgorithm;
	private static BallotBoxService ballotBoxService;

	private String electionEventId;
	private String ballotBoxId;
	private int numberOfAllowedWriteInsPlusOne;
	private GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> partiallyDecryptedVotes;
	private ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords;
	private MixDecOfflineContext mixDecOfflineContext;
	private MixDecOfflineInput mixDecOfflineInput;

	@BeforeAll
	static void setupAll() {
		ballotBoxService = mock(BallotBoxService.class);

		mixDecOfflineAlgorithm = new MixDecOfflineAlgorithm(HashFactory.createHash(), MixnetFactory.createMixnet(), ballotBoxService,
				ZeroKnowledgeProofFactory.createZeroKnowledgeProof()
		);
	}

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		ballotBoxId = uuidGenerator.generate();

		numberOfAllowedWriteInsPlusOne = RANDOM.nextInt(5) + 1;
		mixDecOfflineContext = new MixDecOfflineContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setElectionEventId(electionEventId)
				.setBallotBoxId(ballotBoxId)
				.setNumberOfAllowedWriteInsPlusOne(numberOfAllowedWriteInsPlusOne)
				.build();

		final int n = RANDOM.nextInt(4) + 2;
		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(GQ_GROUP);
		partiallyDecryptedVotes = elGamalGenerator.genRandomCiphertextVector(n, numberOfAllowedWriteInsPlusOne);
		electoralBoardMembersPasswords = ImmutableList.of(new SafePasswordHolder("Password_ElectoralBoard1_1".toCharArray()),
				new SafePasswordHolder("Password_ElectoralBoard2_2".toCharArray()));
		mixDecOfflineInput = new MixDecOfflineInput(partiallyDecryptedVotes, electoralBoardMembersPasswords);
	}

	@Test
	@DisplayName("null arguments throws a NullPointerException")
	void mixDecOfflineWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class,
				() -> mixDecOfflineAlgorithm.mixDecOffline(null, mixDecOfflineInput));
		assertThrows(NullPointerException.class,
				() -> mixDecOfflineAlgorithm.mixDecOffline(mixDecOfflineContext, null));
	}

	@Test
	@DisplayName("context and input having different groups throws an IllegalArgumentException")
	void mixDecOfflineWithDifferentGroupsThrows() {
		final MixDecOfflineContext contextWithDifferentGroup = new MixDecOfflineContext.Builder()
				.setEncryptionGroup(GroupTestData.getDifferentGqGroup(GQ_GROUP))
				.setElectionEventId(electionEventId)
				.setBallotBoxId(ballotBoxId)
				.setNumberOfAllowedWriteInsPlusOne(numberOfAllowedWriteInsPlusOne)
				.build();
		assertThrows(IllegalArgumentException.class, () -> mixDecOfflineAlgorithm.mixDecOffline(contextWithDifferentGroup, mixDecOfflineInput));
	}

	@Test
	@DisplayName("too few partially decrypted votes throws an IllegalArgumentException")
	void mixDecOfflineWithTooFewPartiallyDecryptedVotesThrows() {
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> tooFewPartiallyDecryptedVotes = new ElGamalGenerator(
				GQ_GROUP).genRandomCiphertextVector(1,
				partiallyDecryptedVotes.getElementSize());
		final MixDecOfflineInput tooSmallMixDecOfflineInput = new MixDecOfflineInput(tooFewPartiallyDecryptedVotes, electoralBoardMembersPasswords);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> mixDecOfflineAlgorithm.mixDecOffline(mixDecOfflineContext, tooSmallMixDecOfflineInput));
		assertEquals(String.format("There must be at least 2 partially decrypted votes. [N_c_hat: %s]", tooFewPartiallyDecryptedVotes.size()),
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("number of allowed write-ins + 1 different from number of elements of the partially decrypted votes throws an IllegalArgumentException")
	void mixDecOfflineWithPartiallyDecryptedVotesIncorrectNumberOfElementsThrows() {
		final int biggerNumberOfAllowedWriteInsPlusOne = numberOfAllowedWriteInsPlusOne + 1;
		final MixDecOfflineContext tooBigMixDecOfflineContext = new MixDecOfflineContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setElectionEventId(electionEventId)
				.setBallotBoxId(ballotBoxId)
				.setNumberOfAllowedWriteInsPlusOne(biggerNumberOfAllowedWriteInsPlusOne)
				.build();
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> mixDecOfflineAlgorithm.mixDecOffline(tooBigMixDecOfflineContext, mixDecOfflineInput));
		assertEquals(String.format(
				"The number of elements in the partially decrypted votes must correspond to the number of allowed write-ins + 1. [l: %s, delta: %s]",
				partiallyDecryptedVotes.getElementSize(), biggerNumberOfAllowedWriteInsPlusOne), Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("on already decrypted ballot box throws IllegalArgumentException")
	void alreadyDecryptedThrows() {
		when(ballotBoxService.isDecrypted(ballotBoxId)).thenReturn(true);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> mixDecOfflineAlgorithm.mixDecOffline(mixDecOfflineContext, mixDecOfflineInput));

		final String errorMessage = String.format("The ballot box has already been decrypted. [ballotBoxId: %s]", ballotBoxId);
		assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid inputs does not throw")
	void mixDecOfflineWithValidParametersDoesNotThrow() {
		final MixDecOfflineOutput mixDecOfflineOutput = mixDecOfflineAlgorithm.mixDecOffline(mixDecOfflineContext, mixDecOfflineInput);

		assertEquals(numberOfAllowedWriteInsPlusOne, mixDecOfflineOutput.getVerifiableShuffle().shuffledCiphertexts().getElementSize());
		assertEquals(partiallyDecryptedVotes.size(), mixDecOfflineOutput.getVerifiableShuffle().shuffledCiphertexts().size());
		assertEquals(numberOfAllowedWriteInsPlusOne, mixDecOfflineOutput.getVerifiablePlaintextDecryption().getDecryptedVotes().getElementSize());
		assertEquals(partiallyDecryptedVotes.size(), mixDecOfflineOutput.getVerifiablePlaintextDecryption().getDecryptedVotes().size());
	}
}
