/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Service to manage the state of the election event.
 */
@Service
public class ElectionEventStateService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectionEventStateService.class);

	private final ElectionEventStateRepository electionEventStateRepository;

	public ElectionEventStateService(final ElectionEventStateRepository electionEventStateRepository) {
		this.electionEventStateRepository = electionEventStateRepository;
	}

	/**
	 * Retrieves the election event state of the corresponding election event.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the election event state of the election event identified by {@code electionEventId}
	 * @throws NullPointerException      if {@code electionEventId} is null.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 */
	public ElectionEventState getElectionEventState(final String electionEventId) {
		validateUUID(electionEventId);

		final ElectionEventState state = getElectionEventStateEntity(electionEventId).getState();
		LOGGER.debug("Retrieved election event state. [electionEventId: {}, state: {}]", electionEventId, state);

		return state;
	}

	/**
	 * Updates the election event state of the corresponding election event with the given new state.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param newElectionEventState the new state to transition to. Must be non-null.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 * @throws IllegalStateException     if the transition to the new state is invalid.
	 * @see ElectionEventState#isTransitionValid(ElectionEventState)
	 */
	public void updateElectionEventState(final String electionEventId, final ElectionEventState newElectionEventState) {
		validateUUID(electionEventId);
		checkNotNull(newElectionEventState);

		final ElectionEventStateEntity electionEventStateEntity = getElectionEventStateEntity(electionEventId);
		final ElectionEventState currentState = electionEventStateEntity.getState();

		checkState(currentState.isTransitionValid(newElectionEventState),
				"Invalid state transition. [current: %s, next: %s]", currentState, newElectionEventState);

		electionEventStateEntity.setState(newElectionEventState);
		electionEventStateRepository.save(electionEventStateEntity);
		LOGGER.debug("Updated election event state. [electionEventId: {}, state: {}]", electionEventId, newElectionEventState);
	}

	private ElectionEventStateEntity getElectionEventStateEntity(final String electionEventId) {
		final ElectionEventStateEntity electionEventStateEntity = electionEventStateRepository.findById(electionEventId)
				.orElseThrow(
						() -> new IllegalStateException(String.format("Election event state not found. [electionEventId: %s]", electionEventId)));
		LOGGER.debug("Retrieved election event state. [electionEventId: {}]", electionEventId);

		return electionEventStateEntity;
	}

}
