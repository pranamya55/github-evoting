/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("Building a MixDecOfflineContext with")
class MixDecOfflineContextTest {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final GqGroup GQ_GROUP = GroupTestData.getLargeGqGroup();

	private String electionEventId;
	private String ballotBoxId;
	private int numberOfAllowedWriteInsPlusOne;

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		ballotBoxId = uuidGenerator.generate();
		numberOfAllowedWriteInsPlusOne = RANDOM.nextInt(5) + 1;
	}

	@Test
	@DisplayName("null encryption group throws a NullPointerException")
	void mixDecOfflineContextWithNullEncryptionGroupThrows() {
		final MixDecOfflineContext.Builder builder = new MixDecOfflineContext.Builder()
				.setElectionEventId(electionEventId)
				.setBallotBoxId(ballotBoxId)
				.setNumberOfAllowedWriteInsPlusOne(numberOfAllowedWriteInsPlusOne);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("null election event id throws a NullPointerException")
	void mixDecOfflineContextWithNullElectionEventIdThrows() {
		final MixDecOfflineContext.Builder builder = new MixDecOfflineContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setBallotBoxId(ballotBoxId)
				.setNumberOfAllowedWriteInsPlusOne(numberOfAllowedWriteInsPlusOne);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("null ballot box id throws a NullPointerException")
	void mixDecOfflineContextWithNullBallotBoxIdThrows() {
		final MixDecOfflineContext.Builder builder = new MixDecOfflineContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setElectionEventId(electionEventId)
				.setNumberOfAllowedWriteInsPlusOne(numberOfAllowedWriteInsPlusOne);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("an invalid electionEventId throws a FailedValidationException")
	void mixDecOfflineContextWithElectionEventIdNotUUIDThrows() {
		final String nonUUIDElectionEventId = "no UUID";
		final MixDecOfflineContext.Builder builder = new MixDecOfflineContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setElectionEventId(nonUUIDElectionEventId)
				.setBallotBoxId(ballotBoxId)
				.setNumberOfAllowedWriteInsPlusOne(numberOfAllowedWriteInsPlusOne);
		assertThrows(FailedValidationException.class, builder::build);
	}

	@Test
	@DisplayName("an invalid ballotBoxId throws a FailedValidationException")
	void mixDecOfflineContextWithBallotBoxIdNotUUIDThrows() {
		final String nonUUIDBallotBoxId = "no UUID";
		final MixDecOfflineContext.Builder builder = new MixDecOfflineContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setElectionEventId(electionEventId)
				.setBallotBoxId(nonUUIDBallotBoxId)
				.setNumberOfAllowedWriteInsPlusOne(numberOfAllowedWriteInsPlusOne);
		assertThrows(FailedValidationException.class, builder::build);
	}

	@Test
	@DisplayName("number of write-ins + 1 less than 1 throws an IllegalArgumentException")
	void mixDecOfflineContextWithTooSmallNumberOfAllowedWriteInsPlusOneThrows() {
		final int tooSmallNumberOfWriteInsPlusOne = 0;
		final MixDecOfflineContext.Builder builder = new MixDecOfflineContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setElectionEventId(electionEventId)
				.setBallotBoxId(ballotBoxId)
				.setNumberOfAllowedWriteInsPlusOne(tooSmallNumberOfWriteInsPlusOne);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals(String.format("The number of allowed write-ins + 1 must be greater than or equal to 1. [delta: %s]",
				tooSmallNumberOfWriteInsPlusOne), Throwables.getRootCause(exception).getMessage());
	}

}
