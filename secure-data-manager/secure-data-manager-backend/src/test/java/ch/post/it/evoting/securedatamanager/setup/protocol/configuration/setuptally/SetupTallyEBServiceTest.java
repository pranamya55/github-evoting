/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setuptally;

import static ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory.createElGamal;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory.createZeroKnowledgeProof;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashElectionEventContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyCCSchnorrProofsAlgorithm;

@DisplayName("setupTallyEB called with")
class SetupTallyEBServiceTest {

	private static SetupTallyEBService setupTallyEBService;
	private static ElectionEventContextPayload electionEventContextPayload;
	private static ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords;
	private static ImmutableList<ControlComponentPublicKeys> controlComponentPublicKeys;

	@BeforeAll
	static void setUpAll() {
		final ZeroKnowledgeProof zeroKnowledgeProof = spy(createZeroKnowledgeProof());
		final VerifyCCSchnorrProofsAlgorithm verifyCCSchnorrProofsAlgorithm = new VerifyCCSchnorrProofsAlgorithm(zeroKnowledgeProof);
		final Hash hash = createHash();
		final GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm = new GetHashElectionEventContextAlgorithm(
				BaseEncodingFactory.createBase64(), hash);
		final SetupTallyEBAlgorithm setupTallyEBAlgorithm = new SetupTallyEBAlgorithm(hash, createElGamal(), zeroKnowledgeProof,
				verifyCCSchnorrProofsAlgorithm, getHashElectionEventContextAlgorithm);
		setupTallyEBService = new SetupTallyEBService(setupTallyEBAlgorithm);

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		final ElectionEventContext electionEventContext = electionEventContextPayload.getElectionEventContext();
		electoralBoardMembersPasswords = ImmutableList.of(new SafePasswordHolder("Password_ElectoralBoard1".toCharArray()),
				new SafePasswordHolder("Password_ElectoralBoard2".toCharArray()));

		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload = new SetupComponentPublicKeysPayloadGenerator(encryptionGroup)
				.generate(electionEventContext.maximumNumberOfSelections(), electionEventContext.maximumNumberOfWriteInsPlusOne());
		controlComponentPublicKeys = setupComponentPublicKeysPayload.getSetupComponentPublicKeys().combinedControlComponentPublicKeys();

		doReturn(true).when(zeroKnowledgeProof).verifySchnorrProof(any(), any(), any());
	}

	private static Stream<Arguments> provideNullParameters() {

		return Stream.of(
				Arguments.of(null, electoralBoardMembersPasswords, controlComponentPublicKeys),
				Arguments.of(electionEventContextPayload, null, controlComponentPublicKeys),
				Arguments.of(electionEventContextPayload, electoralBoardMembersPasswords, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void setupTallyEBWithNullParametersThrows(final ElectionEventContextPayload electionEventContextPayload,
			final ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords,
			final ImmutableList<ControlComponentPublicKeys> controlComponentPublicKeys) {
		assertThrows(NullPointerException.class,
				() -> setupTallyEBService.setupTallyEB(electionEventContextPayload, electoralBoardMembersPasswords, controlComponentPublicKeys));
	}

	@Test
	@DisplayName("wrong number of electoral board members passwords throws IllegalArgumentException")
	void setupTallyEBWithWrongNumberOfElectoralBoardMembersPasswordsThrows() {
		final ImmutableList<SafePasswordHolder> tooFewElectoralBoardMembersPasswords = ImmutableList.of(
				new SafePasswordHolder("Password_ElectoralBoard1".toCharArray()));

		assertThrows(IllegalArgumentException.class,
				() -> setupTallyEBService.setupTallyEB(electionEventContextPayload, tooFewElectoralBoardMembersPasswords,
						controlComponentPublicKeys));
	}

	@Test
	@DisplayName("wrong number of control component public keys throws IllegalArgumentException")
	void setupTallyEBWithWrongNumberOfControlComponentPublicKeysThrows() {
		final ImmutableList<ControlComponentPublicKeys> tooFewControlComponentPublicKeys = ImmutableList.emptyList();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> setupTallyEBService.setupTallyEB(electionEventContextPayload, electoralBoardMembersPasswords,
						tooFewControlComponentPublicKeys));

		final String expected = String.format("Wrong number of control component public keys. [expected: %s, actual: %s]", ControlComponentNode.ids(),
				tooFewControlComponentPublicKeys.size());
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void setupTallyEBWithValidParametersDoesNotThrow() {
		assertDoesNotThrow(
				() -> setupTallyEBService.setupTallyEB(electionEventContextPayload, electoralBoardMembersPasswords, controlComponentPublicKeys));
	}

}
