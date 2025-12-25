/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setuptally;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashElectionEventContextAlgorithm;

/**
 * Tests of SetupTallyCCMAlgorithm.
 */
@DisplayName("A SetupTallyCCMAlgorithm")
class SetupTallyCCMAlgorithmTest {

	private static final int NODE_ID = 1;
	private static int deltaMax;
	private static SetupTallyCCMContext context;
	private static SetupTallyCCMAlgorithm setupTallyCCMAlgorithm;

	@BeforeAll
	static void setUpAll() {
		final Random random = RandomFactory.createRandom();
		final ZeroKnowledgeProof zeroKnowledgeProof = ZeroKnowledgeProofFactory.createZeroKnowledgeProof();
		final GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm = new GetHashElectionEventContextAlgorithm(
				BaseEncodingFactory.createBase64(), HashFactory.createHash());
		setupTallyCCMAlgorithm = new SetupTallyCCMAlgorithm(random, zeroKnowledgeProof, getHashElectionEventContextAlgorithm);
	}

	@BeforeEach
	void setup() {
		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		final ElectionEventContext electionEventContext = electionEventContextPayloadGenerator.generate().getElectionEventContext();
		deltaMax = electionEventContext.maximumNumberOfWriteInsPlusOne();

		context = new SetupTallyCCMContext(NODE_ID, electionEventContext);
	}

	@Test
	@DisplayName("with a valid parameter does not throw any Exception.")
	void validParamDoesNotThrow() {
		assertDoesNotThrow(() -> setupTallyCCMAlgorithm.setupTallyCCM(context));
	}

	@Test
	@DisplayName("with a null parameter throws a NullPointerException.")
	void nullParamThrowsANullPointer() {
		assertThrows(NullPointerException.class, () -> setupTallyCCMAlgorithm.setupTallyCCM(null));
	}

	@Test
	@DisplayName("with a valid parameter returns a non-null keypair with expected size.")
	void nonNullOutput() {
		final SetupTallyCCMOutput setupTallyCCMOutput = setupTallyCCMAlgorithm.setupTallyCCM(context);

		assertNotNull(setupTallyCCMOutput);
		assertEquals(deltaMax, setupTallyCCMOutput.getCcmjElectionKeyPair().getPublicKey().size());
		assertEquals(deltaMax, setupTallyCCMOutput.getCcmjElectionKeyPair().getPrivateKey().size());
	}
}
