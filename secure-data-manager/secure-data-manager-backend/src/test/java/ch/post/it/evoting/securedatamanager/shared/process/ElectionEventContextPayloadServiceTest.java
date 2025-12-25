/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet.toImmutableSet;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService.CacheableAllPrimesMappingTableLoader;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService.CacheableElectionEventContextPayloadLoader;

@DisplayName("An ElectionEventContextPayloadService")
class ElectionEventContextPayloadServiceTest {

	private static final String WRONG_ELECTION_EVENT_ID = "414BD34DCF6E4DE4B771A92FA3849D3D";
	private static final String NOT_EXISTING_ELECTION_EVENT_PAYLOAD = "614BD34DCF6E4DE4B771A92FA3849D3D";
	private static final String INVALID_ID = "invalidId";
	private static final String NOT_EXISTING_ELECTION_EVENT_PAYLOAD_MESSAGE = "Requested election event context payload is not present. [electionEventId: %s]";
	private static final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
	private static String electionEventId;
	private static ObjectMapper objectMapper;
	private static PathResolver pathResolver;
	private static ElectionEventContextPayload electionEventContextPayload;
	private static ElectionEventContextPayloadService electionEventContextPayloadService;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {
		objectMapper = DomainObjectMapper.getNewInstance();

		electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		electionEventContextPayload.setSignature(new CryptoPrimitivesSignature(createHash().recursiveHash(electionEventContextPayload)));
		electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();

		final PathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		final ElectionEventContextPayloadFileRepository electionEventContextPayloadFileRepository = new ElectionEventContextPayloadFileRepository(
				objectMapper, pathResolver);

		final CacheableElectionEventContextPayloadLoader cacheableElectionEventContextPayloadLoader = new CacheableElectionEventContextPayloadLoader(
				electionEventContextPayloadFileRepository);
		final CacheableAllPrimesMappingTableLoader cacheableAllPrimesMappingTableLoader = new CacheableAllPrimesMappingTableLoader(
				cacheableElectionEventContextPayloadLoader);

		electionEventContextPayloadService = new ElectionEventContextPayloadService(cacheableAllPrimesMappingTableLoader,
				electionEventContextPayloadFileRepository, cacheableElectionEventContextPayloadLoader);

		electionEventContextPayloadService.save(electionEventContextPayload);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private ElectionEventContextPayloadService electionEventContextPayloadServiceTemp;

		private ElectionEventContextPayload electionEventContextPayload;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
			final ElectionEventContextPayloadFileRepository electionEventContextPayloadFileRepositoryTemp = new ElectionEventContextPayloadFileRepository(
					objectMapper, pathResolver);

			final CacheableElectionEventContextPayloadLoader cacheableElectionEventContextPayloadLoader = new CacheableElectionEventContextPayloadLoader(
					electionEventContextPayloadFileRepositoryTemp);
			final CacheableAllPrimesMappingTableLoader cacheableAllPrimesMappingTableLoader = new CacheableAllPrimesMappingTableLoader(
					cacheableElectionEventContextPayloadLoader);

			electionEventContextPayloadServiceTemp = new ElectionEventContextPayloadService(cacheableAllPrimesMappingTableLoader,
					electionEventContextPayloadFileRepositoryTemp, cacheableElectionEventContextPayloadLoader);
		}

		@BeforeEach
		void setUp() {
			// Create payload.
			electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		}

		@Test
		@DisplayName("a valid payload does not throw")
		void saveValidPayload() {
			assertDoesNotThrow(() -> electionEventContextPayloadServiceTemp.save(electionEventContextPayload));

			assertTrue(Files.exists(
					pathResolver.resolveElectionEventPath(electionEventContextPayload.getElectionEventContext().electionEventId())
							.resolve(ElectionEventContextPayloadFileRepository.PAYLOAD_FILE_NAME)));
		}

