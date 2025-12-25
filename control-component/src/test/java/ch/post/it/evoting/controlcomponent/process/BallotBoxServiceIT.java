/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.domain.multitenancy.TenantConstants.TEST_TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("BallotBoxService")
@ContextConfiguration(initializers = TestKeyStoreInitializer.class)
class BallotBoxServiceIT {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final String BAD_ID = "Bad Id";
	private static PrimesMappingTable primesMappingTable;

	@Autowired
	private BallotBoxService ballotBoxService;

	@Autowired
	private ElectionEventService electionEventService;

	@Autowired
	private VerificationCardSetService verificationCardSetService;

	@Autowired
	private ContextHolder contextHolder;

	// This test does not need keystore functionality
	@MockitoBean
	private SignatureKeystore<Alias> signatureKeystoreService;

	private String ballotBoxId;
	private String electionEventId;
	private String verificationCardSetId;
	private GqGroup encryptionGroup;
	private boolean testBallotBox;
	private LocalDateTime ballotBoxStartTime;
	private LocalDateTime ballotBoxFinishTime;
	private int gracePeriod;

	@BeforeAll
	static void setupAll() {
		final PrimesMappingTableGenerator primesMappingTableGenerator = new PrimesMappingTableGenerator();
		primesMappingTable = primesMappingTableGenerator.generate(5);
	}

	@BeforeEach
	void setup() {
		contextHolder.setTenantId(TEST_TENANT_ID);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		ballotBoxId = uuidGenerator.generate();
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();

		encryptionGroup = primesMappingTable.getEncryptionGroup();

		testBallotBox = SECURE_RANDOM.nextBoolean();

		ballotBoxStartTime = LocalDateTimeUtils.now().minusDays(1);
		ballotBoxFinishTime = LocalDateTimeUtils.now().plusDays(5);
		gracePeriod = 900;
	}

	@Test
	void testSaveWithBadArgumentThrows() {
		assertThrows(NullPointerException.class,
				() -> ballotBoxService.save(null, verificationCardSetId, ballotBoxStartTime, ballotBoxFinishTime, testBallotBox, 10,
						gracePeriod, primesMappingTable));
		assertThrows(NullPointerException.class,
				() -> ballotBoxService.save(ballotBoxId, null, ballotBoxStartTime, ballotBoxFinishTime, testBallotBox, 10,
						gracePeriod, primesMappingTable));
		assertThrows(NullPointerException.class,
				() -> ballotBoxService.save(ballotBoxId, verificationCardSetId, null, ballotBoxFinishTime, testBallotBox, 10,
						gracePeriod, primesMappingTable));
		assertThrows(NullPointerException.class,
				() -> ballotBoxService.save(ballotBoxId, verificationCardSetId, ballotBoxStartTime, null, testBallotBox, 10,
						gracePeriod, primesMappingTable));
		assertThrows(NullPointerException.class,
				() -> ballotBoxService.save(ballotBoxId, verificationCardSetId, ballotBoxStartTime, ballotBoxFinishTime, testBallotBox, 10,
						gracePeriod, null));
		assertThrows(IllegalArgumentException.class,
				() -> ballotBoxService.save(ballotBoxId, verificationCardSetId, ballotBoxStartTime, ballotBoxFinishTime, testBallotBox, -1,
						gracePeriod, primesMappingTable));
		assertThrows(IllegalArgumentException.class,
				() -> ballotBoxService.save(ballotBoxId, verificationCardSetId, ballotBoxStartTime, ballotBoxFinishTime, testBallotBox, 10,
						-1, primesMappingTable));
		final LocalDateTime ballotBoxStartTimeMinusOneDay = ballotBoxStartTime.minusDays(1);
		assertThrows(IllegalArgumentException.class,
				() -> ballotBoxService.save(ballotBoxId, verificationCardSetId, ballotBoxStartTime, ballotBoxStartTimeMinusOneDay, testBallotBox,
						10, gracePeriod, primesMappingTable));
	}

	@Test
	void testExistsWithBadArgumentThrows() {
		assertThrows(NullPointerException.class, () -> ballotBoxService.existsForElectionEventId(null, electionEventId));
		assertThrows(NullPointerException.class, () -> ballotBoxService.existsForElectionEventId(ballotBoxId, null));
		assertThrows(FailedValidationException.class, () -> ballotBoxService.existsForElectionEventId(BAD_ID, electionEventId));
		assertThrows(FailedValidationException.class, () -> ballotBoxService.existsForElectionEventId(ballotBoxId, BAD_ID));
	}

	@Test
	void testExistsWithValidArgumentDoesNotThrow() {
		final boolean existsBefore = ballotBoxService.existsForElectionEventId(ballotBoxId, electionEventId);
		assertFalse(existsBefore);
		setUpElection();
		final boolean existsAfter = ballotBoxService.existsForElectionEventId(ballotBoxId, electionEventId);
		assertTrue(existsAfter);
	}

	@Test
	void testGetBallotBoxByBallotBoxIdWithBadArgumentThrows() {
		assertThrows(NullPointerException.class, () -> ballotBoxService.getBallotBoxByBallotBoxId(null));
		assertThrows(FailedValidationException.class, () -> ballotBoxService.getBallotBoxByBallotBoxId(BAD_ID));
	}

