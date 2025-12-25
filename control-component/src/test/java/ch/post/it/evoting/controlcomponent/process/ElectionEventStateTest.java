/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Election event state")
class ElectionEventStateTest {

	@Test
	@DisplayName("transitions with null new state throws NullPointerException")
	void nullNewStateThrows() {
		assertThrows(NullPointerException.class, () -> ElectionEventState.INITIAL.isTransitionValid(null));
	}

	@DisplayName("transitions")
	@MethodSource("stateTransitionsArgumentProvider")
	@ParameterizedTest(name = "from {1} to {2} validity is {0}")
	void transitionsValid(final boolean isValid, final ElectionEventState from, final ElectionEventState to) {
		assertEquals(isValid, from.isTransitionValid(to));
	}

	private static Stream<Arguments> stateTransitionsArgumentProvider() {
		return Stream.of(
				Arguments.of(true, ElectionEventState.INITIAL, ElectionEventState.CONFIGURED),
				Arguments.of(false, ElectionEventState.INITIAL, ElectionEventState.INITIAL),
				Arguments.of(false, ElectionEventState.CONFIGURED, ElectionEventState.INITIAL),
				Arguments.of(false, ElectionEventState.CONFIGURED, ElectionEventState.CONFIGURED)
		);
	}

}