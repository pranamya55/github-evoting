/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class VerifyAuthenticationChallengeContextTest {

	private String electionEventId;
	private String credentialId;

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		credentialId = uuidGenerator.generate();
	}

	@Test
	void constructWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> new VerifyAuthenticationChallengeContext(null, credentialId));
		assertThrows(NullPointerException.class, () -> new VerifyAuthenticationChallengeContext(electionEventId, null));
	}

	@Test
	void constructWithElectionEventIdNotValidUuidThrows() {
		final String tooLongElectionEventId = electionEventId + "1";
		assertThrows(FailedValidationException.class, () -> new VerifyAuthenticationChallengeContext(tooLongElectionEventId, credentialId));
		final String tooShortElectionEventId = electionEventId.substring(1);
		assertThrows(FailedValidationException.class, () -> new VerifyAuthenticationChallengeContext(tooShortElectionEventId, credentialId));
		final String electionEventIdBadCharacter = "$" + electionEventId.substring(1);
		assertThrows(FailedValidationException.class,
				() -> new VerifyAuthenticationChallengeContext(electionEventIdBadCharacter, credentialId));
	}

	@Test
	void constructWithCredentialIdNotValidUuidThrows() {
		final String tooShortCredentialId = credentialId.substring(1);
		assertThrows(FailedValidationException.class,
				() -> new VerifyAuthenticationChallengeContext(electionEventId, tooShortCredentialId));
		final String tooLongCredentialId = credentialId + "1";
		assertThrows(FailedValidationException.class,
				() -> new VerifyAuthenticationChallengeContext(electionEventId, tooLongCredentialId));
		final String credentialIdBadCharacter = "?" + credentialId;
		assertThrows(FailedValidationException.class,
				() -> new VerifyAuthenticationChallengeContext(electionEventId, credentialIdBadCharacter));
	}

	@Test
	void constructWithValidInputsDoesNotThrow() {
		assertDoesNotThrow(() -> new VerifyAuthenticationChallengeContext(electionEventId, credentialId));
	}
}
