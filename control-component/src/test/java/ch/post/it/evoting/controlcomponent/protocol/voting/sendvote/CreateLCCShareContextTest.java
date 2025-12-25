/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("Construct CreateLCCShareContextTest with")
class CreateLCCShareContextTest {

	private GqGroup encryptionGroup;
	private String electionEventId;
	private String verificationCardSetId;
	private String verificationCardId;
	private ImmutableList<String> blankCorrectnessInformation;

	@BeforeEach
	void setup() {
		final SecureRandom secureRandom = new SecureRandom();
		final int psi = secureRandom.nextInt(1, 5);

		encryptionGroup = GroupTestData.getLargeGqGroup();

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();
		verificationCardId = uuidGenerator.generate();

		blankCorrectnessInformation = ElectionSetupUtils.genBlankCorrectnessInformation(psi);
	}

	@Test
	@DisplayName("Happy path")
	void happyPath() {
		final CreateLCCShareContext createLCCShareContext = assertDoesNotThrow(
				() -> new CreateLCCShareContext(encryptionGroup, 1, electionEventId, verificationCardSetId, verificationCardId,
						blankCorrectnessInformation));
		final ImmutableList<String> resultBlankCorrectnessInformation = createLCCShareContext.blankCorrectnessInformation();
		assertEquals(blankCorrectnessInformation.size(), resultBlankCorrectnessInformation.size());
	}

	@Test
	@DisplayName("any null parameters throws NullPointerException")
	void constructWithNullParametersThrows() {
		assertThrows(NullPointerException.class,
				() -> new CreateLCCShareContext(null, 1, electionEventId, verificationCardSetId, verificationCardId, blankCorrectnessInformation));
		assertThrows(NullPointerException.class,
				() -> new CreateLCCShareContext(encryptionGroup, 1, null, verificationCardSetId, verificationCardId, blankCorrectnessInformation));
		assertThrows(NullPointerException.class,
				() -> new CreateLCCShareContext(encryptionGroup, 1, electionEventId, null, verificationCardId, blankCorrectnessInformation));
		assertThrows(NullPointerException.class,
				() -> new CreateLCCShareContext(encryptionGroup, 1, electionEventId, verificationCardSetId, verificationCardId, null));
	}

	@Test
	@DisplayName("any id not a UUID throws FailedValidationException")
	void constructWithNonUuidThrows() {
		assertThrows(FailedValidationException.class,
				() -> new CreateLCCShareContext(encryptionGroup, 1, electionEventId + "bad", verificationCardSetId, verificationCardId,
						blankCorrectnessInformation));
		assertThrows(FailedValidationException.class,
				() -> new CreateLCCShareContext(encryptionGroup, 1, electionEventId, verificationCardSetId + "bad", verificationCardId,
						blankCorrectnessInformation));
		assertThrows(FailedValidationException.class,
				() -> new CreateLCCShareContext(encryptionGroup, 1, electionEventId, verificationCardSetId, verificationCardId + "bad",
						blankCorrectnessInformation));
	}

	@Test
	@DisplayName("node id not a valid id throws IllegalArgumentException")
	void constructWithNonValidNodeIdThrows() {
		assertThrows(IllegalArgumentException.class,
				() -> new CreateLCCShareContext(encryptionGroup, 0, electionEventId, verificationCardSetId, verificationCardId,
						blankCorrectnessInformation));
		final int tooHighNodeId = ControlComponentNode.ids().size() + 1;
		assertThrows(IllegalArgumentException.class,
				() -> new CreateLCCShareContext(encryptionGroup, tooHighNodeId, electionEventId, verificationCardSetId, verificationCardId,
						blankCorrectnessInformation));
	}

	@Test
	@DisplayName("wrong number of blank correctness information throws IllegalArgumentException")
	void constructWithWrongNumberOfBlankCorrectnessInformationThrows() {
		// Too few.
		final ImmutableList<String> emptyBlankCorrectnessInformation = ImmutableList.emptyList();
		assertThrows(IllegalArgumentException.class,
				() -> new CreateLCCShareContext(encryptionGroup, 1, electionEventId, verificationCardSetId, verificationCardId,
						emptyBlankCorrectnessInformation));
		// Too many.
		// Too many.
		final ImmutableList<String> tooManyBlankCorrectnessInformation = ElectionSetupUtils.genBlankCorrectnessInformation(
				MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS + 1);
		assertThrows(IllegalArgumentException.class,
				() -> new CreateLCCShareContext(encryptionGroup, 1, electionEventId, verificationCardSetId, verificationCardId,
						tooManyBlankCorrectnessInformation));
	}

}
