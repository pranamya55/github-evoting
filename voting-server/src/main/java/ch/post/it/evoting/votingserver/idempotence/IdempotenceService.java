/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.idempotence;

import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;

@Service
public class IdempotenceService<T extends Supplier<String>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdempotenceService.class);

	private final Hash hash;
	private final IdempotentExecutionRepository idempotentExecutionRepository;

	public IdempotenceService(
			final Hash hash,
			final IdempotentExecutionRepository idempotentExecutionRepository) {
		this.hash = hash;
		this.idempotentExecutionRepository = idempotentExecutionRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW) // Ensure all processing is atomically executed in its own transaction.
	public <U, P extends Hashable> U execute(final T context, final String executionKey, final P payload, final Supplier<U> execution,
			final Supplier<U> getter) {

		final IdempotentExecutionId idempotentExecutionId = new IdempotentExecutionId(context.get(), executionKey);
		final ImmutableByteArray payloadHash = hash.recursiveHash(payload);

		if (!exists(idempotentExecutionId)) {
			final U result = execution.get();
			save(idempotentExecutionId, payloadHash);
			return result;
		} else {
			final ImmutableByteArray executedPayloadHash = load(idempotentExecutionId);
			if (!payloadHash.equals(executedPayloadHash)) {
				throw new IllegalStateException(
						"Request already executed, but with different payload. [context: %s, executionKey: %s]".formatted(context.get(),
								executionKey));
			}
			LOGGER.warn("Execution bypassed. [context: {}, executionKey: {}]", context.get(), executionKey);
			return getter.get();
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW) // Ensure all processing is atomically executed in its own transaction.
	public <P extends Hashable> void execute(final T context, final String executionKey, final P payload, final Runnable execution) {
		final IdempotentExecutionId idempotentExecutionId = new IdempotentExecutionId(context.get(), executionKey);
		final ImmutableByteArray payloadHash = hash.recursiveHash(payload);

		if (!exists(idempotentExecutionId)) {
			execution.run();
			save(idempotentExecutionId, payloadHash);
		} else {
			final ImmutableByteArray executedPayloadHash = load(idempotentExecutionId);
			if (!payloadHash.equals(executedPayloadHash)) {
				throw new IllegalStateException(
						"Request already executed, but with different payload. [context: %s, executionKey: %s]".formatted(context.get(),
								executionKey));
			}
			LOGGER.warn("Execution bypassed. [context: {}, executionKey: {}]", context.get(), executionKey);
		}
	}

	private boolean exists(final IdempotentExecutionId idempotentExecutionId) {
		return idempotentExecutionRepository.existsById(idempotentExecutionId);
	}

	private void save(final IdempotentExecutionId idempotentExecutionId, final ImmutableByteArray payloadHash) {
		final IdempotentExecution idempotentExecutionEntity = new IdempotentExecution(idempotentExecutionId.getContext(),
				idempotentExecutionId.getExecutionKey(), payloadHash);
		idempotentExecutionRepository.save(idempotentExecutionEntity);
	}

	private ImmutableByteArray load(final IdempotentExecutionId idempotentExecutionId) {
		final Optional<IdempotentExecution> idempotentExecution = idempotentExecutionRepository.findById(idempotentExecutionId);
		return idempotentExecution.map(IdempotentExecution::getPayloadHash)
				.orElseThrow(() -> new IllegalStateException(
						"No idempotent execution found. [idempotentExecutionId: %s]".formatted(idempotentExecutionId)));
	}
}
