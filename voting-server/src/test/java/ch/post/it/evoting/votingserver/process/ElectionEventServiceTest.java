/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class ElectionEventServiceTest {

	private ElectionEventRepository electionEventRepositoryMock;
	private ElectionEventService electionEventService;

	private String electionEventId;
	private GqGroup encryptionGroup;

	@BeforeEach
	void setup() {
		electionEventRepositoryMock = mock(ElectionEventRepository.class);
		electionEventService = new ElectionEventService(electionEventRepositoryMock);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		encryptionGroup = GroupTestData.getGqGroup();
	}

	@Test
	void saveWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> electionEventService.save(null, encryptionGroup));
		assertThrows(NullPointerException.class, () -> electionEventService.save(electionEventId, null));
	}

	@Nested
	class Exist {
		@Test
		void existingEvent() {
			when(electionEventRepositoryMock.existsById(electionEventId)).thenReturn(true);

			assertTrue(electionEventService.exists(electionEventId));
		}

		@Test
		void nonExistingEvent() {
			when(electionEventRepositoryMock.existsById(electionEventId)).thenReturn(false);

			assertFalse(electionEventService.exists(electionEventId));
		}

		@Test
		void nullPointerException() {
			assertThrows(NullPointerException.class, () -> electionEventService.exists(null));
		}

		@Test
		void failedValidationException() {
			final String invalidElectionEventId = "invalid";

			assertThrows(FailedValidationException.class, () -> electionEventService.exists(invalidElectionEventId));
		}
	}

}
