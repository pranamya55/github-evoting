/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.configuration.upload;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.controlcomponent.process.CcmjElectionKeysService;
import ch.post.it.evoting.controlcomponent.process.CcrjReturnCodesKeysService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.protocol.configuration.setuptally.SetupTallyCCMOutput;
import ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting.GenKeysCCROutput;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyKeyGenerationSchnorrProofsAlgorithm;

@DisplayName("verifySetupComponentPublicKeys with")
class VerifySetupComponentPublicKeysServiceTest {

	private static final int NODE_ID = ControlComponentNode.ONE.id();

	private static String electionEventId;
	private static GqGroup encryptionGroup;
	private static SetupComponentPublicKeys setupComponentPublicKeys;
	private static GenKeysCCROutput genKeysCCROutput;
	private static SetupTallyCCMOutput setupTallyCCMOutput;

	private ElectionEventService electionEventService;
	private ElectionEventContextService electionEventContextService;
	private CcrjReturnCodesKeysService ccrjReturnCodesKeysService;
	private CcmjElectionKeysService ccmjElectionKeysService;
	private VerifyKeyGenerationSchnorrProofsAlgorithm verifyKeyGenerationSchnorrProofsAlgorithm;
	private VerifySetupComponentPublicKeysService verifySetupComponentPublicKeysService;

	@BeforeAll
	static void setupAll() {
		// Generate ElectionEventContext.
		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();

		// Generate CCRj and CCMj key pairs.
		final Random random = RandomFactory.createRandom();
		final int maximumNumberOfSelections = 10;
		final int maximumNumberOfWriteInsPlusOne = 3;
		final ImmutableList<ElGamalMultiRecipientKeyPair> ccrjElectionKeyPairs = ControlComponentNode.ids().stream()
				.map(nodeId -> ElGamalMultiRecipientKeyPair.genKeyPair(encryptionGroup, maximumNumberOfSelections, random))
				.collect(toImmutableList());
		final ImmutableList<ElGamalMultiRecipientKeyPair> ccmjElectionKeyPairs = ControlComponentNode.ids().stream()
				.map(nodeId -> ElGamalMultiRecipientKeyPair.genKeyPair(encryptionGroup, maximumNumberOfWriteInsPlusOne, random))
				.collect(toImmutableList());

		// Generate SetupComponentPublicKeys.
		final SetupComponentPublicKeysPayloadGenerator setupComponentPublicKeysPayloadGenerator = new SetupComponentPublicKeysPayloadGenerator(
				encryptionGroup);
		final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload = setupComponentPublicKeysPayloadGenerator.generate(
				ccrjElectionKeyPairs.stream()
						.map(ElGamalMultiRecipientKeyPair::getPublicKey)
						.collect(toGroupVector()),
				ccmjElectionKeyPairs.stream()
						.map(ElGamalMultiRecipientKeyPair::getPublicKey)
						.collect(toGroupVector())
		);
		setupComponentPublicKeys = setupComponentPublicKeysPayload.getSetupComponentPublicKeys();

		// Generate CCRj and CCMj Schnorr proofs.
		final GroupVector<SchnorrProof, ZqGroup> ccrjSchnorrProofs = setupComponentPublicKeys.combinedControlComponentPublicKeys().stream()
				.map(ControlComponentPublicKeys::ccrjSchnorrProofs)
				.collect(toGroupVector())
				.get(NODE_ID - 1);
		genKeysCCROutput = new GenKeysCCROutput(ccrjElectionKeyPairs.getFirst(),
				ccrjElectionKeyPairs.getFirst().getPrivateKey().get(0),
				ccrjSchnorrProofs);

		final GroupVector<SchnorrProof, ZqGroup> ccmjSchnorrProofs = setupComponentPublicKeys.combinedControlComponentPublicKeys().stream()
				.map(ControlComponentPublicKeys::ccmjSchnorrProofs)
				.collect(toGroupVector())
				.get(NODE_ID - 1);
		setupTallyCCMOutput = new SetupTallyCCMOutput.Builder()
				.setElGamalMultiRecipientKeyPair(ccmjElectionKeyPairs.getFirst())
				.setSchnorrProofs(ccmjSchnorrProofs)
				.build();
	}

	@BeforeEach
	void setUp() {
		electionEventService = mock(ElectionEventService.class);
		electionEventContextService = mock(ElectionEventContextService.class);
		ccrjReturnCodesKeysService = mock(CcrjReturnCodesKeysService.class);
		ccmjElectionKeysService = mock(CcmjElectionKeysService.class);
		verifyKeyGenerationSchnorrProofsAlgorithm = mock(VerifyKeyGenerationSchnorrProofsAlgorithm.class);
		verifySetupComponentPublicKeysService = new VerifySetupComponentPublicKeysService(NODE_ID, electionEventService, electionEventContextService,
				ccmjElectionKeysService, ccrjReturnCodesKeysService, verifyKeyGenerationSchnorrProofsAlgorithm);

		when(electionEventService.getEncryptionGroup(electionEventId)).thenReturn(encryptionGroup);
		when(electionEventContextService.getElectionEventFinishTime(electionEventId)).thenReturn(LocalDateTimeUtils.now().plusDays(2));

		when(ccrjReturnCodesKeysService.getCcrjChoiceReturnCodesEncryptionKeyPair(electionEventId))
				.thenReturn(genKeysCCROutput.ccrjChoiceReturnCodesEncryptionKeyPair());
		when(ccrjReturnCodesKeysService.getCcrjSchnorrProofs(electionEventId)).thenReturn(genKeysCCROutput.ccrjSchnorrProofs());

		when(ccmjElectionKeysService.getCcmjElectionKeyPair(electionEventId)).thenReturn(setupTallyCCMOutput.getCcmjElectionKeyPair());
		when(ccmjElectionKeysService.getCcmjSchnorrProofs(electionEventId)).thenReturn(setupTallyCCMOutput.getSchnorrProofs());
	}