	@Test
	void testGetBallotBoxByBallotBoxIdForUnsavedBallotBoxThrows() {
		assertThrows(IllegalStateException.class, () -> ballotBoxService.getBallotBoxByBallotBoxId(ballotBoxId));
	}

	@Test
	void testGetBallotBoxByBallotBoxIdWithValidArgumentDoesNotThrow() {
		setUpElection();
		final BallotBoxEntity ballotBoxEntity = ballotBoxService.getBallotBoxByBallotBoxId(ballotBoxId);
		assertEquals(ballotBoxId, ballotBoxEntity.getBallotBoxId());
		assertEquals(testBallotBox, ballotBoxEntity.isTestBallotBox());
	}

	@Test
	void testGetBallotBoxByVerificationCardSetIdWithBadArgumentThrows() {
		assertThrows(NullPointerException.class, () -> ballotBoxService.getBallotBoxByVerificationCardSetId(null));
		assertThrows(FailedValidationException.class, () -> ballotBoxService.getBallotBoxByVerificationCardSetId(BAD_ID));
	}

	@Test
	void testGetBallotBoxByVerificationCardSetIdForUnsavedBallotBoxThrows() {
		assertThrows(IllegalStateException.class, () -> ballotBoxService.getBallotBoxByVerificationCardSetId(verificationCardSetId));
	}

	@Test
	void testGetBallotBoxByVerificationCardSetIdWithValidArgumentDoesNotThrow() {
		setUpElection();
		final BallotBoxEntity ballotBoxEntity = ballotBoxService.getBallotBoxByVerificationCardSetId(verificationCardSetId);
		assertEquals(ballotBoxId, ballotBoxEntity.getBallotBoxId());
		assertEquals(testBallotBox, ballotBoxEntity.isTestBallotBox());
	}

	@Test
	void testGetPrimesMappingTableByBallotBoxIdWithBadArgumentThrows() {
		assertThrows(NullPointerException.class, () -> ballotBoxService.getPrimesMappingTableByBallotBoxId(null));
		assertThrows(FailedValidationException.class, () -> ballotBoxService.getPrimesMappingTableByBallotBoxId(BAD_ID));
	}

	@Test
	void testGetPrimesMappingTableByBallotBoxIdForUnsavedBallotBoxThrows() {
		assertThrows(IllegalStateException.class, () -> ballotBoxService.getPrimesMappingTableByBallotBoxId(ballotBoxId));
	}

	@Test
	void testGetPrimesMappingTableByBallotBoxIdWithValidArgumentDoesNotThrow() {
		setUpElection();
		final PrimesMappingTable savedPrimesMappingTable = ballotBoxService.getPrimesMappingTableByBallotBoxId(ballotBoxId);
		assertEquals(primesMappingTable, savedPrimesMappingTable);
	}

	@Test
	void testGetPrimesMappingTableByVerificationCardSetIdWithBadArgumentThrows() {
		assertThrows(NullPointerException.class, () -> ballotBoxService.getPrimesMappingTableByVerificationCardSetId(null));
		assertThrows(FailedValidationException.class, () -> ballotBoxService.getPrimesMappingTableByVerificationCardSetId(BAD_ID));
	}

	@Test
	void testGetPrimesMappingTableByVerificationCardSetIdForUnsavedBallotBoxThrows() {
		assertThrows(IllegalStateException.class, () -> ballotBoxService.getPrimesMappingTableByVerificationCardSetId(verificationCardSetId));
	}

	@Test
	void testGetPrimesMappingTableByVerificationCardSetIdWithValidArgumentDoesNotThrow() {
		setUpElection();
		final PrimesMappingTable savedPrimesMappingTable = ballotBoxService.getPrimesMappingTableByVerificationCardSetId(verificationCardSetId);
		assertEquals(primesMappingTable, savedPrimesMappingTable);
	}

	@Test
	void testIsMixedWithBadArgumentThrows() {
		assertThrows(NullPointerException.class, () -> ballotBoxService.isMixed(null));
	}

	@Test
	void testIsMixedWithValidArgumentDoesNotThrow() {
		setUpElection();
		assertFalse(ballotBoxService.isMixed(ballotBoxId));
		ballotBoxService.setMixed(ballotBoxId);
		assertTrue(ballotBoxService.isMixed(ballotBoxId));
	}

	@Test
	void testSetMixedSetsMixedStateCorrectly() {
		setUpElection();
		final BallotBoxEntity ballotBoxEntity = ballotBoxService.setMixed(ballotBoxId);
		assertTrue(ballotBoxEntity.isMixed());
	}

	private void setUpElection() {
		// Save election event.
		final ElectionEventEntity savedElectionEventEntity = electionEventService.save(electionEventId, encryptionGroup);
		// Save verification card set.
		final VerificationCardSetEntity verificationCardSetEntity = new VerificationCardSetEntity.Builder()
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardSetAlias("alias-" + verificationCardSetId)
				.setVerificationCardSetDescription("Description " + verificationCardSetId)
				.setDomainsOfInfluence(ImmutableList.of("domain1", "domain2"))
				.setElectionEventEntity(savedElectionEventEntity)
				.build();
		verificationCardSetService.save(verificationCardSetEntity);

		ballotBoxService.save(ballotBoxId, verificationCardSetId, LocalDateTimeUtils.now().minusDays(1), LocalDateTimeUtils.now().plusDays(5),
				testBallotBox,
				10, gracePeriod, primesMappingTable);
	}
}
