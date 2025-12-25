/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory.createElGamal;
import static ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory.createZeroKnowledgeProof;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyCCSchnorrProofsAlgorithm;

@DisplayName("genVerCardSetKeys called with")
class GenVerCardSetKeysServiceTest {

	private static GenVerCardSetKeysService genVerCardSetKeysService;
	private static ImmutableList<ControlComponentPublicKeys> controlComponentPublicKeys;
	private static ElectionEventContextPayload electionEventContextPayload;

	@BeforeAll
	static void setUpAll() {
		final VerifyCCSchnorrProofsAlgorithm verifyCCSchnorrProofsAlgorithm = spy(new VerifyCCSchnorrProofsAlgorithm(createZeroKnowledgeProof()));
		final GenVerCardSetKeysAlgorithm genVerCardSetKeysAlgorithm = new GenVerCardSetKeysAlgorithm(createElGamal(), verifyCCSchnorrProofsAlgorithm);
		genVerCardSetKeysService = new GenVerCardSetKeysService(genVerCardSetKeysAlgorithm);

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		final ElectionEventContext electionEventContext = electionEventContextPayload.getElectionEventContext();
		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		final SetupComponentPublicKeys setupComponentPublicKeys = new SetupComponentPublicKeysPayloadGenerator(encryptionGroup)
				.generate(electionEventContext.maximumNumberOfSelections(), electionEventContext.maximumNumberOfWriteInsPlusOne())
				.getSetupComponentPublicKeys();
		controlComponentPublicKeys = setupComponentPublicKeys.combinedControlComponentPublicKeys();

		doReturn(true).when(verifyCCSchnorrProofsAlgorithm).verifyCCSchnorrProofs(any(), any());
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, controlComponentPublicKeys),
				Arguments.of(electionEventContextPayload, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void genVerCardSetKeysWithNullParametersThrows(final ElectionEventContextPayload electionEventContextPayload,
			final ImmutableList<ControlComponentPublicKeys> controlComponentPublicKeys) {
		assertThrows(NullPointerException.class,
				() -> genVerCardSetKeysService.genVerCardSetKeys(electionEventContextPayload, controlComponentPublicKeys));
	}

	@Test
	@DisplayName("wrong number of control component public keys throws IllegalArgumentException")
	void genVerCardSetKeysWithWrongNumberOfControlComponentPublicKeysThrows() {
		final ImmutableList<ControlComponentPublicKeys> tooFewControlComponentPublicKeys = ImmutableList.emptyList();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genVerCardSetKeysService.genVerCardSetKeys(electionEventContextPayload, tooFewControlComponentPublicKeys));

		final String expected = String.format("Wrong number of control component public keys. [expected: %s, actual: %s]", ControlComponentNode.ids(),
				tooFewControlComponentPublicKeys.size());
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("encryption group of election event context payload does not match encryption group of control component public keys throws IllegalArgumentException")
	void genVerCardSetKeysWithInconsistentGroupsThrows() {
		final ElectionEventContextPayload anotherElectionEventContextPayload = mock(ElectionEventContextPayload.class);
		when(anotherElectionEventContextPayload.getEncryptionGroup()).thenReturn(
				GroupTestData.getDifferentGqGroup(electionEventContextPayload.getEncryptionGroup()));

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genVerCardSetKeysService.genVerCardSetKeys(anotherElectionEventContextPayload, controlComponentPublicKeys));

		assertEquals(
				"The encryption group of the control component public keys does not match the encryption group of the election event context payload.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("all ccrjChoiceReturnCodesEncryptionPublicKey elements must belong to the same group throws IllegalArgumentException")
	void genVerCardSetKeysWithDifferentGroupsThrows() {
		final ControlComponentPublicKeys anotherControlComponentPublicKeys = mock(ControlComponentPublicKeys.class);
		final ElGamalMultiRecipientPublicKey anotherElGamalMultiRecipientPublicKey = mock(ElGamalMultiRecipientPublicKey.class);
		when(anotherControlComponentPublicKeys.ccrjChoiceReturnCodesEncryptionPublicKey()).thenReturn(anotherElGamalMultiRecipientPublicKey);
		when(anotherElGamalMultiRecipientPublicKey.getGroup()).thenReturn(
				GroupTestData.getDifferentGqGroup(controlComponentPublicKeys.get(0).ccrjChoiceReturnCodesEncryptionPublicKey().getGroup()));

		final ImmutableList<ControlComponentPublicKeys> controlComponentPublicKeysWithDifferentGroups = Stream.concat(
				controlComponentPublicKeys.stream().limit(1),
				Stream.concat(
						Stream.of(anotherControlComponentPublicKeys),
						controlComponentPublicKeys.stream().skip(2))
		).collect(toImmutableList());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genVerCardSetKeysService.genVerCardSetKeys(electionEventContextPayload, controlComponentPublicKeysWithDifferentGroups));

		assertEquals("All ccrjChoiceReturnCodesEncryptionPublicKey elements must belong to the same group.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("all ccrjSchnorrProofs elements must belong to the same group throws IllegalArgumentException")
	void genVerCardSetKeysWithDifferentGroupsSchnorrProofsThrows() {
		final ControlComponentPublicKeys anotherControlComponentPublicKeys = mock(ControlComponentPublicKeys.class);
		final ElGamalMultiRecipientPublicKey originalElGamalMultiRecipientPublicKey = controlComponentPublicKeys.get(0)
				.ccrjChoiceReturnCodesEncryptionPublicKey();
		when(anotherControlComponentPublicKeys.ccrjChoiceReturnCodesEncryptionPublicKey()).thenReturn(originalElGamalMultiRecipientPublicKey);
		final GroupVector<SchnorrProof, ZqGroup> schnorrProofs = mock(GroupVector.class);
		when(anotherControlComponentPublicKeys.ccrjSchnorrProofs()).thenReturn(schnorrProofs);
		when(schnorrProofs.getGroup()).thenReturn(
				GroupTestData.getDifferentZqGroup(controlComponentPublicKeys.get(0).ccmjSchnorrProofs().getGroup()));

		final ImmutableList<ControlComponentPublicKeys> controlComponentPublicKeysWithDifferentGroups = Stream.concat(
				controlComponentPublicKeys.stream().limit(1),
				Stream.concat(
						Stream.of(anotherControlComponentPublicKeys),
						controlComponentPublicKeys.stream().skip(2))
		).collect(toImmutableList());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genVerCardSetKeysService.genVerCardSetKeys(electionEventContextPayload, controlComponentPublicKeysWithDifferentGroups));

		assertEquals("All ccrjSchnorrProofs elements must belong to the same group.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void genVerCardSetKeysWithValidParametersDoesNotThrow() {
		assertDoesNotThrow(() -> genVerCardSetKeysService.genVerCardSetKeys(electionEventContextPayload, controlComponentPublicKeys));
	}

}
