/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("A GenKeysCCRContext with")
class GenKeysCCRContextTest {

	private static final int PSI_SUP = MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
	private static final int PSI_MAX = RandomFactory.createRandom().genRandomInteger(PSI_SUP - 1) + 1;
	private static final int NODE_ID = 1;
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();

	private static final GqGroup encryptionGroup = GroupTestData.getLargeGqGroup();

	@Test
	@DisplayName("null parameters throws NullPointerException")
	void nullParametersThrows() {
		assertThrows(NullPointerException.class, () -> new GenKeysCCRContext(null, NODE_ID, ELECTION_EVENT_ID, PSI_MAX));
		assertThrows(NullPointerException.class, () -> new GenKeysCCRContext(encryptionGroup, NODE_ID, null, PSI_MAX));
	}

	@Test
	@DisplayName("invalid election event id throws FailedValidationException")
	void invalidElectionEventId() {
		assertThrows(FailedValidationException.class, () -> new GenKeysCCRContext(encryptionGroup, NODE_ID, "123", PSI_MAX));
	}

	@Test
	@DisplayName("zero maximum number of selections throws IllegalArgumentException")
	void nullMaximumNumberOfSelectionsThrows() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenKeysCCRContext(encryptionGroup, NODE_ID, ELECTION_EVENT_ID, 0));
		final String message = String.format("The maximum number of selections must be greater or equal to 1. [psi_max: %s]", 0);
		assertEquals(message, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("negative maximum number of selections throws IllegalArgumentException")
	void negativeMaximumNumberOfSelectionsThrows() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenKeysCCRContext(encryptionGroup, NODE_ID, ELECTION_EVENT_ID, -2));
		final String message = String.format("The maximum number of selections must be greater or equal to 1. [psi_max: %s]", -2);
		assertEquals(message, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("too big maximum number of selections throws IllegalArgumentException")
	void tooBigMaximumNumberOfSelectionsThrows() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenKeysCCRContext(encryptionGroup, NODE_ID, ELECTION_EVENT_ID, PSI_SUP + 1));
		final String message = String.format(
				"The maximum number of selections must be smaller or equal to the maximum supported number of selections. [psi_max: %s, psi_sup: %s]",
				PSI_SUP + 1, PSI_SUP);
		assertEquals(message, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("invalid node id throws IllegalArgumentException")
	void invalidNodeIdThrows() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenKeysCCRContext(encryptionGroup, 5, ELECTION_EVENT_ID, PSI_MAX));
		final String message = String.format(
				"The node id must be part of the known node ids. [nodeId: %s]", 5);
		assertEquals(message, Throwables.getRootCause(exception).getMessage());
	}
}
