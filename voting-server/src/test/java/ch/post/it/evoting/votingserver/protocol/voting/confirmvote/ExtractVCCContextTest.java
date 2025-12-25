/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.confirmvote;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("ExtractVCCContext constructed with")
class ExtractVCCContextTest {

	private String electionEventId;
	private String verificationCardId;
	private GqGroup encryptionGroup;

	@BeforeEach
	void setUp() {
		encryptionGroup = GroupTestData.getGqGroup();

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		verificationCardId = uuidGenerator.generate();
	}

	@Test
	@DisplayName("any null parameter throws NullPointerException")
	void nullParamsThrows() {
		assertThrows(NullPointerException.class, () -> new ExtractVCCContext(null, electionEventId, verificationCardId));
		assertThrows(NullPointerException.class, () -> new ExtractVCCContext(encryptionGroup, null, verificationCardId));
		assertThrows(NullPointerException.class, () -> new ExtractVCCContext(encryptionGroup, electionEventId, null));
	}

	@Test
	@DisplayName("invalid election event id throws FailedValidationException")
	void invalidElectionEventIdThrows() {
		assertThrows(FailedValidationException.class, () -> new ExtractVCCContext(encryptionGroup, "invalid", verificationCardId));
	}

	@Test
	@DisplayName("invalid verification card id throws FailedValidationException")
	void invalidVerificationCardIdThrows() {
		assertThrows(FailedValidationException.class, () -> new ExtractVCCContext(encryptionGroup, electionEventId, "invalid"));
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParamsDoesNotThrow() {
		assertDoesNotThrow(() -> new ExtractVCCContext(encryptionGroup, electionEventId, verificationCardId));
	}

}