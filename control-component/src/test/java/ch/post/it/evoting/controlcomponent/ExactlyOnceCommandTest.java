/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.ID_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base32Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;

class ExactlyOnceCommandTest {
	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final Alphabet base32Alphabet = Base32Alphabet.getInstance();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();

	private static Stream<Arguments> nullArgumentProvider() {
		final String correlationId = random.genRandomString(ID_LENGTH, base64Alphabet);

		final String contextId = random.genRandomString(ID_LENGTH, base32Alphabet);
		final String context = uuidGenerator.generate();
		final Callable<ImmutableByteArray> callable = () -> ImmutableByteArray.of((byte) 0b0000001);
		final ImmutableByteArray requestPayloadHash = ImmutableByteArray.of((byte) 0b0110111);

		return Stream.of(
				Arguments.of(null, contextId, context, callable, callable, requestPayloadHash),
				Arguments.of(correlationId, null, context, callable, callable, requestPayloadHash),
				Arguments.of(correlationId, contextId, null, callable, callable, requestPayloadHash),
				Arguments.of(correlationId, contextId, context, null, callable, requestPayloadHash),
				Arguments.of(correlationId, contextId, context, callable, null, requestPayloadHash),
				Arguments.of(correlationId, contextId, context, callable, callable, null)
		);
	}

	@ParameterizedTest
	@MethodSource("nullArgumentProvider")
	void testBuildWithNullParametersThrowsNullPointerException(final String correlationId, final String contextId, final String context,
			final Callable<ImmutableByteArray> callable, final Callable<ImmutableByteArray> replayCallable,
			final ImmutableByteArray requestPayloadHash) {
		final ExactlyOnceCommand.Builder<ImmutableByteArray> builder = new ExactlyOnceCommand.Builder<ImmutableByteArray>()
				.setCorrelationId(correlationId)
				.setContextId(contextId)
				.setContext(context)
				.setTask(callable)
				.setReplayTask(replayCallable)
				.setRequestPayloadHash(requestPayloadHash);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	void testBuildWithValidParametersDoesNotThrow() {
		final String correlationId = random.genRandomString(ID_LENGTH, base64Alphabet);
		final String contextId = random.genRandomString(ID_LENGTH, base32Alphabet);
		final String context = uuidGenerator.generate();
		final Callable<ImmutableByteArray> callable = () -> ImmutableByteArray.of((byte) 0b0000001);
		final Runnable preValidationTask = () -> {};
		final ImmutableByteArray requestPayloadHash = ImmutableByteArray.of((byte) 0b0110111);
		final int transactionTimeout = 60;

		final ExactlyOnceCommand.Builder<ImmutableByteArray> builder = new ExactlyOnceCommand.Builder<ImmutableByteArray>()
				.setCorrelationId(correlationId)
				.setContextId(contextId)
				.setContext(context)
				.setTask(callable)
				.setReplayTask(callable)
				.setPreValidationTask(preValidationTask)
				.setRequestPayloadHash(requestPayloadHash)
				.setSerializer(Function.identity())
				.setTransactionTimeout(transactionTimeout);
		final ExactlyOnceCommand<ImmutableByteArray> exactlyOnceCommand = assertDoesNotThrow(builder::build);
		assertEquals(correlationId, exactlyOnceCommand.getCorrelationId());
		assertEquals(contextId, exactlyOnceCommand.getContextId());
		assertEquals(context, exactlyOnceCommand.getContext());
		assertEquals(callable, exactlyOnceCommand.getTask());
		assertEquals(callable, exactlyOnceCommand.getReplayTask());
		assertEquals(preValidationTask, exactlyOnceCommand.getPreTask());
		assertEquals(requestPayloadHash, exactlyOnceCommand.getRequestPayloadHash());
		assertEquals(transactionTimeout, exactlyOnceCommand.getTransactionTimeout());
	}
}
