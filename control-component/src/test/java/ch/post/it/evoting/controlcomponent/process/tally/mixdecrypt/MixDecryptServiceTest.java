/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.base.Throwables;

import ch.post.it.evoting.controlcomponent.process.BallotBoxEntity;
import ch.post.it.evoting.controlcomponent.process.BallotBoxService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.SetupComponentPublicKeysService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.domain.generators.ControlComponentShufflePayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;

@ExtendWith(MockitoExtension.class)
class MixDecryptServiceTest {

	@Mock
	private BallotBoxService ballotBoxService;
	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ElectionEventContextService electionEventContextService;
	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private SetupComponentPublicKeysService setupComponentPublicKeysService;
	@InjectMocks
	private MixDecryptService mixDecryptService;

	@Nested
	class ValidateMixIsAllowed {
		private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		private static final String ANY_ID = uuidGenerator.generate();
		private static final int GRACE_PERIOD = 3600;
		private BallotBoxEntity ballotBoxEntity;

		private LocalDateTime electionEndTime;
		private LocalDateTime currentTime;

		@BeforeEach
		void setUp() {
			currentTime = LocalDateTimeUtils.now();
			electionEndTime = currentTime.plusSeconds(3600);
			ballotBoxEntity = mock(BallotBoxEntity.class);

			when(ballotBoxEntity.getGracePeriod()).thenReturn(GRACE_PERIOD);
			when(ballotBoxService.getBallotBoxByBallotBoxId(anyString())).thenReturn(ballotBoxEntity);
			when(electionEventContextService.getElectionEventFinishTime(anyString())).thenReturn(electionEndTime);
		}

		@Test
		@DisplayName("Test ballot box can be mixed at any time.")
		void testBallotBox() {
			when(ballotBoxEntity.isTestBallotBox()).thenReturn(true);

			assertDoesNotThrow(() -> mixDecryptService.validateMixIsAllowed(ANY_ID, ANY_ID, () -> currentTime));
		}

		@Test
		@DisplayName("Possible to mix after the grace period has expired.")
		void prodBallotBoxWithElectionFinished() {
			when(ballotBoxEntity.isTestBallotBox()).thenReturn(false);

			assertDoesNotThrow(() -> mixDecryptService.validateMixIsAllowed(ANY_ID, ANY_ID,
					() -> electionEndTime.plusSeconds(GRACE_PERIOD).plusSeconds(1)));
		}

		@Test
		@DisplayName("Not possible to mix before the election finish, including grace period has expired")
		void prodBallotBoxWithElectionNotFinished() {
			when(ballotBoxEntity.isTestBallotBox()).thenReturn(false);

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> mixDecryptService.validateMixIsAllowed(ANY_ID, ANY_ID, () -> electionEndTime.plusSeconds(GRACE_PERIOD)));

