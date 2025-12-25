/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

package ch.post.it.evoting.controlcomponent.process.voting.sendvote;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.base.Throwables;

import ch.post.it.evoting.controlcomponent.process.BallotBoxEntity;
import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetEntity;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;

@ExtendWith(MockitoExtension.class)
class PartialDecryptServiceTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private VerificationCardService verificationCardService;
	@Mock
	private ElectionEventContextService electionEventContextService;
	@InjectMocks
	private PartialDecryptService partialDecryptService;

	@Nested
	class ValidateVoteIsAllowed {
		private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		private static final String ANY_ID = uuidGenerator.generate();
		private static final int GRACE_PERIOD = 3600;
		private BallotBoxEntity ballotBoxEntity;

		private LocalDateTime electionStartTime;
		private LocalDateTime electionEndTime;
		private LocalDateTime currentTime;

		@BeforeEach
		void setUp() {
			currentTime = LocalDateTimeUtils.now();
			electionStartTime = currentTime.minusSeconds(GRACE_PERIOD);
			electionEndTime = currentTime.plusSeconds(GRACE_PERIOD);
			ballotBoxEntity = mock(BallotBoxEntity.class);

			when(electionEventContextService.getElectionEventStartTime(anyString())).thenReturn(electionStartTime);
			when(electionEventContextService.getElectionEventFinishTime(anyString())).thenReturn(electionEndTime);
			when(verificationCardService.getVerificationCardEntity(anyString()).getVerificationCardSetEntity()).thenReturn(
					new VerificationCardSetEntity());
		}

		@Test
		@DisplayName("Voting at the begin of the election period works.")
		void votingAtTheBeginOfElectionWorks() {
			when(ballotBoxEntity.isMixed()).thenReturn(false);
			when(ballotBoxEntity.getGracePeriod()).thenReturn(GRACE_PERIOD);

			assertDoesNotThrow(() -> partialDecryptService.validateVoteIsAllowed(ANY_ID, ANY_ID, () -> electionStartTime, ballotBoxEntity));
		}

		@Test
		@DisplayName("Voting at the end of the election period works.")
		void votingAtTheEndOfElectionWorks() {
			when(ballotBoxEntity.isMixed()).thenReturn(false);
			when(ballotBoxEntity.getGracePeriod()).thenReturn(GRACE_PERIOD);

			assertDoesNotThrow(() -> partialDecryptService.validateVoteIsAllowed(ANY_ID, ANY_ID, () -> electionEndTime, ballotBoxEntity));
		}

		@Test
		@DisplayName("Voting before the election period fails.")
		void votingBeforeElectionFails() {
			when(ballotBoxEntity.getGracePeriod()).thenReturn(GRACE_PERIOD);
			when(ballotBoxEntity.getBallotBoxId()).thenReturn(ANY_ID);

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> partialDecryptService.validateVoteIsAllowed(ANY_ID, ANY_ID, () -> electionStartTime.minusSeconds(1), ballotBoxEntity));

			final String errorMessage = String.format(
					"Impossible to vote before or after the dedicated time. [electionEventId: %s, ballotBoxId: %s, verificationCardId: %s, startTime: %s, finishTime: %s, gracePeriod: %s]",
					ANY_ID, ANY_ID, ANY_ID, electionStartTime, electionEndTime, GRACE_PERIOD);
			assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
		}

		@Test
		@DisplayName("Voting after the election period fails.")
		void votingAfterElectionFails() {
			when(ballotBoxEntity.getGracePeriod()).thenReturn(GRACE_PERIOD);
			when(ballotBoxEntity.getBallotBoxId()).thenReturn(ANY_ID);

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> partialDecryptService.validateVoteIsAllowed(ANY_ID, ANY_ID, () -> electionEndTime.plusSeconds(GRACE_PERIOD).plusSeconds(1),
							ballotBoxEntity));

			final String errorMessage = String.format(
					"Impossible to vote before or after the dedicated time. [electionEventId: %s, ballotBoxId: %s, verificationCardId: %s, startTime: %s, finishTime: %s, gracePeriod: %s]",
					ANY_ID, ANY_ID, ANY_ID, electionStartTime, electionEndTime, GRACE_PERIOD);
			assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
		}

		@Test
		@DisplayName("Voting in a mixed ballot box fails.")
		void votingInMixedBallotBoxFails() {
			when(ballotBoxEntity.isMixed()).thenReturn(true);
			when(ballotBoxEntity.getBallotBoxId()).thenReturn(ANY_ID);

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> partialDecryptService.validateVoteIsAllowed(ANY_ID, ANY_ID, () -> currentTime, ballotBoxEntity));

			final String errorMessage = String.format(
					"Impossible to vote in an already mixed ballot box. [electionEventId: %s, ballotBoxId: %s, verificationCardId: %s]",
					ANY_ID, ANY_ID, ANY_ID);
			assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
		}
	}
}
