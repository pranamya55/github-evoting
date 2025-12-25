/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixonline;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory.createElGamal;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory.createBase64;
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
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.domain.generators.ControlComponentBallotBoxPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsAlgorithm;
import ch.post.it.evoting.securedatamanager.tally.process.decrypt.IdentifierValidationService;

@DisplayName("getMixnetInitialCiphertexts called with")
class GetMixnetInitialCiphertextsServiceTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static GetMixnetInitialCiphertextsService getMixnetInitialCiphertextsService;
	private static String electionEventId;
	private static VerificationCardSetContext verificationCardSetContext;
	private static SetupComponentPublicKeys setupComponentPublicKeys;
	private static ImmutableList<ControlComponentBallotBoxPayload> controlComponentBallotBoxPayloads;
	private static int psi;
	private static int delta;
	private static ControlComponentBallotBoxPayloadGenerator controlComponentBallotBoxPayloadGenerator;

	@BeforeAll
	static void setUpAll() {
		final IdentifierValidationService identifierValidationService = mock(IdentifierValidationService.class);
		final GetMixnetInitialCiphertextsAlgorithm getMixnetInitialCiphertextsAlgorithm = new GetMixnetInitialCiphertextsAlgorithm(createHash(),
				createBase64(), createElGamal());
		final PrimesMappingTableAlgorithms primesMappingTableAlgorithms = new PrimesMappingTableAlgorithms();
		getMixnetInitialCiphertextsService = new GetMixnetInitialCiphertextsService(identifierValidationService, primesMappingTableAlgorithms,
				getMixnetInitialCiphertextsAlgorithm);

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();
		verificationCardSetContext = electionEventContextPayload.getElectionEventContext().verificationCardSetContexts().get(0);

		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		psi = primesMappingTableAlgorithms.getPsi(verificationCardSetContext.getPrimesMappingTable());
		delta = primesMappingTableAlgorithms.getDelta(verificationCardSetContext.getPrimesMappingTable());
		setupComponentPublicKeys = new SetupComponentPublicKeysPayloadGenerator(encryptionGroup).generate(psi, delta)
				.getSetupComponentPublicKeys();
		controlComponentBallotBoxPayloadGenerator = new ControlComponentBallotBoxPayloadGenerator(encryptionGroup);
		controlComponentBallotBoxPayloads = controlComponentBallotBoxPayloadGenerator.generate(electionEventId,
				verificationCardSetContext.getBallotBoxId(), psi, delta);

		doNothing().when(identifierValidationService).validateBallotBoxRelatedIds(electionEventId, verificationCardSetContext.getBallotBoxId());
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, verificationCardSetContext, setupComponentPublicKeys, controlComponentBallotBoxPayloads),
				Arguments.of(electionEventId, null, setupComponentPublicKeys, controlComponentBallotBoxPayloads),
				Arguments.of(electionEventId, verificationCardSetContext, null, controlComponentBallotBoxPayloads),
				Arguments.of(electionEventId, verificationCardSetContext, setupComponentPublicKeys, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void getMixnetInitialCiphertextsWithNullParametersThrows(final String electionEventId,
			final VerificationCardSetContext verificationCardSetContext, final SetupComponentPublicKeys setupComponentPublicKeys,
			final ImmutableList<ControlComponentBallotBoxPayload> controlComponentBallotBoxPayloads) {
		assertThrows(NullPointerException.class,
				() -> getMixnetInitialCiphertextsService.getMixnetInitialCiphertexts(electionEventId, verificationCardSetContext,
						setupComponentPublicKeys, controlComponentBallotBoxPayloads));
	}

	@Test
	@DisplayName("invalid election event id throws FailedValidationException")
	void getMixnetInitialCiphertextsWithInvalidElectionEventIdThrows() {
		assertThrows(FailedValidationException.class,
				() -> getMixnetInitialCiphertextsService.getMixnetInitialCiphertexts("InvalidElectionEventId", verificationCardSetContext,
						setupComponentPublicKeys, controlComponentBallotBoxPayloads));
	}

	@Test
	@DisplayName("wrong number of control component ballot box payloads throws IllegalStateException")
	void getMixnetInitialCiphertextsWithWrongNumberOfControlComponentBallotBoxPayloadsThrows() {
		final ImmutableList<ControlComponentBallotBoxPayload> tooFewControlComponentBallotBoxPayloads = ImmutableList.emptyList();

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> getMixnetInitialCiphertextsService.getMixnetInitialCiphertexts(electionEventId, verificationCardSetContext,
						setupComponentPublicKeys, tooFewControlComponentBallotBoxPayloads));

		final String expected = "Wrong number of control component ballot box payloads.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("wrong node ids in control component ballot box payloads throws IllegalStateException")
	void getMixnetInitialCiphertextsWithWrongNodeIdsInControlComponentBallotBoxPayloadsThrows() {
		final ImmutableList<ControlComponentBallotBoxPayload> wrongNodeIdsControlComponentBallotBoxPayloads = controlComponentBallotBoxPayloads.stream()
				.map(controlComponentBallotBoxPayload ->
						new ControlComponentBallotBoxPayload(
								controlComponentBallotBoxPayload.getEncryptionGroup(),
								controlComponentBallotBoxPayload.getElectionEventId(),
								controlComponentBallotBoxPayload.getBallotBoxId(),
								controlComponentBallotBoxPayload.getNodeId() == ControlComponentNode.last().id() ?
										ControlComponentNode.first().id() :
										controlComponentBallotBoxPayload.getNodeId(),
								controlComponentBallotBoxPayload.getConfirmedEncryptedVotes()))
				.collect(toImmutableList());

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> getMixnetInitialCiphertextsService.getMixnetInitialCiphertexts(electionEventId, verificationCardSetContext,
						setupComponentPublicKeys, wrongNodeIdsControlComponentBallotBoxPayloads));

		final String expected = "Wrong number of control component ballot box payloads.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("different group payloads throws IllegalArgumentException")
	void getMixnetInitialCiphertextsWithDifferentGroupPayloadsThrows() {
		final ControlComponentBallotBoxPayload lastControlComponentBallotBoxPayload = controlComponentBallotBoxPayloads.getLast();
		final ControlComponentBallotBoxPayloadGenerator generator = new ControlComponentBallotBoxPayloadGenerator(
				GroupTestData.getDifferentGqGroup(lastControlComponentBallotBoxPayload.getEncryptionGroup()));
		final ImmutableList<ControlComponentBallotBoxPayload> differentGroupControlComponentBallotBoxPayloads = controlComponentBallotBoxPayloads.subList(
						0, 3)
				.append(generator.generate().getLast());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> getMixnetInitialCiphertextsService.getMixnetInitialCiphertexts(electionEventId, verificationCardSetContext,
						setupComponentPublicKeys, differentGroupControlComponentBallotBoxPayloads));

		final String expected = "All control component ballot box payloads must have the same group.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("different election event id payloads throws IllegalArgumentException")
	void getMixnetInitialCiphertextsWithDifferentElectionEventIdPayloadsThrows() {
		final ImmutableList<ControlComponentBallotBoxPayload> ballotBoxPayloads = controlComponentBallotBoxPayloadGenerator.generate(
				uuidGenerator.generate(), verificationCardSetContext.getBallotBoxId(), psi, delta);
		final ControlComponentBallotBoxPayload otherElectionEventIdPayload = ballotBoxPayloads.get(ballotBoxPayloads.size() - 1);

		final ImmutableList<ControlComponentBallotBoxPayload> differentElectionEventIdControlComponentBallotBoxPayloads = controlComponentBallotBoxPayloads.stream()
				.map(controlComponentBallotBoxPayload ->
						controlComponentBallotBoxPayload.getNodeId() == ControlComponentNode.last().id() ?
								otherElectionEventIdPayload :
								controlComponentBallotBoxPayload
				).collect(toImmutableList());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> getMixnetInitialCiphertextsService.getMixnetInitialCiphertexts(electionEventId, verificationCardSetContext,
						setupComponentPublicKeys, differentElectionEventIdControlComponentBallotBoxPayloads));

		final String expected = "All control component ballot box payloads must have the same election event id.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("different ballot box id payloads throws IllegalArgumentException")
	void getMixnetInitialCiphertextsWithDifferentBallotBoxIdPayloadsThrows() {
		final ImmutableList<ControlComponentBallotBoxPayload> differentBallotBoxIdControlComponentBallotBoxPayloads = controlComponentBallotBoxPayloads.stream()
				.map(controlComponentBallotBoxPayload ->
						new ControlComponentBallotBoxPayload(
								controlComponentBallotBoxPayload.getEncryptionGroup(),
								controlComponentBallotBoxPayload.getElectionEventId(),
								controlComponentBallotBoxPayload.getNodeId() == ControlComponentNode.last().id() ?
										uuidGenerator.generate() :
										controlComponentBallotBoxPayload.getBallotBoxId(),
								controlComponentBallotBoxPayload.getNodeId(),
								controlComponentBallotBoxPayload.getConfirmedEncryptedVotes()))
				.collect(toImmutableList());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> getMixnetInitialCiphertextsService.getMixnetInitialCiphertexts(electionEventId, verificationCardSetContext,
						setupComponentPublicKeys,
						differentBallotBoxIdControlComponentBallotBoxPayloads));

		final String expected = "All control component ballot box payloads must have the same ballot box id.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void getMixnetInitialCiphertextsWithValidParametersDoesNotThrow() {
		assertDoesNotThrow(() -> getMixnetInitialCiphertextsService.getMixnetInitialCiphertexts(electionEventId, verificationCardSetContext,
				setupComponentPublicKeys, controlComponentBallotBoxPayloads));
	}
}
