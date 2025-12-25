/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.base.Throwables;

import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Election event service")
class ElectionEventStateServiceTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	@Mock
	private ElectionEventStateRepository electionEventStateRepository;

	@InjectMocks
	private ElectionEventStateService electionEventStateService;

	@Nested
	@DisplayName("getting election event state")
	class getElectionEventStateTest {

		private String electionEventId;

		@BeforeEach
		void setup() {
			reset(electionEventStateRepository);

			electionEventId = uuidGenerator.generate();
		}

		@Test
		@DisplayName("with null election event id throws NullPointerException")
		void nullElectionEventIdThrows() {
			assertThrows(NullPointerException.class, () -> electionEventStateService.getElectionEventState(null));
		}

		@Test
		@DisplayName("with invalid election event id throws FailedValidationException")
		void invalidElectionEventIdThrows() {
			assertThrows(FailedValidationException.class, () -> electionEventStateService.getElectionEventState("invalid"));
		}

		@Test
		@DisplayName("returns INITIAL state")
		void existingReturnsInitialState() {
			final ElectionEventStateEntity electionEventStateEntity = new ElectionEventStateEntity();
			when(electionEventStateRepository.findById(electionEventId)).thenReturn(Optional.of(electionEventStateEntity));

			final ElectionEventState electionEventState = electionEventStateService.getElectionEventState(electionEventId);
			assertEquals(ElectionEventState.INITIAL, electionEventState);
		}

		@Test
		@DisplayName("of non existing election throws")
		void nonExistingElectionThrows() {
			final String nonExistingElectionEventId = uuidGenerator.generate();

			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> electionEventStateService.getElectionEventState(nonExistingElectionEventId));

			final String errorMessage = String.format("Election event state not found. [electionEventId: %s]", nonExistingElectionEventId);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}

	}

	@Nested
	@DisplayName("updating election event state")
	class updateElectionEventStateTest {

		private String electionEventId;

		@BeforeEach
		void setup() {
			reset(electionEventStateRepository);

			electionEventId = uuidGenerator.generate();
		}

		@Test
		@DisplayName("with null parameters throws NullPointerException")
		void nullParametersThrows() {
			assertThrows(NullPointerException.class, () -> electionEventStateService.updateElectionEventState(null, ElectionEventState.CONFIGURED));
			assertThrows(NullPointerException.class, () -> electionEventStateService.updateElectionEventState(electionEventId, null));
		}

		@Test
		@DisplayName("with invalid election event id throws FailedValidationException")
		void invalidElectionEventIdThrows() {
			assertThrows(FailedValidationException.class,
					() -> electionEventStateService.updateElectionEventState("invalid", ElectionEventState.CONFIGURED));
		}

		@Test
		@DisplayName("with invalid state transition throws IllegalStateException")
		void invalidStateTransitionThrows() {
			final ElectionEventStateEntity electionEventStateEntity = new ElectionEventStateEntity();
			when(electionEventStateRepository.findById(electionEventId)).thenReturn(Optional.of(electionEventStateEntity));

			final ElectionEventState newElectionEventState = ElectionEventState.INITIAL;
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> electionEventStateService.updateElectionEventState(electionEventId, newElectionEventState));

			final String errorMessage = String.format("Invalid state transition. [current: %s, next: %s]", ElectionEventState.INITIAL,
					newElectionEventState);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("correctly updates state")
		void correctlyUpdatesState() {
			final ElectionEventStateEntity electionEventStateEntity = new ElectionEventStateEntity();
			when(electionEventStateRepository.findById(electionEventId)).thenReturn(Optional.of(electionEventStateEntity));

			final ElectionEventState newElectionEventState = ElectionEventState.CONFIGURED;
			electionEventStateService.updateElectionEventState(electionEventId, newElectionEventState);

			electionEventStateEntity.setState(newElectionEventState);
			verify(electionEventStateRepository).save(electionEventStateEntity);
		}

	}

}
