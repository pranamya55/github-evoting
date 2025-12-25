/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("A GetMixnetInitialCiphertextsRequestPayload built with")
class GetMixnetInitialCiphertextsRequestPayloadTest {

	private static String electionEventId;
	private static String ballotBoxId;

	@BeforeEach
	void setUp() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		ballotBoxId = uuidGenerator.generate();
	}

	@Test
	@DisplayName("valid inputs does not throw.")
	void happyPath() {
		assertDoesNotThrow(() -> new GetMixnetInitialCiphertextsRequestPayload(electionEventId, ballotBoxId));
	}

	@Test
	@DisplayName("any null input throws a NullPointerException.")
	void nullInputs() {
		assertAll(
				() -> assertThrows(NullPointerException.class, () -> new GetMixnetInitialCiphertextsRequestPayload(null, ballotBoxId)),
				() -> assertThrows(NullPointerException.class, () -> new GetMixnetInitialCiphertextsRequestPayload(electionEventId, null)));
	}

	@Test
	@DisplayName("invalid election event id throws a FailedValidationException.")
	void invalidElectionEventId() {
		final String invalidElectionEventId = "invalid";
		assertThrows(FailedValidationException.class, () -> new GetMixnetInitialCiphertextsRequestPayload(invalidElectionEventId, ballotBoxId));
	}

	@Test
	@DisplayName("invalid ballot box id throws a FailedValidationException.")
	void invalidBallotBoxId() {
		final String invalidBallotBoxId = "invalid";
		assertThrows(FailedValidationException.class, () -> new GetMixnetInitialCiphertextsRequestPayload(electionEventId, invalidBallotBoxId));
	}
}
