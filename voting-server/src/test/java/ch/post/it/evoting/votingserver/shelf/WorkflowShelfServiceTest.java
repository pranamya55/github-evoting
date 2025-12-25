/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.shelf;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.votingserver.messaging.Serializer;

class WorkflowShelfServiceTest {

	private static final WorkflowShelfRepository workflowShelfRepository = mock(WorkflowShelfRepository.class);
	private static final Serializer serializer = mock(Serializer.class);
	private static WorkflowShelfService workflowShelfService;

	@BeforeAll
	static void beforeAll() {
		workflowShelfService = new WorkflowShelfService(workflowShelfRepository, serializer);
	}

	@Test
	@SuppressWarnings("unchecked")
	void happyPath() {
		final String id = "uniqueId";
		final ImmutableByteArray data = ImmutableByteArray.of((byte) 1, (byte) 2, (byte) 3);

		when(serializer.serialize(any())).thenReturn(data);
		when(serializer.deserialize(any(), eq(ImmutableByteArray.class))).thenReturn(data);
		when(workflowShelfRepository.findById(anyString())).thenReturn(Optional.of(new WorkflowShelfEntity(id, data)));

		assertDoesNotThrow(() -> workflowShelfService.pushToShelf(id, data));

		final ImmutableByteArray bytes = assertDoesNotThrow(() -> workflowShelfService.pullFromShelf(id, data.getClass()));
		assertEquals(data, bytes);

		when(workflowShelfRepository.findById(anyString())).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class, () -> workflowShelfService.pullFromShelf(id, byte[].class));
	}

}
