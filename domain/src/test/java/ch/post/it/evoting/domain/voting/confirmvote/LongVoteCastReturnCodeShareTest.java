/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

package ch.post.it.evoting.domain.voting.confirmvote;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class LongVoteCastReturnCodeShareTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private final GqGroup gqGroup = GroupTestData.getGqGroup();
	private final GqGroupGenerator gqGroupGenerator = new GqGroupGenerator(gqGroup);
	private final GqElement longVoteCastReturnCodeShare = gqGroupGenerator.genMember();
	private final String electionEventId = uuidGenerator.generate();
	private final String verificationCardSetId = uuidGenerator.generate();
	private final String verificationCardId = uuidGenerator.generate();
	private final int nodeId = 1;

	@Nested
	@DisplayName("Check constructor validation")
	class CheckConstructor {
		@Test
		@DisplayName("Check null arguments")
		void nullArgs() {

			assertAll(
					() -> assertThrows(NullPointerException.class,
							() -> new LongVoteCastReturnCodeShare(null, verificationCardSetId, verificationCardId, nodeId,
									longVoteCastReturnCodeShare)),
					() -> assertThrows(NullPointerException.class,
							() -> new LongVoteCastReturnCodeShare(electionEventId, null, verificationCardId, nodeId,
									longVoteCastReturnCodeShare)),
					() -> assertThrows(NullPointerException.class,
							() -> new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId, null, nodeId,
									longVoteCastReturnCodeShare)),
					() -> assertThrows(NullPointerException.class,
							() -> new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, nodeId,
									null)),

					() -> assertDoesNotThrow(
							() -> new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, nodeId,
									longVoteCastReturnCodeShare))
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
							() -> new LongVoteCastReturnCodeShare(invalidElectionEventId, verificationCardSetId, verificationCardId, nodeId,
									longVoteCastReturnCodeShare)),
					() -> assertThrows(FailedValidationException.class,
							() -> new LongVoteCastReturnCodeShare(electionEventId, invalidVerificationCardSetId, verificationCardId, nodeId,
									longVoteCastReturnCodeShare)),
					() -> assertThrows(FailedValidationException.class,
							() -> new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId, invalidVerificationCardId, nodeId,
									longVoteCastReturnCodeShare))
			);
		}

		@Test
		@DisplayName("Check nodeIds in range 1 to 4")
		void nodeIsArgs() {

			assertAll(
					() -> assertThrows(IllegalArgumentException.class,
							() -> new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, 0,
									longVoteCastReturnCodeShare)),
					() -> assertDoesNotThrow(() -> new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, 1,
							longVoteCastReturnCodeShare)),
					() -> assertDoesNotThrow(() -> new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, 2,
							longVoteCastReturnCodeShare)),
					() -> assertDoesNotThrow(() -> new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, 3,
							longVoteCastReturnCodeShare)),
					() -> assertDoesNotThrow(() -> new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, 4,
							longVoteCastReturnCodeShare)),
					() -> assertThrows(IllegalArgumentException.class,
							() -> new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, 5,
									longVoteCastReturnCodeShare))
			);
		}

		@Test
		@DisplayName("Check all groups are of the same order")
		void checkGroupAreSameOrder() {
			final GqGroup differentGqGroup = GroupTestData.getDifferentGqGroup(gqGroup);
			final GqGroupGenerator gqDifferentGroupGenerator = new GqGroupGenerator(differentGqGroup);

			final GqElement longVoteCastReturnCodeShareDifferentGroup = gqDifferentGroupGenerator.genMember();

			assertThrows(IllegalArgumentException.class,
					() -> new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, 0,
							longVoteCastReturnCodeShareDifferentGroup));
		}
	}

}
