/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static ch.post.it.evoting.securedatamanager.shared.Constants.SETUP_KEY_PAIR_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;

class SetupKeyPairFileRepositoryTest {

	private static final String INVALID_ELECTION_EVENT_ID = "invalidId";
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static GqGroup gqGroup;
	private static String electionEventId;
	private static ObjectMapper objectMapper;
	private static SetupPathResolver pathResolver;
	private static SetupKeyPairFileRepository setupKeyPairFileRepository;
	private static ElectionEventContextPayloadService electionEventContextPayloadService;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws Exception {
		gqGroup = GroupTestData.getLargeGqGroup();
		objectMapper = DomainObjectMapper.getNewInstance();
		electionEventId = uuidGenerator.generate();

		pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		electionEventContextPayloadService = mock(ElectionEventContextPayloadService.class);
		when(electionEventContextPayloadService.loadEncryptionGroup(anyString())).thenReturn(gqGroup);

		setupKeyPairFileRepository = new SetupKeyPairFileRepository(objectMapper, pathResolver, electionEventContextPayloadService);

		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);
		final ElGamalMultiRecipientKeyPair setupKeyPair = elGamalGenerator.genRandomKeyPair(10);

		setupKeyPairFileRepository.save(electionEventId, setupKeyPair);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private ElGamalMultiRecipientKeyPair setupKeyPair;
		private SetupKeyPairFileRepository setupKeyPairFileRepositoryTemp;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			final SetupPathResolver setupPathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
			setupKeyPairFileRepositoryTemp = new SetupKeyPairFileRepository(objectMapper, setupPathResolver, electionEventContextPayloadService);
		}

		@BeforeEach
		void setUp() {
			final int numElements = secureRandom.nextInt(10) + 1;
			setupKeyPair = ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, numElements, random);
		}

		@Test
		@DisplayName("valid key pair creates file")
		void saveValidKeyPair() {
			final Path savedPath = setupKeyPairFileRepositoryTemp.save(electionEventId, setupKeyPair);

			assertTrue(Files.exists(savedPath));
		}

		@Test
		@DisplayName("null key pair throws NullPointerException")
		void saveNullKeyPair() {
			assertThrows(NullPointerException.class, () -> setupKeyPairFileRepositoryTemp.save(electionEventId, null));
		}

		@Test
		@DisplayName("invalid election end id throws FailedValidationException")
		void saveInvalidElectionEventId() {
			assertThrows(FailedValidationException.class, () -> setupKeyPairFileRepositoryTemp.save(INVALID_ELECTION_EVENT_ID, setupKeyPair));
		}

		@Nested
		@TestInstance(TestInstance.Lifecycle.PER_CLASS)
		@DisplayName("finding by id")
		class FindByIdTest {

			@Test
			@DisplayName("for existing key pair is present")
			void findByIdExistingKeyPair() {
				assertTrue(setupKeyPairFileRepository.findById(electionEventId).isPresent());
			}

			@Test
			@DisplayName("with invalid election event id throws NullPointerException")
			void findByIdInvalidId() {
				assertThrows(FailedValidationException.class, () -> setupKeyPairFileRepository.findById(INVALID_ELECTION_EVENT_ID));
			}

			@Test
			@DisplayName("with wrong election event id returns empty optional")
			void findByIdWrongElectionEventId() {
				final String wrongElectionEventId = uuidGenerator.generate();
				assertFalse(setupKeyPairFileRepository.findById(wrongElectionEventId).isPresent());
			}

			@Test
			@DisplayName("with missing encryption parameters throws IllegalStateException")
			void findByIdMissingEncryptionParameters() {
				when(electionEventContextPayloadService.loadEncryptionGroup(electionEventId))
						.thenThrow(new IllegalStateException("Encryption group not found."));

				final SetupKeyPairFileRepository repository = new SetupKeyPairFileRepository(objectMapper, pathResolver,
						electionEventContextPayloadService);

				final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> repository.findById(electionEventId));

				assertEquals("Encryption group not found.", Throwables.getRootCause(exception).getMessage());
			}

			@Test
			@DisplayName("with corrupted key pair throws UncheckedIOException")
			void findByIdCorruptedKeyPair() {
				final GqGroup otherGqGroup = GroupTestData.getGqGroup();
				when(electionEventContextPayloadService.loadEncryptionGroup(anyString())).thenReturn(otherGqGroup);

				final UncheckedIOException exception = assertThrows(UncheckedIOException.class,
						() -> setupKeyPairFileRepository.findById(electionEventId));

				final Path setupKeyPairPath = pathResolver.resolveElectionEventPath(electionEventId).resolve(SETUP_KEY_PAIR_FILE_NAME);
				final String errorMessage = String.format("Failed to deserialize setup key pair. [electionEventId: %s, path: %s]",
						electionEventId, setupKeyPairPath);
				assertEquals(errorMessage, exception.getMessage());
			}
		}

	}

}
