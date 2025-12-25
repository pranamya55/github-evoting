/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@SpringBootTest
@ContextConfiguration(initializers = TestKeyStoreInitializer.class)
@ActiveProfiles("test")
@DisplayName("A verificationCardSetService")
class VerificationCardSetServiceIT {
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_SET_ID = uuidGenerator.generate();
	private static final String ALREADY_SET_VERIFICATION_CARD_SET_ID = uuidGenerator.generate();

	@Autowired
	private VerificationCardSetService verificationCardSetService;

	@BeforeAll
	static void setUpElection(
			@Autowired
			final ElectionEventService electionEventService,
			@Autowired
			final VerificationCardSetRepository verificationCardSetRepository) {

		final GqGroup encryptionGroup = GroupTestData.getGqGroup();

		// Save election event.
		final ElectionEventEntity savedElectionEventEntity = electionEventService.save(ELECTION_EVENT_ID, encryptionGroup);

		// Create and save verification card sets
		final VerificationCardSetEntity verificationCardSet1 = new VerificationCardSetEntity.Builder()
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID)
				.setVerificationCardSetAlias("alias-" + VERIFICATION_CARD_SET_ID)
				.setVerificationCardSetDescription("Description " + VERIFICATION_CARD_SET_ID)
				.setDomainsOfInfluence(ImmutableList.of("domain1", "domain2"))
				.setElectionEventEntity(savedElectionEventEntity)
				.build();

		final VerificationCardSetEntity verificationCardSet2 = new VerificationCardSetEntity.Builder()
				.setVerificationCardSetId(ALREADY_SET_VERIFICATION_CARD_SET_ID)
				.setVerificationCardSetAlias("alias-" + ALREADY_SET_VERIFICATION_CARD_SET_ID)
				.setVerificationCardSetDescription("Description " + ALREADY_SET_VERIFICATION_CARD_SET_ID)
				.setDomainsOfInfluence(ImmutableList.of("domain1", "domain2"))
				.setElectionEventEntity(savedElectionEventEntity)
				.build();

		verificationCardSetRepository.saveAll(ImmutableList.of(verificationCardSet1, verificationCardSet2));
	}

	@DisplayName("setting a long vote cast return codes allow list behaves as expected.")
	@Test
	void happyPathTest() {
		final ImmutableList<String> longVoteCastReturnCodesAllowList = ImmutableList.of("A", "B", "C");

		assertDoesNotThrow(
				() -> verificationCardSetService.setLongVoteCastReturnCodesAllowList(VERIFICATION_CARD_SET_ID, longVoteCastReturnCodesAllowList));

		assertEquals(longVoteCastReturnCodesAllowList, verificationCardSetService.getLongVoteCastReturnCodesAllowList(VERIFICATION_CARD_SET_ID));
	}

	@DisplayName("setting with an invalid input throws.")
	@Test
	void invalidInputValidationTest() {
		final ImmutableList<String> longVoteCastReturnCodesAllowList = ImmutableList.emptyList();

		assertAll(
				() -> assertThrows(NullPointerException.class,
						() -> verificationCardSetService.setLongVoteCastReturnCodesAllowList(null, longVoteCastReturnCodesAllowList)),
				() -> assertThrows(FailedValidationException.class,
						() -> verificationCardSetService.setLongVoteCastReturnCodesAllowList("invalidVerificationCardSetId",
								longVoteCastReturnCodesAllowList)),
				() -> assertThrows(NullPointerException.class,
						() -> verificationCardSetService.setLongVoteCastReturnCodesAllowList(VERIFICATION_CARD_SET_ID, null))
		);
	}

	@DisplayName("setting a long vote cast return codes allow list with non-matching verification card set ids throws.")
	@Test
	void nonMatchingVerificationCardSetThrows() {
		final String verificationCardSetId = uuidGenerator.generate();
		final ImmutableList<String> longVoteCastReturnCodesAllowList = ImmutableList.emptyList();

		final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
				() -> verificationCardSetService.setLongVoteCastReturnCodesAllowList(verificationCardSetId, longVoteCastReturnCodesAllowList));

		assertEquals(String.format("Could not find any matching verification card set [verificationCardSetId: %s]", verificationCardSetId),
				illegalStateException.getMessage());

	}

	@DisplayName("setting a long vote cast return codes allow list on a verification card set already containing throws.")
	@Test
	void alreadyExistsInputValidationTest() {
		final Random random = RandomFactory.createRandom();
		final Alphabet base64Alphabet = Base64Alphabet.getInstance();
		final String longVoteCastReturnCode = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		final ImmutableList<String> longVoteCastReturnCodesAllowList = ImmutableList.of(longVoteCastReturnCode);
		verificationCardSetService.setLongVoteCastReturnCodesAllowList(ALREADY_SET_VERIFICATION_CARD_SET_ID, longVoteCastReturnCodesAllowList);

		assertThrows(PersistenceException.class,
				() -> verificationCardSetService.setLongVoteCastReturnCodesAllowList(ALREADY_SET_VERIFICATION_CARD_SET_ID,
						longVoteCastReturnCodesAllowList));
	}

}
