/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Different states possible for the election event. It also maintains the state transitions.
 */
public enum ElectionEventState {

	CONFIGURED(null),
	INITIAL(CONFIGURED);

	private final ElectionEventState next;

	ElectionEventState(final ElectionEventState next) {
		this.next = next;
	}

	/**
	 * Checks if a state transition from {@code this} to {@code toState} is valid.
	 *
	 * @param toState the state transition to check. Must be non-null.
	 * @return {@code true} if the state transition is valid, {@code false} otherwise.
	 * @throws NullPointerException if {@code toState} is null.
	 */
	public boolean isTransitionValid(final ElectionEventState toState) {
		checkNotNull(toState);

		return toState.equals(this.next);
	}

}