	@Test
	@DisplayName("null arguments throws a NullPointerException.")
	void nullArgumentsThrows() {
		assertThrows(NullPointerException.class,
				() -> verifySetupComponentPublicKeysService.verifySetupComponentPublicKeys(null, setupComponentPublicKeys));
		assertThrows(NullPointerException.class, () -> verifySetupComponentPublicKeysService.verifySetupComponentPublicKeys(electionEventId, null));
	}

	@Test
	@DisplayName("invalid encryption group throws IllegalStateException.")
	void invalidEncryptionGroupThrows() {
		when(electionEventService.getEncryptionGroup(any())).thenReturn(GroupTestData.getGroupP59());

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> verifySetupComponentPublicKeysService.verifySetupComponentPublicKeys(electionEventId, setupComponentPublicKeys));

		final String expected = String.format(
				"The Setup Component encryption group does not match the saved encryption group. [electionEventId: %s, nodeId: %s]", electionEventId,
				NODE_ID);
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("invalid finish time throws IllegalStateException.")
	void invalidFinishTimeThrows() {
		when(electionEventContextService.getElectionEventFinishTime(any())).thenReturn(LocalDateTimeUtils.now().minusDays(1));

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> verifySetupComponentPublicKeysService.verifySetupComponentPublicKeys(electionEventId, setupComponentPublicKeys));

		final String expected = String.format("The election event is over. [electionEventId: %s, nodeId: %s]", electionEventId, NODE_ID);
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("invalid CCRj election public key throws IllegalStateException.")
	void invalidCcrjElectionPublicKeyThrows() {
		when(ccrjReturnCodesKeysService.getCcrjChoiceReturnCodesEncryptionKeyPair(any())).thenReturn(setupTallyCCMOutput.getCcmjElectionKeyPair());

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> verifySetupComponentPublicKeysService.verifySetupComponentPublicKeys(electionEventId, setupComponentPublicKeys));

		final String expected = String.format(
				"The Setup Component CCRj Return Codes encryption public key does not match the saved CCRj Choice Return Codes encryption public key. [electionEventId: %s, nodeId: %s]",
				electionEventId, NODE_ID);
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("invalid CCRj Schnorr proofs throws IllegalStateException.")
	void invalidCcrjSchnorrProofsThrows() {
		when(ccrjReturnCodesKeysService.getCcrjSchnorrProofs(any())).thenReturn(setupTallyCCMOutput.getSchnorrProofs());

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> verifySetupComponentPublicKeysService.verifySetupComponentPublicKeys(electionEventId, setupComponentPublicKeys));

		final String expected = String.format(
				"The Setup Component CCRj Return Codes Schnorr proofs do not match the saved CCRj Schnorr proofs. [electionEventId: %s, nodeId: %s]",
				electionEventId, NODE_ID);
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("invalid CCMj election public key throws IllegalStateException.")
	void invalidCcmjElectionPublicKeyThrows() {
		when(ccmjElectionKeysService.getCcmjElectionKeyPair(any())).thenReturn(genKeysCCROutput.ccrjChoiceReturnCodesEncryptionKeyPair());

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> verifySetupComponentPublicKeysService.verifySetupComponentPublicKeys(electionEventId, setupComponentPublicKeys));

		final String expected = String.format(
				"The Setup Component CCMj election public key does not match the saved CCMj election public key. [electionEventId: %s, nodeId: %s]",
				electionEventId, NODE_ID);
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("invalid CCMj Schnorr proofs throws IllegalStateException.")
	void invalidCcmjSchnorrProofsThrows() {
		when(ccmjElectionKeysService.getCcmjSchnorrProofs(any())).thenReturn(genKeysCCROutput.ccrjSchnorrProofs());

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> verifySetupComponentPublicKeysService.verifySetupComponentPublicKeys(electionEventId, setupComponentPublicKeys));

		final String expected = String.format(
				"The Setup Component CCMj Schnorr proofs do not match the saved CCMj Schnorr proofs. [electionEventId: %s, nodeId: %s]",
				electionEventId, NODE_ID);
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("invalid key generation Schnorr proofs throws IllegalStateException.")
	void invalidKeyGenerationSchnorrProofsThrows() {
		when(verifyKeyGenerationSchnorrProofsAlgorithm.verifyKeyGenerationSchnorrProofs(any(), any())).thenReturn(false);

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> verifySetupComponentPublicKeysService.verifySetupComponentPublicKeys(electionEventId, setupComponentPublicKeys));

		final String expected = String.format("The Schnorr proofs are invalid. [electionEventId: %s, nodeId: %s]", electionEventId, NODE_ID);
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("valid arguments does not throw.")
	void validArgumentsDoesNotThrow() {
		when(verifyKeyGenerationSchnorrProofsAlgorithm.verifyKeyGenerationSchnorrProofs(any(), any())).thenReturn(true);

		assertDoesNotThrow(() -> verifySetupComponentPublicKeysService.verifySetupComponentPublicKeys(electionEventId, setupComponentPublicKeys));
	}
}
