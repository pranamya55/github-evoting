/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("a GenEncLongCodeSharesContext built with")
class GenEncLongCodeSharesContextTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final GqGroup GQ_GROUP = GroupTestData.getGqGroup();
	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_SET_ID = uuidGenerator.generate();
	private static final ImmutableList<String> VERIFICATION_CARD_IDS = ImmutableList.of(
			uuidGenerator.generate(),
			uuidGenerator.generate(),
			uuidGenerator.generate());

	private static final int NODE_ID = 1;

	private static int numberOfVotingOptions;

	@BeforeAll
	static void setupAll() {
		numberOfVotingOptions = new SecureRandom().nextInt(5) + 1;
	}

	@Test
	@DisplayName("null encryption group throws NullPointerException")
	void nullEncryptionGroup() {
		final GenEncLongCodeSharesContext.Builder builder = new GenEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(null)
				.setNodeId(NODE_ID)
				.setElectionEventId(ELECTION_EVENT_ID)
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID)
				.setVerificationCardIds(VERIFICATION_CARD_IDS)
				.setNumberOfVotingOptions(numberOfVotingOptions);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("invalid node id throws IllegalArgumentException")
	void invalidNodeId() {
		final int invalidNodeId = 0;
		final GenEncLongCodeSharesContext.Builder builder = new GenEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setNodeId(invalidNodeId)
				.setElectionEventId(ELECTION_EVENT_ID)
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID)
				.setVerificationCardIds(VERIFICATION_CARD_IDS)
				.setNumberOfVotingOptions(numberOfVotingOptions);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String errorMessage = String.format("The node id must be part of the known node ids. [nodeId: %s]", invalidNodeId);
		assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("null election event id throws NullPointerException")
	void nullElectionEventId() {
		final GenEncLongCodeSharesContext.Builder builder = new GenEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setNodeId(NODE_ID)
				.setElectionEventId(null)
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID)
				.setVerificationCardIds(VERIFICATION_CARD_IDS)
				.setNumberOfVotingOptions(numberOfVotingOptions);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("invalid election event id throws FailedValidationException")
	void invalidElectionEventId() {
		GenEncLongCodeSharesContext.Builder builder = new GenEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setNodeId(NODE_ID)
				.setElectionEventId("")
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID)
				.setVerificationCardIds(VERIFICATION_CARD_IDS)
				.setNumberOfVotingOptions(numberOfVotingOptions);

		assertThrows(FailedValidationException.class, builder::build);

		builder = builder.setElectionEventId("0b88257ec32142b");
		assertThrows(FailedValidationException.class, builder::build);
	}

	@Test
	@DisplayName("null verification card set id throws NullPointerException")
	void nullVerificationCardSetId() {
		final GenEncLongCodeSharesContext.Builder builder = new GenEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setNodeId(NODE_ID)
				.setElectionEventId(ELECTION_EVENT_ID)
				.setVerificationCardSetId(null)
				.setVerificationCardIds(VERIFICATION_CARD_IDS)
				.setNumberOfVotingOptions(numberOfVotingOptions);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("invalid verification card set id throws FailedValidationException")
	void invalidVerificationCardSetId() {
		GenEncLongCodeSharesContext.Builder builder = new GenEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setNodeId(NODE_ID)
				.setElectionEventId(ELECTION_EVENT_ID)
				.setVerificationCardSetId("")
				.setVerificationCardIds(VERIFICATION_CARD_IDS)
				.setNumberOfVotingOptions(numberOfVotingOptions);

		assertThrows(FailedValidationException.class, builder::build);

		builder = builder.setVerificationCardSetId("f1bd7b195d6f38");
		assertThrows(FailedValidationException.class, builder::build);
	}

	@Test
	@DisplayName("null verification card ids throws NullPointerException")
	void nullVerificationCardIds() {
		final GenEncLongCodeSharesContext.Builder builder = new GenEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setNodeId(NODE_ID)
				.setElectionEventId(ELECTION_EVENT_ID)
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID)
				.setVerificationCardIds(null)
				.setNumberOfVotingOptions(numberOfVotingOptions);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("non unique verification card ids throws IllegalArgumentException")
	void nonUniqueVerificationCardIds() {
		final ImmutableList<String> verificationCardIdsWithDuplicates = ImmutableList.of(VERIFICATION_CARD_IDS.get(0), VERIFICATION_CARD_IDS.get(1),
				VERIFICATION_CARD_IDS.get(0));

		final GenEncLongCodeSharesContext.Builder builder = new GenEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setNodeId(NODE_ID)
				.setElectionEventId(ELECTION_EVENT_ID)
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID)
				.setVerificationCardIds(verificationCardIdsWithDuplicates)
				.setNumberOfVotingOptions(numberOfVotingOptions);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("The list of verification card ids contains duplicated values.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("invalid number of voting options throws IllegalArgumentException")
	void invalidNumberOfVotingOptions() {
		final GenEncLongCodeSharesContext.Builder builder = new GenEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setNodeId(NODE_ID)
				.setElectionEventId(ELECTION_EVENT_ID)
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID)
				.setVerificationCardIds(VERIFICATION_CARD_IDS)
				.setNumberOfVotingOptions(0);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("The number of voting options must be strictly positive.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid parameters gives expected context")
	void validParameters() {
		final GenEncLongCodeSharesContext.Builder builder = new GenEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(GQ_GROUP)
				.setNodeId(NODE_ID)
				.setElectionEventId(ELECTION_EVENT_ID)
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID)
				.setVerificationCardIds(VERIFICATION_CARD_IDS)
				.setNumberOfVotingOptions(numberOfVotingOptions);

		assertDoesNotThrow(builder::build);
	}
}
