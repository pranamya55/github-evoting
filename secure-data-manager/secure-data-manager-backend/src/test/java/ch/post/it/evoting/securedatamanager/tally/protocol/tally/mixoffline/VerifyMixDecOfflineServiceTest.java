/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory.createElGamal;
import static ch.post.it.evoting.cryptoprimitives.mixnet.MixnetFactory.createMixnet;
import static ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory.createZeroKnowledgeProof;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.domain.generators.ControlComponentShufflePayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.VerifyMixDecOfflineAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsOutput;
import ch.post.it.evoting.securedatamanager.tally.process.decrypt.IdentifierValidationService;

@DisplayName("verifyMixDecOffline called with")
class VerifyMixDecOfflineServiceTest {

	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static VerifyMixDecOfflineService verifyMixDecOfflineService;
	private static String electionEventId;
	private static VerificationCardSetContext verificationCardSetContext;
	private static SetupComponentPublicKeys setupComponentPublicKeys;
	private static ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads;
	private static GetMixnetInitialCiphertextsOutput getMixnetInitialCiphertextsOutput;
	private static int numberOfMixedVotes;
	private static int numberOfWriteInsPlusOne;

	@BeforeAll
	static void setUpAll() {
		final IdentifierValidationService identifierValidationService = mock(IdentifierValidationService.class);
		final VerifyMixDecOfflineAlgorithm verifyMixDecOfflineAlgorithm = new VerifyMixDecOfflineAlgorithm(createElGamal(), createMixnet(),
				createZeroKnowledgeProof());
		final PrimesMappingTableAlgorithms primesMappingTableAlgorithms = new PrimesMappingTableAlgorithms();
		verifyMixDecOfflineService = new VerifyMixDecOfflineService(identifierValidationService, verifyMixDecOfflineAlgorithm,
				primesMappingTableAlgorithms);

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();
		verificationCardSetContext = electionEventContextPayload.getElectionEventContext().verificationCardSetContexts().get(0);

		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		final int numberOfSelections = primesMappingTableAlgorithms.getPsi(verificationCardSetContext.getPrimesMappingTable());
		numberOfWriteInsPlusOne = primesMappingTableAlgorithms.getDelta(verificationCardSetContext.getPrimesMappingTable());
		setupComponentPublicKeys = new SetupComponentPublicKeysPayloadGenerator(encryptionGroup).generate(numberOfSelections, numberOfWriteInsPlusOne)
				.getSetupComponentPublicKeys();

		numberOfMixedVotes = 10;
		controlComponentShufflePayloads = new ControlComponentShufflePayloadGenerator(encryptionGroup).generate(electionEventId,
				verificationCardSetContext.getBallotBoxId(), numberOfMixedVotes, numberOfWriteInsPlusOne);

		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(encryptionGroup);
		getMixnetInitialCiphertextsOutput = new GetMixnetInitialCiphertextsOutput(
				random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, Base64Alphabet.getInstance()),
				elGamalGenerator.genRandomCiphertextVector(numberOfMixedVotes, numberOfWriteInsPlusOne)
		);

