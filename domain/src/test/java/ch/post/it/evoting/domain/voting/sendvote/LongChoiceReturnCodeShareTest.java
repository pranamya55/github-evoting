/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

package ch.post.it.evoting.domain.voting.sendvote;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class LongChoiceReturnCodeShareTest extends TestGroupSetup {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private final String electionEventId = uuidGenerator.generate();
	private final String verificationCardSetId = uuidGenerator.generate();
	private final String verificationCardId = uuidGenerator.generate();
	private final int nodeId = 1;
	private final GroupVector<GqElement, GqGroup> longChoiceReturnCodeShare = gqGroupGenerator.genRandomGqElementVector(2);

	@Test
	@DisplayName("Check null arguments")
	void nullArgs() {
		assertAll(
				() -> assertThrows(NullPointerException.class,
						() -> new LongChoiceReturnCodeShare(null, verificationCardSetId, verificationCardId, nodeId,
								longChoiceReturnCodeShare)),
				() -> assertThrows(NullPointerException.class,
						() -> new LongChoiceReturnCodeShare(electionEventId, null, verificationCardId, nodeId,
								longChoiceReturnCodeShare)),
				() -> assertThrows(NullPointerException.class,
						() -> new LongChoiceReturnCodeShare(electionEventId, verificationCardSetId, null, nodeId,
								longChoiceReturnCodeShare)),
				() -> assertThrows(NullPointerException.class,
						() -> new LongChoiceReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, nodeId,
								null)),
				() -> assertDoesNotThrow(
						() -> new LongChoiceReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, nodeId,
								longChoiceReturnCodeShare))
		);
	}

	@Test
	@DisplayName("Check UUID format")
	void formatArgs() {
		final String invalidElectionEventId = "electionEventId";
		final String invalidVerificationCardSetId = "verificationCardSetId";
		final String invalidVerificationCardId = "verificationCardId";

		assertAll(
				() -> assertThrows(FailedValidationException.class,
						() -> new LongChoiceReturnCodeShare(invalidElectionEventId, verificationCardSetId, verificationCardId, nodeId,
								longChoiceReturnCodeShare)),
				() -> assertThrows(FailedValidationException.class,
						() -> new LongChoiceReturnCodeShare(electionEventId, invalidVerificationCardSetId, verificationCardId, nodeId,
								longChoiceReturnCodeShare)),
				() -> assertThrows(FailedValidationException.class,
						() -> new LongChoiceReturnCodeShare(electionEventId, verificationCardSetId, invalidVerificationCardId, nodeId,
								longChoiceReturnCodeShare))
		);
	}

	@Test
	@DisplayName("Check nodeIds in range 1 to 4")
	void nodeIsArgs() {
		assertAll(
				() -> assertThrows(IllegalArgumentException.class,
						() -> new LongChoiceReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, 0,
								longChoiceReturnCodeShare)),
				() -> assertDoesNotThrow(() -> new LongChoiceReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, 1,
						longChoiceReturnCodeShare)),
				() -> assertDoesNotThrow(() -> new LongChoiceReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, 2,
						longChoiceReturnCodeShare)),
				() -> assertDoesNotThrow(() -> new LongChoiceReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, 3,
						longChoiceReturnCodeShare)),
				() -> assertDoesNotThrow(() -> new LongChoiceReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, 4,
						longChoiceReturnCodeShare)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> new LongChoiceReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, 5,
								longChoiceReturnCodeShare))
		);
	}

}