			final String errorMessage = String.format(
					"The ballot box can not be mixed. [isTestBallotBox: %s, finishTime: %s, electionEventId: %s, ballotBoxId: %s]",
					ballotBoxEntity.isTestBallotBox(), electionEndTime, ANY_ID, ANY_ID);
			assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
		}
	}

	@Nested
	class ValidateShufflePayload {

		private ControlComponentShufflePayloadGenerator generator;
		private int numberOfMixedVotes;
		private int numberOfWriteInsPlusOne;
		private String electionEventId;
		private String ballotBoxId;
		private GqGroup gqGroup;

		@BeforeEach
		void setUp() {
			generator = new ControlComponentShufflePayloadGenerator();
			final ControlComponentShufflePayload controlComponentShufflePayload = generator.generate().getFirst();
			numberOfMixedVotes = controlComponentShufflePayload.getVerifiableDecryptions().get_N();
			numberOfWriteInsPlusOne = controlComponentShufflePayload.getVerifiableDecryptions().get_l();
			electionEventId = controlComponentShufflePayload.getElectionEventId();
			ballotBoxId = controlComponentShufflePayload.getBallotBoxId();
			gqGroup = controlComponentShufflePayload.getEncryptionGroup();

			when(setupComponentPublicKeysService.getElectionPublicKey(anyString()).getGroup()).thenReturn(gqGroup);
		}

		@ParameterizedTest
		@DisplayName("Valid payload pass validation")
		@ValueSource(ints = { 1, 2, 3, 4 })
		void validPayloadPassValidation(final int nodeId) {
			ReflectionTestUtils.setField(mixDecryptService, "nodeId", nodeId);
			final ImmutableList<ControlComponentShufflePayload> shufflePayloads = generator.generate(electionEventId, ballotBoxId, nodeId);

			assertDoesNotThrow(() -> mixDecryptService.validateShufflePayload(gqGroup, electionEventId, ballotBoxId, shufflePayloads));
		}

		@ParameterizedTest
		@DisplayName("Incorrect payload count is given according node ID")
		@MethodSource("incorrectPayloadCountAccordingNodeIdProvider")
		void incorrectPayloadCountAccordingNodeId(final int nodeId, final int payloadCount) {
			ReflectionTestUtils.setField(mixDecryptService, "nodeId", nodeId);
			final ImmutableList<ControlComponentShufflePayload> shufflePayloads = IntStream.range(0, payloadCount)
					.mapToObj(value -> generator.generate(electionEventId, ballotBoxId, nodeId, numberOfMixedVotes, numberOfWriteInsPlusOne))
					.collect(toImmutableList());

			final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
					() -> mixDecryptService.validateShufflePayload(gqGroup, electionEventId, ballotBoxId, shufflePayloads));

			final String errorMessage = String.format(
					"There must be exactly the expected number of shuffle payloads. [expected: %s, actual: %s]", nodeId - 1, shufflePayloads.size());
			assertEquals(errorMessage, Throwables.getRootCause(illegalArgumentException).getMessage());
		}

		static Stream<Arguments> incorrectPayloadCountAccordingNodeIdProvider() {
			return Stream.of(
					Arguments.of(1, 1),
					Arguments.of(2, 0),
					Arguments.of(2, 2),
					Arguments.of(3, 1),
					Arguments.of(3, 3),
					Arguments.of(4, 2),
					Arguments.of(4, 5)
			);
		}

		@ParameterizedTest
		@DisplayName("Payload with incorrect election event id failed validation (except node 1 which has no payload)")
		@ValueSource(ints = { 2, 3, 4 })
		void payloadWithIncorrectElectionEventIdFailedValidation(final int nodeId) {
			ReflectionTestUtils.setField(mixDecryptService, "nodeId", nodeId);
			final String wrongElectionEventId = "22222222222222222222222222222222";
			final ImmutableList<ControlComponentShufflePayload> shufflePayloads = generator.generate(wrongElectionEventId, ballotBoxId, nodeId);

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> mixDecryptService.validateShufflePayload(gqGroup, electionEventId, ballotBoxId, shufflePayloads));

			final String errorMessage = String.format("Election event ID must be identical in shuffle payload. [expected: %s, actual: %s]",
					electionEventId, wrongElectionEventId);
			assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
		}

		@ParameterizedTest
		@DisplayName("Payload with incorrect election event id failed validation (except node 1 which has no payload)")
		@ValueSource(ints = { 2, 3, 4 })
		void payloadWithIncorrectBallotBoxIdFailedValidation(final int nodeId) {
			ReflectionTestUtils.setField(mixDecryptService, "nodeId", nodeId);
			final String wrongBallotBoxId = "22222222222222222222222222222222";
			final ImmutableList<ControlComponentShufflePayload> shufflePayloads = generator.generate(electionEventId, wrongBallotBoxId, nodeId);

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> mixDecryptService.validateShufflePayload(gqGroup, electionEventId, ballotBoxId, shufflePayloads));

			final String errorMessage = String.format("Ballot box ID must be identical in shuffle payload. [expected: %s, actual: %s]", ballotBoxId,
					wrongBallotBoxId);
			assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
		}

		@ParameterizedTest
		@DisplayName("Payload with incorrect encryption group failed validation (except node 1 which has no payload)")
		@ValueSource(ints = { 2, 3, 4 })
		void payloadWithIncorrectGqGroupFailedValidation(final int nodeId) {
			ReflectionTestUtils.setField(mixDecryptService, "nodeId", nodeId);
			final GqGroup wrongGqGroup = GroupTestData.getDifferentGqGroup(gqGroup);
			final ControlComponentShufflePayloadGenerator wrongGqGroupGenerator = new ControlComponentShufflePayloadGenerator(wrongGqGroup);
			final ImmutableList<ControlComponentShufflePayload> shufflePayloads = wrongGqGroupGenerator.generate(electionEventId, ballotBoxId,
					nodeId);

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> mixDecryptService.validateShufflePayload(gqGroup, electionEventId, ballotBoxId, shufflePayloads));

			final String errorMessage = String.format("Gq groups must be identical in shuffle payload. [expected: %s, actual: %s]", gqGroup,
					wrongGqGroup);
			assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
		}

		@ParameterizedTest
		@DisplayName("Payload with incorrect node id failed validation (except node 1 which has no payload)")
		@ValueSource(ints = { 2, 3, 4 })
		void payloadWithIncorrectNodeIdFailedValidation(final int nodeId) {
			ReflectionTestUtils.setField(mixDecryptService, "nodeId", nodeId);
			final ImmutableList<ControlComponentShufflePayload> shufflePayloads = Stream.generate(
							() -> generator.generate(electionEventId, ballotBoxId, nodeId, numberOfMixedVotes, numberOfWriteInsPlusOne))
					.limit(nodeId - 1)
					.collect(toImmutableList());

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> mixDecryptService.validateShufflePayload(gqGroup, electionEventId, ballotBoxId, shufflePayloads));

			final String errorMessage = "Payloads must come from expected nodes.";
			assertTrue(Throwables.getRootCause(illegalStateException).getMessage().startsWith(errorMessage));
		}
	}
}
