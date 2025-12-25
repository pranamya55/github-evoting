/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
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

@DisplayName("SetupKeyPairService")
class SetupKeyPairServiceTest {

	private static final String INVALID_ELECTION_EVENT_ID = "invalidId";
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static String electionEventId;
	private static GqGroup gqGroup;
	private static ObjectMapper objectMapper;
	private static SetupKeyPairService setupKeyPairService;
	private static ElectionEventContextPayloadService electionEventContextPayloadService;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {
		gqGroup = GroupTestData.getGqGroup();
		objectMapper = DomainObjectMapper.getNewInstance();
		electionEventId = uuidGenerator.generate();

		final SetupPathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		electionEventContextPayloadService = mock(ElectionEventContextPayloadService.class);
		when(electionEventContextPayloadService.loadEncryptionGroup(anyString())).thenReturn(gqGroup);
		final SetupKeyPairFileRepository setupKeyPairFileRepository = new SetupKeyPairFileRepository(objectMapper, pathResolver,
				electionEventContextPayloadService);
		setupKeyPairService = new SetupKeyPairService(setupKeyPairFileRepository);

		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);
		final ElGamalMultiRecipientKeyPair setupKeyPair = elGamalGenerator.genRandomKeyPair(10);

		setupKeyPairService.save(electionEventId, setupKeyPair);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private ElGamalMultiRecipientKeyPair setupKeyPair;
		private SetupKeyPairService setupKeyPairServiceTempDir;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			final SetupPathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

			final SetupKeyPairFileRepository setupKeyPairFileRepository = new SetupKeyPairFileRepository(objectMapper, pathResolver,
					electionEventContextPayloadService);
			setupKeyPairServiceTempDir = new SetupKeyPairService(setupKeyPairFileRepository);
		}

		@BeforeEach
		void setUp() {
			final int numElements = secureRandom.nextInt(10) + 1;
			setupKeyPair = ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, numElements, random);
		}

		@Test
		@DisplayName("a valid key pair does not throw")
		void saveValidKeyPair() {
			assertDoesNotThrow(() -> setupKeyPairServiceTempDir.save(electionEventId, setupKeyPair));
		}

		@Test
		@DisplayName("with invalid election event id throws FailedValidationException")
		void saveInvalidElectionEventId() {
			assertThrows(FailedValidationException.class, () -> setupKeyPairServiceTempDir.save(INVALID_ELECTION_EVENT_ID, setupKeyPair));
		}

		@Test
		@DisplayName("with null setup key pair throws NullPointerException")
		void saveNullSetupKeyPair() {
			assertThrows(NullPointerException.class, () -> setupKeyPairServiceTempDir.save(electionEventId, null));
		}

	}

	@Nested
	@DisplayName("loading")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LoadTest {

		@Test
		@DisplayName("existing key pair returns it")
		void loadExistingKeyPair() {
			assertNotNull(setupKeyPairService.load(electionEventId));
		}

		@Test
		@DisplayName("invalid election event id")
		void loadInvalidElectionEventId() {
			assertThrows(FailedValidationException.class, () -> setupKeyPairService.load(INVALID_ELECTION_EVENT_ID));
		}

		@Test
		@DisplayName("non existing key pair throws IllegalArgumentException")
		void loadNonExistingKeyPair() {
			final String wrongElectionEventId = uuidGenerator.generate();
			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> setupKeyPairService.load(wrongElectionEventId));

			final String errorMessage = String.format("Setup key pair not found. [electionEventId: %s]", wrongElectionEventId);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}

	}

}

