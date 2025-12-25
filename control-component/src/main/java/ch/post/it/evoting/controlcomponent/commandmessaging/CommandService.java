/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.commandmessaging;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

@Service
public class CommandService {

	private final CommandRepository commandRepository;

	public CommandService(final CommandRepository commandRepository) {
		this.commandRepository = commandRepository;
	}

	public CommandEntity save(final CommandId commandId, final ImmutableByteArray requestPayloadHash, final Instant requestTimestamp,
			final ImmutableByteArray responsePayloadHash, final ImmutableByteArray responsePayload, final Instant responseTimestamp) {
		checkNotNull(commandId);
		checkNotNull(requestPayloadHash);
		checkNotNull(responsePayloadHash);
		checkNotNull(responsePayload);

		final CommandEntity commandEntity = CommandEntity.builder()
				.commandId(commandId)
				.requestPayloadHash(requestPayloadHash)
				.requestTimestamp(requestTimestamp)
				.responsePayloadHash(responsePayloadHash)
				.responsePayload(responsePayload)
				.responseTimestamp(responseTimestamp)
				.build();
		return commandRepository.save(commandEntity);
	}

	public ImmutableList<CommandEntity> findAllCommandsWithCorrelationId(final String correlationId) {
		checkNotNull(correlationId);

		return ImmutableList.from(commandRepository.findAllByCorrelationId(correlationId));
	}

	public Optional<CommandEntity> findIdenticalCommand(final CommandId commandId) {
		checkNotNull(commandId);

		return commandRepository.findById(commandId);
	}

	public ImmutableList<CommandEntity> findSemanticallyIdenticalCommand(final CommandId commandId) {
		checkNotNull(commandId);

		final String contextId = commandId.getContextId();
		final String context = commandId.getContext();
		final Integer nodeId = commandId.getNodeId();

		return ImmutableList.from(commandRepository.findAllByContextIdAndContextAndNodeId(contextId, context, nodeId));
	}
}
