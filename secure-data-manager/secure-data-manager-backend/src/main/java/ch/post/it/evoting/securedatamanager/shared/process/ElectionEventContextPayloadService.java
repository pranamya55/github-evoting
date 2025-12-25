/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Allows saving, retrieving and finding existing election event context payloads.
 */
@Service
public class ElectionEventContextPayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectionEventContextPayloadService.class);

	private final CacheableAllPrimesMappingTableLoader cacheableAllPrimesMappingTableLoader;
	private final ElectionEventContextPayloadFileRepository electionEventContextPayloadFileRepository;
	private final CacheableElectionEventContextPayloadLoader cacheableElectionEventContextPayloadLoader;

	public ElectionEventContextPayloadService(
			final CacheableAllPrimesMappingTableLoader cacheableAllPrimesMappingTableLoader,
			final ElectionEventContextPayloadFileRepository electionEventContextPayloadFileRepository,
			final CacheableElectionEventContextPayloadLoader cacheableElectionEventContextPayloadLoader) {
		this.cacheableAllPrimesMappingTableLoader = cacheableAllPrimesMappingTableLoader;
		this.electionEventContextPayloadFileRepository = electionEventContextPayloadFileRepository;
		this.cacheableElectionEventContextPayloadLoader = cacheableElectionEventContextPayloadLoader;
	}

	/**
	 * Saves an election event context payload in the corresponding election event folder.
	 *
	 * @param electionEventContextPayload the election event context payload to save.
	 * @throws NullPointerException if {@code electionEventContext} is null.
	 */
	public void save(final ElectionEventContextPayload electionEventContextPayload) {
		checkNotNull(electionEventContextPayload);

		final String electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();

		electionEventContextPayloadFileRepository.save(electionEventContextPayload);
		LOGGER.info("Saved election event context payload. [electionEventId: {}]", electionEventId);
	}

	/**
	 * Checks if the election event context payload is present for the given election event id.
	 *
	 * @param electionEventId the election event id to check.
	 * @return {@code true} if the election event context payload is present, {@code false} otherwise.
	 * @throws FailedValidationException if {@code electionEventId} is invalid.
	 */
	public boolean exist(final String electionEventId) {
		validateUUID(electionEventId);

		return electionEventContextPayloadFileRepository.existsById(electionEventId);
	}

	/**
	 * Loads the election event context payload for the given {@code electionEventId}. The result of this method is stored in a synchronized cache.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the election event context payload for this {@code electionEventId}.
	 * @throws FailedValidationException if {@code electionEventId} is invalid.
	 * @throws IllegalStateException     if the requested election event context is not present.
	 */
	public ElectionEventContextPayload load(final String electionEventId) {
		validateUUID(electionEventId);

		return cacheableElectionEventContextPayloadLoader.load(electionEventId);
	}

	/**
	 * Loads the encryption group for the given {@code electionEventId}. The result of this method is stored in a synchronized cache.
	 *
	 * @param electionEventId the election event id for which to get the encryption group. Must be non-null and a valid UUID.
	 * @return the encryption group.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 * @throws IllegalStateException     if the election event context payload is not found for this {@code electionEventId}.
	 */
	@Cacheable(value = "gqGroups", sync = true)
	public GqGroup loadEncryptionGroup(final String electionEventId) {
		validateUUID(electionEventId);

		final GqGroup encryptionGroup = load(electionEventId).getEncryptionGroup();

		LOGGER.info("Loaded encryption group. [electionEventId: {}]", electionEventId);

		return encryptionGroup;
	}

	/**
	 * Loads the small primes for the given {@code electionEventId}.
	 *
	 * @param electionEventId the election event id for which to get the small primes. Must be non-null and a valid UUID.
	 * @return the small primes.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 * @throws IllegalStateException     if the election event context payload is not found for this {@code electionEventId}.
	 */
	public GroupVector<PrimeGqElement, GqGroup> loadSmallPrimes(final String electionEventId) {
		validateUUID(electionEventId);

		return load(electionEventId).getSmallPrimes();
	}

	/**
	 * Loads all the primes mapping tables for the given {@code electionEventId}. The result of this method is stored in a synchronized cache.
	 *
	 * @param electionEventId the election event id for which to get the primes mapping tables. Must be non-null and a valid UUID.
	 * @return the map of primes mapping tables with the verification card set id as key.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 * @throws IllegalStateException     if the election event context payload is not found for this {@code electionEventId}.
	 */
	public ImmutableMap<String, PrimesMappingTable> loadAllPrimesMappingTables(final String electionEventId) {
		validateUUID(electionEventId);

		return cacheableAllPrimesMappingTableLoader.loadMappedByVerificationCardSet(electionEventId);
	}

	/**
	 * Loads all the primes mapping tables for the given {@code electionEventId}. The result of this method is stored in a synchronized cache.
	 *
	 * @param electionEventId the election event id for which to get the primes mapping tables. Must be non-null and a valid UUID.
	 * @return the map of primes mapping tables with the ballot box id as key.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 * @throws IllegalStateException     if the election event context payload is not found for this {@code electionEventId}.
	 */
	public ImmutableMap<String, PrimesMappingTable> loadAllPrimesMappingTablesMappedByBallotBox(final String electionEventId) {
		validateUUID(electionEventId);

		return cacheableAllPrimesMappingTableLoader.loadMappedByBallotBox(electionEventId);
	}

	/**
	 * Loads the primes mapping table for the given {@code electionEventId} and {@code verificationCardSetId}. The result of this method is stored in
	 * a synchronized cache.
	 *
	 * @param electionEventId       the election event id for which to get the primes mapping table. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id for which to get the primes mapping table. Must be non-null and a valid UUID.
	 * @return the primes mapping table.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} are not valid UUIDs.
	 * @throws IllegalStateException     if the election event context payload is not found for this {@code electionEventId}.
	 */
	@Cacheable(value = "primesMappingTables", sync = true)
	public PrimesMappingTable loadPrimesMappingTable(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final ImmutableMap<String, PrimesMappingTable> allPrimesMappingTables = loadAllPrimesMappingTables(electionEventId);
		checkState(allPrimesMappingTables.containsKey(verificationCardSetId),
				"Primes mapping table not found. [electionEventId: %s, verificationCardSetId: %s]", electionEventId, verificationCardSetId);

		final PrimesMappingTable primesMappingTable = allPrimesMappingTables.get(verificationCardSetId);

		LOGGER.info("Loaded primes mapping table. [electionEventId: {}, verificationCardSetId: {}]", electionEventId, verificationCardSetId);

		return primesMappingTable;
	}

	/**
	 * Loads the primes mapping table for the given {@code electionEventId} and {@code ballotBoxId}. The result of this method is stored in a
	 * synchronized cache.
	 *
	 * @param electionEventId the election event id for which to get the primes mapping table. Must be non-null and a valid UUID.
	 * @param ballotBoxId     the ballotBox id for which to get the primes mapping table. Must be non-null and a valid UUID.
	 * @return the primes mapping table.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardSetId} are not valid UUIDs.
	 * @throws IllegalStateException     if the election event context payload is not found for this {@code electionEventId}.
	 */
	@Cacheable(value = "primesMappingTablesByBallotBox", sync = true)
	public PrimesMappingTable loadPrimesMappingTableByBallotBox(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		final ImmutableMap<String, PrimesMappingTable> allPrimesMappingTables = loadAllPrimesMappingTablesMappedByBallotBox(electionEventId);
		checkState(allPrimesMappingTables.containsKey(ballotBoxId),
				"Primes mapping table not found. [electionEventId: %s, ballotBoxId: %s]", electionEventId, ballotBoxId);

		final PrimesMappingTable primesMappingTable = allPrimesMappingTables.get(ballotBoxId);

		LOGGER.info("Loaded primes mapping table. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		return primesMappingTable;
	}

	@Service
	public static class CacheableElectionEventContextPayloadLoader {
		private static final Logger LOGGER = LoggerFactory.getLogger(
				ElectionEventContextPayloadService.CacheableElectionEventContextPayloadLoader.class);

		private final ElectionEventContextPayloadFileRepository electionEventContextPayloadFileRepository;

		public CacheableElectionEventContextPayloadLoader(
				final ElectionEventContextPayloadFileRepository electionEventContextPayloadFileRepository) {
			this.electionEventContextPayloadFileRepository = electionEventContextPayloadFileRepository;
		}

		/**
		 * Loads the election event context payload for the given {@code electionEventId}. The result of this method is stored in a synchronized
		 * cache.
		 *
		 * @param electionEventId the election event id. Must be non-null and a valid UUID.
		 * @return the election event context payload for this {@code electionEventId}.
		 * @throws FailedValidationException if {@code electionEventId} is invalid.
		 * @throws IllegalStateException     if the requested election event context is not present.
		 */
		@Cacheable(value = "electionEventContextPayloads", sync = true)
		public ElectionEventContextPayload load(final String electionEventId) {
			validateUUID(electionEventId);

			final ElectionEventContextPayload payload = electionEventContextPayloadFileRepository.findById(electionEventId)
					.orElseThrow(() -> new IllegalStateException(
							String.format("Requested election event context payload is not present. [electionEventId: %s]", electionEventId)));

			LOGGER.info("Loaded election event context payload. [electionEventId: {}]", electionEventId);

			return payload;
		}
	}

	@Service
	public static class CacheableAllPrimesMappingTableLoader {
		private static final Logger LOGGER = LoggerFactory.getLogger(ElectionEventContextPayloadService.CacheableAllPrimesMappingTableLoader.class);

		private final CacheableElectionEventContextPayloadLoader cacheableElectionEventContextPayloadLoader;

		public CacheableAllPrimesMappingTableLoader(final CacheableElectionEventContextPayloadLoader cacheableElectionEventContextPayloadLoader) {
			this.cacheableElectionEventContextPayloadLoader = cacheableElectionEventContextPayloadLoader;
		}

		private ImmutableMap<String, PrimesMappingTable> loadPrimesMappingTables(final String electionEventId,
				final Function<VerificationCardSetContext, String> keyMapper) {

			final ImmutableMap<String, PrimesMappingTable> allPrimesMappingTables = cacheableElectionEventContextPayloadLoader.load(electionEventId)
					.getElectionEventContext()
					.verificationCardSetContexts()
					.stream()
					.parallel()
					.collect(toImmutableMap(
							keyMapper,
							VerificationCardSetContext::getPrimesMappingTable
					));

			LOGGER.info("Loaded all primes mapping tables. [electionEventId: {}]", electionEventId);

			return allPrimesMappingTables;
		}

		/**
		 * Loads all the primes mapping tables for the given {@code electionEventId} mapped by verification card set id.
		 *
		 * @param electionEventId the election event id for which to get the primes mapping table. Must be non-null and a valid UUID.
		 * @return the map of primes mapping tables with the verification card set id as key.
		 */
		@Cacheable(value = "allPrimesMappingTables", sync = true)
		public ImmutableMap<String, PrimesMappingTable> loadMappedByVerificationCardSet(final String electionEventId) {
			validateUUID(electionEventId);
			return loadPrimesMappingTables(electionEventId, VerificationCardSetContext::getVerificationCardSetId);
		}

		/**
		 * Loads all the primes mapping tables for the given {@code electionEventId} mapped by ballot box id.
		 *
		 * @param electionEventId the election event id for which to get the primes mapping table. Must be non-null and a valid UUID.
		 * @return the map of primes mapping tables with the ballot box id as key.
		 */
		@Cacheable(value = "allPrimesMappingTablesMappedByBallotBox", sync = true)
		public ImmutableMap<String, PrimesMappingTable> loadMappedByBallotBox(final String electionEventId) {
			validateUUID(electionEventId);
			return loadPrimesMappingTables(electionEventId, VerificationCardSetContext::getBallotBoxId);
		}
	}
}
