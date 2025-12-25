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
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class UsedVotingCardDtoTest {

	private String electionEventId;
	private String verificationCardSetId;
	private String verificationCardId;
	private String votingCardId;
	private VerificationCardState verificationCardState;
	private LocalDateTime votingCardStateDate;

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();
		verificationCardId = uuidGenerator.generate();
		votingCardId = uuidGenerator.generate();
		verificationCardState = VerificationCardState.INITIAL;
		votingCardStateDate = LocalDateTimeUtils.now();
	}

	@Test
	void constructWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> new UsedVotingCardDto(null, verificationCardSetId, verificationCardId, votingCardId,
				verificationCardState, votingCardStateDate));
		assertThrows(NullPointerException.class,
				() -> new UsedVotingCardDto(electionEventId, null, verificationCardId, votingCardId, verificationCardState, votingCardStateDate));
		assertThrows(NullPointerException.class, () -> new UsedVotingCardDto(electionEventId, verificationCardSetId, null, votingCardId,
				verificationCardState, votingCardStateDate));
		assertThrows(NullPointerException.class, () -> new UsedVotingCardDto(electionEventId, verificationCardSetId, verificationCardId, null,
				verificationCardState, votingCardStateDate));
		assertThrows(NullPointerException.class,
				() -> new UsedVotingCardDto(electionEventId, verificationCardSetId, verificationCardId, votingCardId, null, votingCardStateDate));
		assertThrows(NullPointerException.class, () -> new UsedVotingCardDto(electionEventId, verificationCardSetId, verificationCardId, votingCardId,
				verificationCardState, null));
	}

	@Test
	void constructWithInvalidUuidsThrows() {
		assertThrows(FailedValidationException.class, () -> new UsedVotingCardDto("badId", verificationCardSetId, verificationCardId, votingCardId,
				verificationCardState, votingCardStateDate));
		assertThrows(FailedValidationException.class, () -> new UsedVotingCardDto(electionEventId, "badId", verificationCardId, votingCardId,
				verificationCardState, votingCardStateDate));
		assertThrows(FailedValidationException.class, () -> new UsedVotingCardDto(electionEventId, verificationCardSetId, "badId", votingCardId,
				verificationCardState, votingCardStateDate));
		assertThrows(FailedValidationException.class, () -> new UsedVotingCardDto(electionEventId, verificationCardSetId, verificationCardId, "badId",
				verificationCardState, votingCardStateDate));
	}

	@Test
	void constructWithValidArgumentsDoesNotThrow() {
		final UsedVotingCardDto usedVotingCardDTO = assertDoesNotThrow(
				() -> new UsedVotingCardDto(electionEventId, verificationCardSetId, verificationCardId, votingCardId, verificationCardState,
						votingCardStateDate));
		assertEquals(electionEventId, usedVotingCardDTO.electionEventId());
		assertEquals(verificationCardSetId, usedVotingCardDTO.verificationCardSetId());
		assertEquals(verificationCardId, usedVotingCardDTO.verificationCardId());
		assertEquals(votingCardId, usedVotingCardDTO.votingCardId());
		assertEquals(verificationCardState, usedVotingCardDTO.verificationCardState());
		assertEquals(votingCardStateDate, usedVotingCardDTO.votingCardStateDate());
	}
}