		doNothing().when(identifierValidationService).validateBallotBoxRelatedIds(electionEventId, verificationCardSetContext.getBallotBoxId());
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, verificationCardSetContext, setupComponentPublicKeys, controlComponentShufflePayloads,
						getMixnetInitialCiphertextsOutput),
				Arguments.of(electionEventId, null, setupComponentPublicKeys, controlComponentShufflePayloads,
						getMixnetInitialCiphertextsOutput),
				Arguments.of(electionEventId, verificationCardSetContext, null, controlComponentShufflePayloads,
						getMixnetInitialCiphertextsOutput),
				Arguments.of(electionEventId, verificationCardSetContext, setupComponentPublicKeys, null,
						getMixnetInitialCiphertextsOutput),
				Arguments.of(electionEventId, verificationCardSetContext, setupComponentPublicKeys, controlComponentShufflePayloads, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void verifyMixDecOfflineWithNullParametersThrows(final String electionEventId, final VerificationCardSetContext verificationCardSetContext,
			final SetupComponentPublicKeys setupComponentPublicKeys,
			final ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads,
			final GetMixnetInitialCiphertextsOutput getMixnetInitialCiphertextsOutput) {
		assertThrows(NullPointerException.class,
				() -> verifyMixDecOfflineService.verifyMixDecOffline(electionEventId, verificationCardSetContext, setupComponentPublicKeys,
						controlComponentShufflePayloads, getMixnetInitialCiphertextsOutput));
	}

	@Test
	@DisplayName("invalid election event id throws FailedValidationException")
	void verifyMixDecOfflineWithInvalidElectionEventIdThrows() {
		assertThrows(FailedValidationException.class,
				() -> verifyMixDecOfflineService.verifyMixDecOffline("InvalidElectionEventId", verificationCardSetContext, setupComponentPublicKeys,
						controlComponentShufflePayloads, getMixnetInitialCiphertextsOutput));
	}

	@Test
	@DisplayName("wrong number of control component shuffle payloads throws IllegalStateException")
	void verifyMixDecOfflineWithWrongNumberOfControlComponentShufflePayloadsThrows() {
		final ImmutableList<ControlComponentShufflePayload> tooFewControlComponentShufflePayloads = ImmutableList.emptyList();

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> verifyMixDecOfflineService.verifyMixDecOffline(electionEventId, verificationCardSetContext, setupComponentPublicKeys,
						tooFewControlComponentShufflePayloads, getMixnetInitialCiphertextsOutput));

		final String expected = "Wrong number of control component shuffle payloads.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("wrong node ids in control component shuffle payloads throws IllegalStateException")
	void verifyMixDecOfflineWithWrongNodeIdsInControlComponentShufflePayloadsThrows() {
		final ImmutableList<ControlComponentShufflePayload> wrongNodeIdsControlComponentShufflePayloads = controlComponentShufflePayloads.stream()
				.map(controlComponentShufflePayload ->
						new ControlComponentShufflePayload(
								controlComponentShufflePayload.getEncryptionGroup(),
								controlComponentShufflePayload.getElectionEventId(),
								controlComponentShufflePayload.getBallotBoxId(),
								controlComponentShufflePayload.getNodeId() == ControlComponentNode.last().id() ?
										ControlComponentNode.first().id() :
										controlComponentShufflePayload.getNodeId(),
								controlComponentShufflePayload.getVerifiableShuffle(),
								controlComponentShufflePayload.getVerifiableDecryptions()))
				.collect(toImmutableList());

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> verifyMixDecOfflineService.verifyMixDecOffline(electionEventId, verificationCardSetContext, setupComponentPublicKeys,
						wrongNodeIdsControlComponentShufflePayloads, getMixnetInitialCiphertextsOutput));

		final String expected = "Wrong number of control component shuffle payloads.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("different group payloads throws IllegalArgumentException")
	void verifyMixDecOfflineWithDifferentGroupPayloadsThrows() {
		final GqGroup differentGroup = GroupTestData.getDifferentGqGroup(setupComponentPublicKeys.electionPublicKey().getGroup());
		final ImmutableList<ControlComponentShufflePayload> shufflePayloads = new ControlComponentShufflePayloadGenerator(differentGroup)
				.generate(electionEventId, verificationCardSetContext.getBallotBoxId(), numberOfMixedVotes, numberOfWriteInsPlusOne);
		final ControlComponentShufflePayload differentGroupPayload = shufflePayloads.get(shufflePayloads.size() - 1);
		final ImmutableList<ControlComponentShufflePayload> differentGroupControlComponentShufflePayloads = controlComponentShufflePayloads.stream()
				.map(controlComponentShufflePayload ->
						new ControlComponentShufflePayload(
								controlComponentShufflePayload.getNodeId() == ControlComponentNode.last().id() ?
										differentGroupPayload.getEncryptionGroup() :
										controlComponentShufflePayload.getEncryptionGroup(),
								controlComponentShufflePayload.getElectionEventId(),
								controlComponentShufflePayload.getBallotBoxId(),
								controlComponentShufflePayload.getNodeId(),
								controlComponentShufflePayload.getNodeId() == ControlComponentNode.last().id() ?
										differentGroupPayload.getVerifiableShuffle() :
										controlComponentShufflePayload.getVerifiableShuffle(),
								controlComponentShufflePayload.getNodeId() == ControlComponentNode.last().id() ?
										differentGroupPayload.getVerifiableDecryptions() :
										controlComponentShufflePayload.getVerifiableDecryptions()))
				.collect(toImmutableList());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> verifyMixDecOfflineService.verifyMixDecOffline(electionEventId, verificationCardSetContext, setupComponentPublicKeys,
						differentGroupControlComponentShufflePayloads, getMixnetInitialCiphertextsOutput));

		final String expected = "All control component shuffle payloads must have the same group.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("different election event id payloads throws IllegalArgumentException")
	void verifyMixDecOfflineWithDifferentElectionEventIdPayloadsThrows() {
		final ImmutableList<ControlComponentShufflePayload> differentElectionEventIdControlComponentShufflePayloads = controlComponentShufflePayloads.stream()
				.map(controlComponentShufflePayload ->
						new ControlComponentShufflePayload(
								controlComponentShufflePayload.getEncryptionGroup(),
								controlComponentShufflePayload.getNodeId() == ControlComponentNode.last().id() ?
										uuidGenerator.generate() :
										controlComponentShufflePayload.getElectionEventId(),
								controlComponentShufflePayload.getBallotBoxId(),
								controlComponentShufflePayload.getNodeId(),
								controlComponentShufflePayload.getVerifiableShuffle(),
								controlComponentShufflePayload.getVerifiableDecryptions()))
				.collect(toImmutableList());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> verifyMixDecOfflineService.verifyMixDecOffline(electionEventId, verificationCardSetContext, setupComponentPublicKeys,
						differentElectionEventIdControlComponentShufflePayloads, getMixnetInitialCiphertextsOutput));

		final String expected = "All control component shuffle payloads must have the same election event id.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("different ballot box id payloads throws IllegalArgumentException")
	void verifyMixDecOfflineWithDifferentBallotBoxIdPayloadsThrows() {
		final ImmutableList<ControlComponentShufflePayload> differentBallotBoxIdControlComponentShufflePayloads = controlComponentShufflePayloads.stream()
				.map(controlComponentShufflePayload ->
						new ControlComponentShufflePayload(
								controlComponentShufflePayload.getEncryptionGroup(),
								controlComponentShufflePayload.getElectionEventId(),
								controlComponentShufflePayload.getNodeId() == ControlComponentNode.last().id() ?
										uuidGenerator.generate() :
										controlComponentShufflePayload.getBallotBoxId(),
								controlComponentShufflePayload.getNodeId(),
								controlComponentShufflePayload.getVerifiableShuffle(),
								controlComponentShufflePayload.getVerifiableDecryptions()))
				.collect(toImmutableList());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> verifyMixDecOfflineService.verifyMixDecOffline(electionEventId, verificationCardSetContext, setupComponentPublicKeys,
						differentBallotBoxIdControlComponentShufflePayloads, getMixnetInitialCiphertextsOutput));

		final String expected = "All control component shuffle payloads must have the same ballot box id.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void verifyMixDecOfflineWithValidParametersDoesNotThrow() {
		assertDoesNotThrow(() -> verifyMixDecOfflineService.verifyMixDecOffline(electionEventId, verificationCardSetContext, setupComponentPublicKeys,
				controlComponentShufflePayloads, getMixnetInitialCiphertextsOutput));
	}
}
