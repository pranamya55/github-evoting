/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

import static ch.post.it.evoting.domain.multitenancy.TenantConstants.TEST_TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.base.Strings;

import ch.post.it.evoting.controlcomponent.process.BallotBoxEntity;
import ch.post.it.evoting.controlcomponent.process.BallotBoxRepository;
import ch.post.it.evoting.controlcomponent.process.BallotBoxService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventEntity;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetEntity;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;

/**
 * These test are here to simulate multiple JVMs accessing the same shared resources on the database. This is the case when the control components are
 * replicated over multiple machines with the same service logic. The first tests whether two simultaneous insert leads to the second one overwriting
 * the first. Without optimistic locking JPA save becomes an insert or update so the second transaction to go through overwrites the first. We saw
 * this bug in testing. The second test is to make sure that two simultaneous updates to the same record are atomic, ie doesn't lead to an
 * inconsistent state. Optimistic locking put in place guarantees that.
 */
@SpringBootTest
@ContextConfiguration(initializers = TestKeyStoreInitializer.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConcurrentDatabaseAccessIT {

	private static final String ELECTION_EVENT_ID = Strings.padEnd("", 32, '0');
	private static final String VERIFICATION_CARD_SET_ID = Strings.padEnd("", 32, '0');
	private static final String BALLOT_BOX_ID = Strings.padEnd("", 32, '0');
	private static final GqGroup encryptionGroup = GroupTestData.getLargeGqGroup();

	@Autowired
	private ElectionEventService electionEventService;

	@Autowired
	private VerificationCardSetService verificationCardSetService;

	@Autowired
	private BallotBoxRepository ballotBoxRepository;

	@Autowired
	private BallotBoxService ballotBoxService;

	@Autowired
	private TestDatabaseCleanUpService testDatabaseCleanUpService;
	@Autowired
	private ContextHolder contextHolder;

	@BeforeEach
	void initializeTenant() {
		contextHolder.setTenantId(TEST_TENANT_ID);
	}

	@AfterEach
	void cleanUp() {
		testDatabaseCleanUpService.cleanUp();
	}

	@RepeatedTest(5)
	void testPrimaryKeyViolation() throws InterruptedException {
		final int numThreads = 2;
		final CountDownLatch checkLatch = new CountDownLatch(numThreads);
		final CountDownLatch saveLatch = new CountDownLatch(1);

		final ExecutorService executorService = Executors.newFixedThreadPool(numThreads, new CustomizableThreadFactory("lock-"));

		final CompletableFuture<?> saveFirst = CompletableFuture.runAsync(checkTogetherThenSaveFirst(checkLatch, saveLatch), executorService);
		final CompletableFuture<?> saveSecond = CompletableFuture.runAsync(checkTogetherThenSaveSecond(checkLatch, saveLatch), executorService);

		executorService.shutdown();
		final boolean normalTermination = executorService.awaitTermination(1, TimeUnit.SECONDS);
		assertTrue(normalTermination);

		assertFalse(saveFirst.isCompletedExceptionally());
		assertTrue(saveSecond.isCompletedExceptionally());

		final ElectionEventEntity electionEventEntity = assertDoesNotThrow(() -> electionEventService.getElectionEventEntity(ELECTION_EVENT_ID));
		assertEquals(encryptionGroup, electionEventEntity.getEncryptionGroup());

		final Throwable cause = getCause(saveSecond);
		assertInstanceOf(DataIntegrityViolationException.class, cause);
	}

	Runnable checkTogetherThenSaveFirst(final CountDownLatch checkLatch, final CountDownLatch saveLatch) {
		return () -> {
			try {
				contextHolder.setTenantId(TEST_TENANT_ID);
				electionEventService.getElectionEventEntity(ELECTION_EVENT_ID);
			} catch (final IllegalStateException ignored) {
				checkLatch.countDown();

				awaitOneSecondWithRuntimeException(checkLatch);

				electionEventService.save(ELECTION_EVENT_ID, encryptionGroup);
				saveLatch.countDown();
			}
		};
	}

	Runnable checkTogetherThenSaveSecond(final CountDownLatch checkLatch, final CountDownLatch saveLatch) {
		return () -> {
			try {
				contextHolder.setTenantId(TEST_TENANT_ID);
				electionEventService.getElectionEventEntity(ELECTION_EVENT_ID);
			} catch (final IllegalStateException ignored) {
				checkLatch.countDown();

				awaitOneSecondWithRuntimeException(checkLatch);
				awaitOneSecondWithRuntimeException(saveLatch);

				electionEventService.save(ELECTION_EVENT_ID, encryptionGroup);
				saveLatch.countDown();
			}
		};
	}

	@RepeatedTest(5)
	void testOptimisticLockingException() throws InterruptedException {
		setUpElection();

		final int numThreads = 2;
		final CountDownLatch getLatch = new CountDownLatch(numThreads);
		final CountDownLatch saveLatch = new CountDownLatch(1);

		final ExecutorService executorService = Executors.newFixedThreadPool(numThreads, new CustomizableThreadFactory("lock-"));

		final CompletableFuture<?> updateFirst = CompletableFuture.runAsync(getTogetherThenUpdateFirst(getLatch, saveLatch), executorService);
		final CompletableFuture<?> updateSecond = CompletableFuture.runAsync(getTogetherThenUpdateSecond(getLatch, saveLatch), executorService);

		executorService.shutdown();
		final boolean normalTermination = executorService.awaitTermination(1, TimeUnit.SECONDS);
		assertTrue(normalTermination);

		assertFalse(updateFirst.isCompletedExceptionally());
		assertTrue(updateSecond.isCompletedExceptionally());

		final BallotBoxEntity ballotBoxEntity = assertDoesNotThrow(
				() -> ballotBoxService.getBallotBoxByBallotBoxId(BALLOT_BOX_ID));
		assertTrue(ballotBoxEntity.isMixed());

		final Throwable cause = getCause(updateSecond);
		assertInstanceOf(OptimisticLockingFailureException.class, cause);
	}

	Runnable getTogetherThenUpdateFirst(final CountDownLatch checkLatch, final CountDownLatch saveLatch) {

		return () -> {
			try {
				contextHolder.setTenantId(TEST_TENANT_ID);
				final BallotBoxEntity ballotBox = ballotBoxService.getBallotBoxByBallotBoxId(BALLOT_BOX_ID);
				checkLatch.countDown();

				awaitOneSecondWithRuntimeException(checkLatch);

				ballotBox.setMixed();
				ballotBoxRepository.save(ballotBox);
				saveLatch.countDown();
			} catch (final IllegalStateException ignored) {
				// Ignored.
			}
		};
	}

	Runnable getTogetherThenUpdateSecond(final CountDownLatch checkLatch, final CountDownLatch saveLatch) {
		return () -> {
			try {
				contextHolder.setTenantId(TEST_TENANT_ID);
				final BallotBoxEntity ballotBox = ballotBoxService.getBallotBoxByBallotBoxId(BALLOT_BOX_ID);
				checkLatch.countDown();

				awaitOneSecondWithRuntimeException(checkLatch);
				awaitOneSecondWithRuntimeException(saveLatch);

				ballotBox.setMixed();
				ballotBoxRepository.save(ballotBox);
				saveLatch.countDown();
			} catch (final IllegalStateException ignored) {
				// Ignored.
			}
		};
	}

	private void setUpElection() {

		// Save election event.
		final ElectionEventEntity savedElectionEventEntity = electionEventService.save(ELECTION_EVENT_ID, encryptionGroup);

		// Save verification card set.
		final VerificationCardSetEntity verificationCardSetEntity = new VerificationCardSetEntity.Builder()
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID)
				.setVerificationCardSetAlias("alias-" + VERIFICATION_CARD_SET_ID)
				.setVerificationCardSetDescription("Description " + VERIFICATION_CARD_SET_ID)
				.setDomainsOfInfluence(ImmutableList.of("domain1", "domain2"))
				.setElectionEventEntity(savedElectionEventEntity)
				.build();
		verificationCardSetService.save(verificationCardSetEntity);

		final PrimesMappingTable primesMappingTable = new PrimesMappingTableGenerator(encryptionGroup).generate(2);

		ballotBoxService.save(BALLOT_BOX_ID, VERIFICATION_CARD_SET_ID, LocalDateTimeUtils.now().minusDays(1), LocalDateTimeUtils.now().plusDays(3),
				true,
				1,
				900, primesMappingTable);
	}

	private Throwable getCause(final CompletableFuture<?> future) throws InterruptedException {
		assert (future.isCompletedExceptionally());
		try {
			future.get();
		} catch (final ExecutionException e) {
			return e.getCause();
		}
		throw new IllegalStateException("Shouldn't reach this state.");
	}

	private void awaitOneSecondWithRuntimeException(final CountDownLatch countDownLatch) {
		awaitWithRuntimeException(countDownLatch, 1);
	}

	private void awaitWithRuntimeException(final CountDownLatch countDownLatch, final int timeout) {
		final boolean awaited;
		try {
			awaited = countDownLatch.await(timeout, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			throw new IllegalStateException("We should not reach this state.");
		}
		if (!awaited) {
			throw new IllegalStateException("Timeout for countDownLatch.");
		}
	}

}
