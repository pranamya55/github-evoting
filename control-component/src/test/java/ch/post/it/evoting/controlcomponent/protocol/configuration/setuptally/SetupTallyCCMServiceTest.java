/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setuptally;

import static ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory.createZeroKnowledgeProof;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashElectionEventContextAlgorithm;

@DisplayName("setupTallyCCM called with")
class SetupTallyCCMServiceTest {

	private static final Random random = RandomFactory.createRandom();

	private static SetupTallyCCMService setupTallyCCMService;
	private static GqGroup encryptionGroup;
	private static ElectionEventContext electionEventContext;

	@BeforeAll
	static void setUpAll() {
		final GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm = new GetHashElectionEventContextAlgorithm(
				BaseEncodingFactory.createBase64(), HashFactory.createHash());
		final SetupTallyCCMAlgorithm setupTallyCCMAlgorithm = new SetupTallyCCMAlgorithm(random, createZeroKnowledgeProof(),
				getHashElectionEventContextAlgorithm);
		setupTallyCCMService = new SetupTallyCCMService(ControlComponentNode.first().id(), setupTallyCCMAlgorithm);

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		electionEventContext = electionEventContextPayloadGenerator.generate().getElectionEventContext();
		encryptionGroup = electionEventContext.encryptionGroup();
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, electionEventContext),
				Arguments.of(encryptionGroup, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void setupTallyCCMWithNullParametersThrows(final GqGroup encryptionGroup, final ElectionEventContext electionEventContext) {
		assertThrows(NullPointerException.class,
				() -> setupTallyCCMService.setupTallyCCM(encryptionGroup, electionEventContext));
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void setupTallyCCMWithValidParametersDoesNotThrow() {
		assertDoesNotThrow(() -> setupTallyCCMService.setupTallyCCM(encryptionGroup, electionEventContext));
	}
}
