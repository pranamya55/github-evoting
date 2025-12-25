/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.exceptions;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;

class ExceptionPayloadTest {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private String correlationId;
	private int nodeId;
	private Throwable throwable;

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		correlationId = uuidGenerator.generate();

		nodeId = SECURE_RANDOM.nextInt(4) + 1;
		throwable = new Throwable();
	}

	@Test
	void constructWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> new ExceptionPayload(null, nodeId, throwable));
		assertThrows(NullPointerException.class, () -> new ExceptionPayload(correlationId, nodeId, null));
	}

	@Test
	void constructWithNodeIdNotInRangeThrows() {
		assertThrows(IllegalArgumentException.class, () -> new ExceptionPayload(correlationId, 0, throwable));
		assertThrows(IllegalArgumentException.class, () -> new ExceptionPayload(correlationId, 5, throwable));
	}

	@Test
	void constructWithValidInputCreatesPayload() {
		assertDoesNotThrow(() -> new ExceptionPayload(correlationId, nodeId, throwable));
	}
}
