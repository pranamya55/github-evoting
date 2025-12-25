/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.votingcardmanagement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class ElectionEventDtoTest {

	private String electionEventId;
	private String electionEventAlias;
	private String electionEventDescription;
	private LocalDateTime startTime;
	private LocalDateTime finishTime;

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		electionEventAlias = electionEventId + "_alias";
		electionEventDescription = electionEventId + "_description";
		startTime = LocalDateTimeUtils.now();
		finishTime = startTime.plusDays(1);
	}

	@Test
	void constructWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class,
				() -> new ElectionEventDto(null, electionEventAlias, electionEventDescription, startTime, finishTime));
		assertThrows(NullPointerException.class, () -> new ElectionEventDto(electionEventId, null, electionEventDescription, startTime, finishTime));
		assertThrows(NullPointerException.class, () -> new ElectionEventDto(electionEventId, electionEventAlias, null, startTime, finishTime));
		assertThrows(NullPointerException.class,
				() -> new ElectionEventDto(electionEventId, electionEventAlias, electionEventDescription, null, finishTime));
		assertThrows(NullPointerException.class,
				() -> new ElectionEventDto(electionEventId, electionEventAlias, electionEventDescription, startTime, null));
	}

	@Test
	void constructWithNonUuidElectionEventIdThrows() {
		assertThrows(FailedValidationException.class, () -> new ElectionEventDto("xyz", electionEventAlias, electionEventDescription,
				startTime, finishTime));
	}

	@Test
	void constructWithAliasOrDescriptionNotValidUcsThrows() {
		assertThrows(FailedValidationException.class, () -> new ElectionEventDto(electionEventId, "\uDFFF", electionEventDescription,
				startTime, finishTime));
		assertThrows(FailedValidationException.class, () -> new ElectionEventDto(electionEventId, electionEventAlias, "\uDFFF",
				startTime, finishTime));
	}

	@Test
	void constructWithValidArgumentsDoesNotThrow() {
		final ElectionEventDto electionEventDTO = assertDoesNotThrow(
				() -> new ElectionEventDto(electionEventId, electionEventAlias, electionEventDescription, startTime,
						finishTime));
		assertEquals(electionEventId, electionEventDTO.electionEventId());
		assertEquals(electionEventAlias, electionEventDTO.electionEventAlias());
		assertEquals(electionEventDescription, electionEventDTO.electionEventDescription());
	}

	@Test
	void constructWithInvalidDatesThrows() {
		assertThrows(IllegalArgumentException.class, () -> new ElectionEventDto(electionEventId, electionEventAlias,
				electionEventDescription, finishTime, startTime));
	}
}
