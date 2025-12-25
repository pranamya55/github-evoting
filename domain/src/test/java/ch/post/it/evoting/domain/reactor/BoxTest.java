/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.reactor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BoxTest {

	@Test
	void constructWithNullArgumentThrows() {
		assertThrows(NullPointerException.class, () -> new Box<>(null));
	}

	@Test
	void constructWithValidArgumentDoesNotThrow() {
		final String argument = "argument";
		final Box<String> box = assertDoesNotThrow(() -> new Box<>(argument));
		assertEquals(argument, box.boxed());
	}
}
