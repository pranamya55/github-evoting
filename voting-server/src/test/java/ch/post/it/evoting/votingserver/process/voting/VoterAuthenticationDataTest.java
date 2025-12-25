/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class VoterAuthenticationDataTest {

	private String electionEventId;
	private String verificationCardSetId;
	private String ballotBoxId;
	private String verificationCardId;
	private String votingCardId;
	private String credentialId;

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();
		ballotBoxId = uuidGenerator.generate();
		verificationCardId = uuidGenerator.generate();
		votingCardId = uuidGenerator.generate();
		credentialId = uuidGenerator.generate();
	}

	@Test
	void constructWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class,
				() -> new VoterAuthenticationData(null, verificationCardSetId, ballotBoxId, verificationCardId, votingCardId,
						credentialId));
		assertThrows(NullPointerException.class,
				() -> new VoterAuthenticationData(electionEventId, null, ballotBoxId, verificationCardId, votingCardId,
						credentialId));
		assertThrows(NullPointerException.class,
				() -> new VoterAuthenticationData(electionEventId, verificationCardSetId, null, verificationCardId, votingCardId,
						credentialId));
		assertThrows(NullPointerException.class,
				() -> new VoterAuthenticationData(electionEventId, verificationCardSetId, ballotBoxId, null, votingCardId,
						credentialId));
		assertThrows(NullPointerException.class,
				() -> new VoterAuthenticationData(electionEventId, verificationCardSetId, ballotBoxId, verificationCardId, null,
						credentialId));
		assertThrows(NullPointerException.class,
				() -> new VoterAuthenticationData(electionEventId, verificationCardSetId, ballotBoxId, verificationCardId,
						votingCardId, null));
	}

	@Test
	void constructWithNonUuidArgumentsThrows() {
		final String nonUuid = "this is not a UUID";
		assertThrows(FailedValidationException.class,
				() -> new VoterAuthenticationData(nonUuid, verificationCardSetId, ballotBoxId, verificationCardId, votingCardId,
						credentialId));
		assertThrows(FailedValidationException.class,
				() -> new VoterAuthenticationData(electionEventId, nonUuid, ballotBoxId, verificationCardId, votingCardId,
						credentialId));
		assertThrows(FailedValidationException.class,
				() -> new VoterAuthenticationData(electionEventId, verificationCardSetId, nonUuid, verificationCardId, votingCardId,
						credentialId));
		assertThrows(FailedValidationException.class,
				() -> new VoterAuthenticationData(electionEventId, verificationCardSetId, ballotBoxId, nonUuid, votingCardId,
						credentialId));
		assertThrows(FailedValidationException.class,
				() -> new VoterAuthenticationData(electionEventId, verificationCardSetId, ballotBoxId, verificationCardId, nonUuid,
						credentialId));
		assertThrows(FailedValidationException.class,
				() -> new VoterAuthenticationData(electionEventId, verificationCardSetId, ballotBoxId, verificationCardId,
						votingCardId, nonUuid));
	}

	@Test
	void constructWithValidInputInstantiatesObject() {
		final VoterAuthenticationData voterAuthenticationData = assertDoesNotThrow(
				() -> new VoterAuthenticationData(electionEventId, verificationCardSetId, ballotBoxId, verificationCardId, votingCardId,
						credentialId));

		assertEquals(electionEventId, voterAuthenticationData.electionEventId());
		assertEquals(verificationCardSetId, voterAuthenticationData.verificationCardSetId());
		assertEquals(ballotBoxId, voterAuthenticationData.ballotBoxId());
		assertEquals(verificationCardId, voterAuthenticationData.verificationCardId());
		assertEquals(votingCardId, voterAuthenticationData.votingCardId());
		assertEquals(credentialId, voterAuthenticationData.credentialId());
	}
}
