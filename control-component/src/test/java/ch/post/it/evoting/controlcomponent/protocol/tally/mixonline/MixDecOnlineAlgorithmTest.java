/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.mixonline;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;
import com.google.common.collect.MoreCollectors;

import ch.post.it.evoting.controlcomponent.process.BallotBoxService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.mixnet.MixnetFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentVotesHashPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ControlComponentVotesHashPayloadGenerator;

/**
 * Tests of MixDecOnlineAlgorithm.
 */
@DisplayName("mixDecOnline with")
class MixDecOnlineAlgorithmTest {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final GqGroup GQ_GROUP = GroupTestData.getLargeGqGroup();

	private BallotBoxService ballotBoxService;
	private ElGamalGenerator elGamalGenerator;
	private int n;
	private int l;
	private int deltaMax;
	private int deltaSup;
	private int nodeId;
	private String encryptedConfirmedVotesHash;
	private ImmutableList<String> encryptedConfirmedVotesHashes;
	private ElGamalMultiRecipientPrivateKey ccmjElectionSecretKey;
	private MixDecOnlineAlgorithm mixDecOnlineAlgorithm;
	private MixDecOnlineContext mixDecOnlineContext;
	private MixDecOnlineInput mixDecOnlineInput;

	@BeforeEach
	void setup() {
		elGamalGenerator = new ElGamalGenerator(GQ_GROUP);

		n = SECURE_RANDOM.nextInt(4) + 2;
		l = SECURE_RANDOM.nextInt(5) + 1;
		deltaMax = l + SECURE_RANDOM.nextInt(5);
		deltaSup = MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1;
		nodeId = SECURE_RANDOM.nextInt(ControlComponentNode.ids().size()) + 1;

		ballotBoxService = mock(BallotBoxService.class);
		mixDecOnlineAlgorithm = new MixDecOnlineAlgorithm(ballotBoxService, ElGamalFactory.createElGamal(), MixnetFactory.createMixnet(),
				ZeroKnowledgeProofFactory.createZeroKnowledgeProof());

		ccmjElectionSecretKey = elGamalGenerator.genRandomPrivateKey(deltaMax);
		mixDecOnlineContext = generateMixDecryptContext(l);

		final ControlComponentVotesHashPayloadGenerator controlComponentVotesHashPayloadGenerator = new ControlComponentVotesHashPayloadGenerator();
		final ImmutableList<ControlComponentVotesHashPayload> controlComponentVotesHashPayloads = controlComponentVotesHashPayloadGenerator.generate(
				mixDecOnlineContext.getElectionEventId(), mixDecOnlineContext.getBallotBoxId());
		encryptedConfirmedVotesHash = controlComponentVotesHashPayloads.stream()
				.filter(payload -> payload.getNodeId() == nodeId)
				.map(ControlComponentVotesHashPayload::getEncryptedConfirmedVotesHash)
				.collect(MoreCollectors.onlyElement());
		encryptedConfirmedVotesHashes = controlComponentVotesHashPayloads.stream()
				.map(ControlComponentVotesHashPayload::getEncryptedConfirmedVotesHash)
				.collect(toImmutableList());
		mixDecOnlineInput = generateMixDecryptInput(n, l, encryptedConfirmedVotesHash, encryptedConfirmedVotesHashes);
	}