		@Test
		@DisplayName("a null payload throws NullPointerException")
		void saveNullPayload() {
			assertThrows(NullPointerException.class, () -> electionEventContextPayloadServiceTemp.save(null));
		}

	}

	@Nested
	@DisplayName("calling exist")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ExistTest {

		@Test
		@DisplayName("for valid election event returns true")
		void existValidElectionEvent() {
			assertTrue(electionEventContextPayloadService.exist(electionEventId));
		}

		@Test
		@DisplayName("for invalid election event id throws FailedValidationException")
		void existInvalidElectionEvent() {
			assertThrows(FailedValidationException.class, () -> electionEventContextPayloadService.exist(INVALID_ID));
		}

		@Test
		@DisplayName("for non existing election event returns false")
		void existNonExistingElectionEvent() {
			assertFalse(electionEventContextPayloadService.exist(WRONG_ELECTION_EVENT_ID));
		}

	}

	@Nested
	@DisplayName("loading")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LoadTest {

		@Test
		@DisplayName("existing election event returns expected election event context payload")
		void loadExistingElectionEvent() {
			final ElectionEventContextPayload loadedElectionEventContextPayload = electionEventContextPayloadService.load(electionEventId);

			assertNotNull(loadedElectionEventContextPayload);
		}

		@Test
		@DisplayName("invalid election event id throws FailedValidationException")
		void loadInvalidElectionEventId() {
			assertThrows(FailedValidationException.class, () -> electionEventContextPayloadService.load(INVALID_ID));
		}

		@Test
		@DisplayName("missing payload throws IllegalStateException")
		void loadMissingPayload() {
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> electionEventContextPayloadService.load(NOT_EXISTING_ELECTION_EVENT_PAYLOAD));

			final String errorMessage = String.format(NOT_EXISTING_ELECTION_EVENT_PAYLOAD_MESSAGE, NOT_EXISTING_ELECTION_EVENT_PAYLOAD);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}
	}

	@Nested
	@DisplayName("loading encryption group")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LoadEncryptionGroupTest {

		@Test
		@DisplayName("with existing election event returns expected encryption group")
		void loadEncryptionGroupExistingElectionEvent() {
			final GqGroup encryptionGroup = electionEventContextPayloadService.loadEncryptionGroup(electionEventId);

			assertEquals(electionEventContextPayload.getEncryptionGroup(), encryptionGroup);
		}

		@Test
		@DisplayName("with invalid election event id throws FailedValidationException")
		void loadEncryptionGroupInvalidElectionEventId() {
			assertThrows(FailedValidationException.class, () -> electionEventContextPayloadService.loadEncryptionGroup(INVALID_ID));
		}

		@Test
		@DisplayName("with missing payload throws IllegalStateException")
		void loadEncryptionGroupMissingPayload() {
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> electionEventContextPayloadService.loadEncryptionGroup(NOT_EXISTING_ELECTION_EVENT_PAYLOAD));

			final String errorMessage = String.format(NOT_EXISTING_ELECTION_EVENT_PAYLOAD_MESSAGE, NOT_EXISTING_ELECTION_EVENT_PAYLOAD);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}
	}

	@Nested
	@DisplayName("loading small primes")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LoadSmallPrimesTest {

		@Test
		@DisplayName("with existing election event returns expected encryption group")
		void loadSmallPrimesExistingElectionEvent() {
			final GroupVector<PrimeGqElement, GqGroup> smallPrimes = electionEventContextPayloadService.loadSmallPrimes(electionEventId);

			assertEquals(electionEventContextPayload.getSmallPrimes(), smallPrimes);
		}

		@Test
		@DisplayName("with invalid election event id throws FailedValidationException")
		void loadSmallPrimesInvalidElectionEventId() {
			assertThrows(FailedValidationException.class, () -> electionEventContextPayloadService.loadSmallPrimes(INVALID_ID));
		}

		@Test
		@DisplayName("with missing payload throws IllegalStateException")
		void loadSmallPrimesMissingPayload() {
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> electionEventContextPayloadService.loadSmallPrimes(NOT_EXISTING_ELECTION_EVENT_PAYLOAD));

			final String errorMessage = String.format(NOT_EXISTING_ELECTION_EVENT_PAYLOAD_MESSAGE, NOT_EXISTING_ELECTION_EVENT_PAYLOAD);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}
	}

	@Nested
	@DisplayName("loading all primes mapping tables")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LoadAllPrimesMappingTablesTest {

		@Test
		@DisplayName("with existing election event returns expected encryption group")
		void loadAllPrimesMappingTablesExistingElectionEvent() {
			final ImmutableMap<String, PrimesMappingTable> allPrimesMappingTables = electionEventContextPayloadService.loadAllPrimesMappingTables(
					electionEventId);

			final ImmutableList<VerificationCardSetContext> verificationCardSetContexts = electionEventContextPayload.getElectionEventContext()
					.verificationCardSetContexts();
			assertEquals(verificationCardSetContexts.size(), allPrimesMappingTables.size());
			assertTrue(verificationCardSetContexts.stream()
					.map(VerificationCardSetContext::getVerificationCardSetId)
					.allMatch(allPrimesMappingTables::containsKey));
			assertEquals(verificationCardSetContexts.stream()
							.map(VerificationCardSetContext::getPrimesMappingTable)
							.collect(toImmutableSet()),
					allPrimesMappingTables.values().stream().collect(toImmutableSet()));
		}

		@Test
		@DisplayName("with invalid election event id throws FailedValidationException")
		void loadAllPrimesMappingTablesInvalidElectionEventId() {
			assertThrows(FailedValidationException.class, () -> electionEventContextPayloadService.loadAllPrimesMappingTables(INVALID_ID));
		}

		@Test
		@DisplayName("with missing payload throws IllegalStateException")
		void loadAllPrimesMappingTablesMissingPayload() {
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> electionEventContextPayloadService.loadAllPrimesMappingTables(NOT_EXISTING_ELECTION_EVENT_PAYLOAD));

			final String errorMessage = String.format(NOT_EXISTING_ELECTION_EVENT_PAYLOAD_MESSAGE, NOT_EXISTING_ELECTION_EVENT_PAYLOAD);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}
	}

	@Nested
	@DisplayName("loading primes mapping table")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LoadPrimesMappingTableTest {

		@Test
		@DisplayName("with existing election event and verification card set returns expected encryption group")
		void loadPrimesMappingTableExistingElectionEvent() {
			final VerificationCardSetContext verificationCardSetContext = electionEventContextPayload.getElectionEventContext()
					.verificationCardSetContexts()
					.get(0);
			final PrimesMappingTable primesMappingTable = electionEventContextPayloadService.loadPrimesMappingTable(
					electionEventId, verificationCardSetContext.getVerificationCardSetId());

			assertEquals(verificationCardSetContext.getPrimesMappingTable(), primesMappingTable);
		}

		@Test
		@DisplayName("with invalid ids throws FailedValidationException")
		void loadPrimesMappingTableInvalidElectionEventId() {
			final String verificationCardSetId = electionEventContextPayload.getElectionEventContext()
					.verificationCardSetContexts()
					.get(0)
					.getVerificationCardSetId();
			assertThrows(FailedValidationException.class,
					() -> electionEventContextPayloadService.loadPrimesMappingTable(INVALID_ID, verificationCardSetId));
			assertThrows(FailedValidationException.class,
					() -> electionEventContextPayloadService.loadPrimesMappingTable(electionEventId, INVALID_ID));
		}

		@Test
		@DisplayName("with missing payload throws IllegalStateException")
		void loadPrimesMappingTableMissingPayload() {
			final String verificationCardSetId = electionEventContextPayload.getElectionEventContext()
					.verificationCardSetContexts()
					.get(0)
					.getVerificationCardSetId();
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> electionEventContextPayloadService.loadPrimesMappingTable(NOT_EXISTING_ELECTION_EVENT_PAYLOAD, verificationCardSetId));

			final String errorMessage = String.format(NOT_EXISTING_ELECTION_EVENT_PAYLOAD_MESSAGE, NOT_EXISTING_ELECTION_EVENT_PAYLOAD);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("with missing verification card set throws IllegalStateException")
		void loadPrimesMappingTableMissingVerificationCardSet() {
			final String verificationCardSetId = "B77134DCF6E4DE43849DA92FA414BD3D";
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> electionEventContextPayloadService.loadPrimesMappingTable(electionEventId, verificationCardSetId));

			final String errorMessage = String.format("Primes mapping table not found. [electionEventId: %s, verificationCardSetId: %s]",
					electionEventId, verificationCardSetId);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}
	}
}
