/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.sendvote;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("ExtractCRCContext constructed with")
class ExtractCRCContextTest {

	private static final SecureRandom secureRandom = new SecureRandom();

	private String electionEventId;
	private String verificationCardId;
	private GqGroup encryptionGroup;
	private ImmutableList<String> blankCorrectnessInformation;

	@BeforeEach
	void setUp() {
		encryptionGroup = GroupTestData.getGqGroup();

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		verificationCardId = uuidGenerator.generate();

		final int psi = secureRandom.nextInt(1, 5);
		blankCorrectnessInformation = ElectionSetupUtils.genBlankCorrectnessInformation(psi);
	}

	@Test
	@DisplayName("any null parameter throws NullPointerException")
	void nullParamsThrows() {
		assertThrows(NullPointerException.class, () -> new ExtractCRCContext(null, electionEventId, verificationCardId, blankCorrectnessInformation));
		assertThrows(NullPointerException.class, () -> new ExtractCRCContext(encryptionGroup, null, verificationCardId, blankCorrectnessInformation));
		assertThrows(NullPointerException.class, () -> new ExtractCRCContext(encryptionGroup, electionEventId, null, blankCorrectnessInformation));
		assertThrows(NullPointerException.class, () -> new ExtractCRCContext(encryptionGroup, electionEventId, verificationCardId, null));
	}

	@Test
	@DisplayName("invalid election event id throws FailedValidationException")
	void invalidElectionEventIdThrows() {
		assertThrows(FailedValidationException.class,
				() -> new ExtractCRCContext(encryptionGroup, "invalid", verificationCardId, blankCorrectnessInformation));
	}

	@Test
	@DisplayName("invalid verification card id throws FailedValidationException")
	void invalidVerificationCardIdThrows() {
		assertThrows(FailedValidationException.class,
				() -> new ExtractCRCContext(encryptionGroup, electionEventId, "invalid", blankCorrectnessInformation));
	}

	@Test
	@DisplayName("blank correctness information not in range throws IllegalArgumentException")
	void notRangeBlankCorrectnessInformationThrows() {
		final String errorMessage = String.format("The blank correctness information size must be in range [1, %s].",
				MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);

		// Too few.
		final ImmutableList<String> emptyBlankCorrectnessInformation = ImmutableList.emptyList();

		final IllegalArgumentException tooFewException = assertThrows(IllegalArgumentException.class,
				() -> new ExtractCRCContext(encryptionGroup, electionEventId, verificationCardId, emptyBlankCorrectnessInformation));
		assertEquals(errorMessage, Throwables.getRootCause(tooFewException).getMessage());

		// Too many.
		final ImmutableList<String> tooManyBlankCorrectnessInformation = ElectionSetupUtils.genBlankCorrectnessInformation(
				MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS + 1);

		final IllegalArgumentException tooManyException = assertThrows(IllegalArgumentException.class,
				() -> new ExtractCRCContext(encryptionGroup, electionEventId, verificationCardId, tooManyBlankCorrectnessInformation));
		assertEquals(errorMessage, Throwables.getRootCause(tooManyException).getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParamsDoesNotThrow() {
		assertDoesNotThrow(() -> new ExtractCRCContext(encryptionGroup, electionEventId, verificationCardId, blankCorrectnessInformation));
	}

}