	@Test
	@DisplayName("null arguments throws a NullPointerException")
	void mixDecOnlineWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> mixDecOnlineAlgorithm.mixDecOnline(null, mixDecOnlineInput));
		assertThrows(NullPointerException.class, () -> mixDecOnlineAlgorithm.mixDecOnline(mixDecOnlineContext, null));
	}

	@Test
	@DisplayName("context with different group than input throws an IllegalArgumentException")
	void mixDecOnlineWithDifferentGroupThrows() {
		final GqGroup otherGroup = GroupTestData.getDifferentGqGroup(GQ_GROUP);
		final ElGamalGenerator otherElGamalGenerator = new ElGamalGenerator(otherGroup);
		final List<ElGamalMultiRecipientPublicKey> ccmElectionPublicKeys = Stream.generate(
						() -> otherElGamalGenerator.genRandomPublicKey(deltaMax))
				.limit(ControlComponentNode.ids().size())
				.collect(Collectors.toList());
		ccmElectionPublicKeys.set(nodeId - 1,
				ElGamalMultiRecipientKeyPair.from(otherElGamalGenerator.genRandomPrivateKey(deltaMax), otherGroup.getGenerator()).getPublicKey());
		final ElGamalMultiRecipientPublicKey electoralBoardPublicKey = otherElGamalGenerator.genRandomPublicKey(deltaMax);
		final MixDecOnlineContext otherGroupContext = new MixDecOnlineContext.Builder()
				.setEncryptionGroup(otherGroup)
				.setNodeId(nodeId)
				.setElectionEventId(mixDecOnlineContext.getElectionEventId())
				.setBallotBoxId(mixDecOnlineContext.getBallotBoxId())
				.setNumberOfAllowedWriteInsPlusOne(mixDecOnlineContext.getNumberOfAllowedWriteInsPlusOne())
				.setCcmElectionPublicKeys(GroupVector.from(ccmElectionPublicKeys))
				.setElectoralBoardPublicKey(electoralBoardPublicKey)
				.build();
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> mixDecOnlineAlgorithm.mixDecOnline(otherGroupContext, mixDecOnlineInput));
		assertEquals("The context and input must have the same encryption group.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("less than 2 partially decrypted votes throws an IllegalArgumentException")
	void mixDecOnlineWithLessThanTwoPartiallyEncryptedVotesThrows() {
		final MixDecOnlineInput badMixDecOnlineInput = generateMixDecryptInput(1, l, encryptedConfirmedVotesHash,
				encryptedConfirmedVotesHashes);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> mixDecOnlineAlgorithm.mixDecOnline(mixDecOnlineContext, badMixDecOnlineInput));
		assertEquals(String.format("There must be at least 2 partially decrypted votes. [N_c_hat: %s]",
				badMixDecOnlineInput.getPartiallyDecryptedVotes().size()), Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("the partially decrypted votes having an element size different from delta throws an IllegalArgumentException")
	void mixDecryptOnlineWithPartiallyDecryptedVotesBadPhiSizeThrows() {
		final MixDecOnlineInput badMixDecOnlineInput = generateMixDecryptInput(n, l + 1, encryptedConfirmedVotesHash,
				encryptedConfirmedVotesHashes);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> mixDecOnlineAlgorithm.mixDecOnline(mixDecOnlineContext, badMixDecOnlineInput));
		assertEquals(String.format(
						"The number of element of each partially decrypted vote must be the allowed number of write-ins + 1. [c_dec_j_minus_one_numberOfElements: %s, delta: %s]",
						badMixDecOnlineInput.getPartiallyDecryptedVotes().getElementSize(), mixDecOnlineContext.getNumberOfAllowedWriteInsPlusOne()),
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("delta_sup smaller than delta_max throws an IllegalArgumentException")
	void mixDecryptOnlineWithDeltaSupSmallerThanDeltaThrows() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> generateMixDecryptContext(deltaSup + 1));
		assertEquals(String.format(
						"The number of write-ins + 1 must be smaller or equal to the maximum supported number of write-ins + 1. [delta: %s, delta_sup: %s]",
						deltaSup + 1, deltaSup),
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("CCMj public key not corresponding to CCMj secret key throws an IllegalArgumentException")
	void mixDecOnlineWithBadCcmjKeyPairThrows() {
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys = Stream.generate(
						() -> elGamalGenerator.genRandomPublicKey(deltaMax))
				.limit(4)
				.collect(GroupVector.toGroupVector());
		final ElGamalMultiRecipientPublicKey electoralBoardPublicKey = elGamalGenerator.genRandomPublicKey(deltaMax);

		final MixDecOnlineContext badMixDecOnlineContext = new MixDecOnlineContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setNodeId(nodeId)
				.setElectionEventId(mixDecOnlineContext.getElectionEventId())
				.setBallotBoxId(mixDecOnlineContext.getBallotBoxId())
				.setNumberOfAllowedWriteInsPlusOne(mixDecOnlineContext.getNumberOfAllowedWriteInsPlusOne())
				.setCcmElectionPublicKeys(ccmElectionPublicKeys)
				.setElectoralBoardPublicKey(electoralBoardPublicKey)
				.build();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> mixDecOnlineAlgorithm.mixDecOnline(badMixDecOnlineContext, mixDecOnlineInput));
		assertEquals("The public key of the reconstituted CCM_j election public key pair does not correspond to the given CCM_j election public key.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("the CCM election public keys having a different group order than the CCMj election secret key throws an IllegalArgumentException")
	void constructWithCcmElectionPublicKeysDifferentGroupOrderThanCcmjElectionSecretKeyThrows() {
		final ElGamalGenerator otherGroupElGamalGenerator = new ElGamalGenerator(GroupTestData.getGroupP59());
		final ElGamalMultiRecipientPrivateKey differentCcmjElectionPrivateKey = otherGroupElGamalGenerator.genRandomPrivateKey(
				ccmjElectionSecretKey.size());
		final MixDecOnlineInput.Builder builderWithDifferentCcmjElectionPrivateKey = new MixDecOnlineInput.Builder()
				.setPartiallyDecryptedVotes(mixDecOnlineInput.getPartiallyDecryptedVotes())
				.setCcmjElectionSecretKey(differentCcmjElectionPrivateKey)
				.setEncryptedConfirmedVotesHash(encryptedConfirmedVotesHash)
				.setEncryptedConfirmedVotesHashes(encryptedConfirmedVotesHashes);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builderWithDifferentCcmjElectionPrivateKey::build);
		assertEquals("The partially decrypted votes must have the same group order as the CCM_j election secret key.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid input does not throw")
	void mixDecryptOnlineWithValidInput() {
		final String ballotBoxId = mixDecOnlineContext.getBallotBoxId();
		when(ballotBoxService.isMixed(ballotBoxId)).thenReturn(false, true);
		mixDecOnlineAlgorithm.mixDecOnline(mixDecOnlineContext, mixDecOnlineInput);
		verify(ballotBoxService).setMixed(ballotBoxId);
		assertTrue(ballotBoxService.isMixed(ballotBoxId));
	}

	private MixDecOnlineContext generateMixDecryptContext(final int numberAllowedWriteInsPlusOne) {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		final String ballotBoxId = uuidGenerator.generate();

		final List<ElGamalMultiRecipientPublicKey> ccmElectionPublicKeys = Stream.generate(
						() -> elGamalGenerator.genRandomPublicKey(deltaMax))
				.limit(ControlComponentNode.ids().size())
				.collect(Collectors.toList());
		ccmElectionPublicKeys.set(nodeId - 1, ElGamalMultiRecipientKeyPair.from(ccmjElectionSecretKey, GQ_GROUP.getGenerator()).getPublicKey());
		final ElGamalMultiRecipientPublicKey electoralBoardPublicKey = elGamalGenerator.genRandomPublicKey(deltaMax);
		return new MixDecOnlineContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setNodeId(nodeId)
				.setElectionEventId(electionEventId)
				.setBallotBoxId(ballotBoxId)
				.setNumberOfAllowedWriteInsPlusOne(numberAllowedWriteInsPlusOne)
				.setCcmElectionPublicKeys(GroupVector.from(ccmElectionPublicKeys))
				.setElectoralBoardPublicKey(electoralBoardPublicKey)
				.build();
	}

	/**
	 * Generates a MixDecryptInput object with a given number of partially decrypted votes.
	 *
	 * @param n the desired number of partially decrypted votes.
	 * @return a {@link MixDecOnlineInput}
	 */
	private MixDecOnlineInput generateMixDecryptInput(final int n, final int l, final String hvc_j, final ImmutableList<String> hvc) {
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> partiallyDecryptedVotes = elGamalGenerator.genRandomCiphertextVector(n, l);

		return new MixDecOnlineInput.Builder()
				.setPartiallyDecryptedVotes(partiallyDecryptedVotes)
				.setCcmjElectionSecretKey(ccmjElectionSecretKey)
				.setEncryptedConfirmedVotesHash(hvc_j)
				.setEncryptedConfirmedVotesHashes(hvc)
				.build();
	}
}
