/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.domain.generators.ControlComponentShufflePayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.tally.process.decrypt.IdentifierValidationService;

@DisplayName("mixDecOffline called with")
class MixDecOfflineServiceTest extends TestGroupSetup {

	private static String electionEventId;
	private static String ballotBoxId;
	private static PrimesMappingTable primesMappingTable;
	private static ControlComponentShufflePayload controlComponentShufflePayload;
	private static ImmutableList<SafePasswordHolder> electoralBoardPasswords;
	private MixDecOfflineService mixDecOfflineService;

	@BeforeAll
	static void setUpSuite() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		ballotBoxId = uuidGenerator.generate();

		primesMappingTable = new PrimesMappingTableGenerator(gqGroup).generate(1);
		electoralBoardPasswords = ImmutableList.of(new SafePasswordHolder("Password_ElectoralBoard1".toCharArray()),
				new SafePasswordHolder("Password_ElectoralBoard2".toCharArray()));

		final int N = 2;
		final int l = 5;
		final ControlComponentShufflePayloadGenerator controlComponentShufflePayloadGenerator = new ControlComponentShufflePayloadGenerator(gqGroup);
		controlComponentShufflePayload = controlComponentShufflePayloadGenerator.generate(electionEventId, ballotBoxId,
				ControlComponentNode.last().id(), N, l);
	}

	@BeforeEach
	void setUp() {
		final MixDecOfflineAlgorithm mixDecOfflineAlgorithm = mock(MixDecOfflineAlgorithm.class);
		final ElectionEventService electionEventService = mock(ElectionEventService.class);
		final BallotBoxService ballotBoxService = mock(BallotBoxService.class);

		final IdentifierValidationService identifierValidationService = new IdentifierValidationService(ballotBoxService, electionEventService);
		final PrimesMappingTableAlgorithms primesMappingTableAlgorithms = mock(PrimesMappingTableAlgorithms.class);
		when(primesMappingTableAlgorithms.getDelta(any())).thenReturn(1);

		mixDecOfflineService = new MixDecOfflineService(mixDecOfflineAlgorithm, identifierValidationService, primesMappingTableAlgorithms);

		when(electionEventService.exists(electionEventId)).thenReturn(true);
		when(ballotBoxService.getBallotBoxIds(electionEventId)).thenReturn(ImmutableList.of(ballotBoxId));
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, ballotBoxId, primesMappingTable, controlComponentShufflePayload, electoralBoardPasswords),
				Arguments.of(electionEventId, null, primesMappingTable, controlComponentShufflePayload, electoralBoardPasswords),
				Arguments.of(electionEventId, ballotBoxId, null, controlComponentShufflePayload, electoralBoardPasswords),
				Arguments.of(electionEventId, ballotBoxId, primesMappingTable, null, electoralBoardPasswords),
				Arguments.of(electionEventId, ballotBoxId, primesMappingTable, controlComponentShufflePayload, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void mixDecOfflineWithNullParametersThrows(final String electionEventId, final String ballotBoxId, final PrimesMappingTable primesMappingTable,
			final ControlComponentShufflePayload controlComponentShufflePayload, final ImmutableList<SafePasswordHolder> electoralBoardPasswords) {
		assertThrows(NullPointerException.class,
				() -> mixDecOfflineService.mixDecOffline(electionEventId, ballotBoxId, primesMappingTable, controlComponentShufflePayload,
						electoralBoardPasswords));
	}

	@Test
	@DisplayName("invalid election event id throws FailedValidationException")
	void mixDecOfflineWithInvalidElectionEventIdThrows() {
		assertThrows(FailedValidationException.class,
				() -> mixDecOfflineService.mixDecOffline("InvalidElectionEventId", ballotBoxId, primesMappingTable,
						controlComponentShufflePayload, electoralBoardPasswords));
	}

	@Test
	@DisplayName("invalid ballot box id throws FailedValidationException")
	void mixDecOfflineWithInvalidBallotBoxIdThrows() {
		assertThrows(FailedValidationException.class,
				() -> mixDecOfflineService.mixDecOffline(electionEventId, "InvalidBallotBoxId", primesMappingTable, controlComponentShufflePayload,
						electoralBoardPasswords));
	}

	@Test
	@DisplayName("wrong number of electoral board members passwords throws IllegalArgumentException")
	void mixDecOfflineWithWrongNumberOfElectoralBoardMembersPasswordsThrows() {
		final ImmutableList<SafePasswordHolder> tooFewElectoralBoardMembersPasswords = ImmutableList.of(
				new SafePasswordHolder("Password_ElectoralBoard1".toCharArray()));

		assertThrows(IllegalArgumentException.class,
				() -> mixDecOfflineService.mixDecOffline(electionEventId, ballotBoxId, primesMappingTable, controlComponentShufflePayload,
						tooFewElectoralBoardMembersPasswords));
	}

	@Test
	@DisplayName("non existent election event id throws IllegalArgumentException")
	void mixDecryptWhenNonExistingElectionEventIdThrowsIllegalArgumentException() {
		final String nonExistingElectionEventId = "0123456789ABCDEF0123456789ABCDEF";

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> mixDecOfflineService.mixDecOffline(nonExistingElectionEventId, ballotBoxId, primesMappingTable, controlComponentShufflePayload,
						electoralBoardPasswords));

		final String expected = String.format("The given election event ID does not exist. [electionEventId: %s]", nonExistingElectionEventId);
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("non existent ballot box id throws IllegalArgumentException")
	void mixDecryptWhenNonExistingBallotBoxIdThrowsIllegalArgumentException() {
		final String nonExistingBallotBoxId = "0123456789ABCDEF0123456789ABCDEF";

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> mixDecOfflineService.mixDecOffline(electionEventId, nonExistingBallotBoxId, primesMappingTable, controlComponentShufflePayload,
						electoralBoardPasswords));

		final String expected = String.format(
				"The given ballot box ID does not belong to the given election event ID. [ballotBoxId: %s, electionEventId: %s]",
				nonExistingBallotBoxId, electionEventId);
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("mixDecOffline with different encryption group throws IllegalArgumentException")
	void mixDecryptWithNonMatchingEncryptionGroup() {
		final GqGroup anotherEncryptionGroup = GroupTestData.getDifferentGqGroup(controlComponentShufflePayload.getEncryptionGroup());

		final PrimesMappingTable anotherPrimesMappingTable = mock(PrimesMappingTable.class);
		when(anotherPrimesMappingTable.getEncryptionGroup()).thenReturn(anotherEncryptionGroup);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> mixDecOfflineService.mixDecOffline(electionEventId, ballotBoxId, anotherPrimesMappingTable, controlComponentShufflePayload,
						electoralBoardPasswords));

		final String expectedMessage = "The primes mapping table encryption group and the control component shuffle payload encryption group do not match.";
		assertEquals(expectedMessage, exception.getMessage());
	}

	@Test
	@DisplayName("mixDecOffline with valid parameters does not throw")
	void mixDecOfflineWithValidParameters() {
		assertDoesNotThrow(
				() -> mixDecOfflineService.mixDecOffline(electionEventId, ballotBoxId, primesMappingTable, controlComponentShufflePayload,
						electoralBoardPasswords));
	}
}
