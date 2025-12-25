/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.shelf;

import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.votingserver.messaging.Serializer;

@Service
public class WorkflowShelfService {

	private final WorkflowShelfRepository repository;
	private final Serializer serializer;

	public WorkflowShelfService(final WorkflowShelfRepository repository, final Serializer serializer) {
		this.repository = repository;
		this.serializer = serializer;
	}

	/**
	 * Push a data to a shelf in database
	 *
	 * @param id   the key identifying the data that will be shelf
	 * @param data the data to be shelved.
	 * @param <T>  the type of the data
	 */
	public <T> void pushToShelf(final String id, final T data) {
		checkNotNull(id);
		checkNotNull(data);

		final ImmutableByteArray dataAsBytes = serializer.serialize(data);

		final WorkflowShelfEntity entity = new WorkflowShelfEntity(id, dataAsBytes);

		repository.save(entity);
	}

	/**
	 * Pull data from shelf in database
	 *
	 * @param id    the key identifying the data to retrieve from shelf
	 * @param clazz the clazz of the data to be pull
	 * @param <T>   the type of the data
	 * @return the data
	 */
	public <T> T pullFromShelf(final String id, final Class<T> clazz) {
		checkNotNull(id);
		checkNotNull(clazz);

		final WorkflowShelfEntity entity = repository.findById(id)
				.orElseThrow(() -> new IllegalStateException(String.format("No shelf with this correlationId. [id: %s]", id)));

		final T deserialize = serializer.deserialize(entity.getShelfData(), clazz);

		repository.deleteById(id);

		return deserialize;
	}
}
