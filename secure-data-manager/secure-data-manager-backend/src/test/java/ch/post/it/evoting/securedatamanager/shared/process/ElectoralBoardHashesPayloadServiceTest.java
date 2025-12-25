/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.configuration.ElectoralBoardHashesPayload;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;

@DisplayName("An ElectoralBoardHashesPayloadService")
class ElectoralBoardHashesPayloadServiceTest {

	private static final String ELECTION_EVENT_ID = "DF384F4747A0819718C22CC57ED8183D";
	private static final String MISSING_ELECTION_EVENT_ID = "414BD34DCF6E4DE4B771A92FA3849D3D";
	private static final String INVALID_ID = "invalidId";

	private static ObjectMapper objectMapper;
	private static PathResolver pathResolver;
	private static ElectoralBoardHashesPayloadService electoralBoardHashesPayloadService;
	private static ElectoralBoardHashesPayload electoralBoardHashesPayload;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {
		objectMapper = DomainObjectMapper.getNewInstance();

		final PathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		final ElectoralBoardHashesPayloadFileRepository electoralBoardHashesPayloadFileRepository =
				new ElectoralBoardHashesPayloadFileRepository(objectMapper, pathResolver);

		electoralBoardHashesPayload = validElectoralBoardHashesPayload();
		electoralBoardHashesPayloadFileRepository.save(electoralBoardHashesPayload);

		electoralBoardHashesPayloadService = new ElectoralBoardHashesPayloadService(electoralBoardHashesPayloadFileRepository
		);
	}

	private static ElectoralBoardHashesPayload validElectoralBoardHashesPayload() {
		final ImmutableList<ImmutableByteArray> electoralBoardHashes = ImmutableList.of(
				new ImmutableByteArray("Password_ElectoralBoard1_2".getBytes(StandardCharsets.UTF_8)),
				new ImmutableByteArray("Password_ElectoralBoard2_2".getBytes(StandardCharsets.UTF_8)));
		final ElectoralBoardHashesPayload electoralBoardHashesPayload =
				new ElectoralBoardHashesPayload(ELECTION_EVENT_ID, electoralBoardHashes);

		final ImmutableByteArray signature = ImmutableByteArray.of((byte) 1, (byte) 2);
		final CryptoPrimitivesSignature electoralBoardHashesPayloadSignature = new CryptoPrimitivesSignature(signature);
		electoralBoardHashesPayload.setSignature(electoralBoardHashesPayloadSignature);

		return electoralBoardHashesPayload;
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private ElectoralBoardHashesPayload electoralBoardHashesPayload;

		private ElectoralBoardHashesPayloadService electoralBoardHashesPayloadServiceTemp;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
			final ElectoralBoardHashesPayloadFileRepository electoralBoardHashesPayloadFileRepositoryTemp = new ElectoralBoardHashesPayloadFileRepository(
					objectMapper, pathResolver);

			electoralBoardHashesPayloadServiceTemp = new ElectoralBoardHashesPayloadService(electoralBoardHashesPayloadFileRepositoryTemp
			);
		}

		@BeforeEach
		void setUp() {
			final ImmutableList<ImmutableByteArray> electoralBoardHashes = ImmutableList.of(
					ImmutableByteArray.of((byte) 9),
					ImmutableByteArray.of((byte) 2));
			electoralBoardHashesPayload = new ElectoralBoardHashesPayload(ELECTION_EVENT_ID, electoralBoardHashes);
		}

		@Test
		@DisplayName("a valid payload does not throw")
		void saveValidPayload() {
			assertDoesNotThrow(() -> electoralBoardHashesPayloadServiceTemp.save(electoralBoardHashesPayload));

			assertTrue(Files.exists(
					pathResolver.resolveElectionEventPath(ELECTION_EVENT_ID).resolve(ElectoralBoardHashesPayloadFileRepository.PAYLOAD_FILE_NAME)));
		}

		@Test
		@DisplayName("a invalid election event id or electoral board hashes throws")
		void saveNullPayload() {
			assertThrows(NullPointerException.class, () -> electoralBoardHashesPayloadServiceTemp.save(null));
		}
	}

	@Nested
	@DisplayName("calling exist")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ExistTest {

		@Test
		@DisplayName("for valid election event returns true")
		void existValidElectionEvent() {
			assertTrue(electoralBoardHashesPayloadService.exist(ELECTION_EVENT_ID));
		}

		@Test
		@DisplayName("for invalid election event id throws FailedValidationException")
		void existInvalidElectionEvent() {
			assertThrows(FailedValidationException.class, () -> electoralBoardHashesPayloadService.exist(INVALID_ID));
		}

		@Test
		@DisplayName("for non existing election event returns false")
		void existNonExistingElectionEvent() {
			assertFalse(electoralBoardHashesPayloadService.exist(MISSING_ELECTION_EVENT_ID));
		}

	}

	@Nested
	@DisplayName("loading")
	@TestInstance(TestInstance.Lifecycle.PER_METHOD)
	class LoadTest {

		@Test
		@DisplayName("existing election event with valid signature returns expected electoral board hashes")
		void loadExistingElectionEventValidSignature() {
			final ElectoralBoardHashesPayload loadedElectoralBoardHashesPayload = electoralBoardHashesPayloadService.load(ELECTION_EVENT_ID);
			final ImmutableList<ImmutableByteArray> electoralBoardHashes = loadedElectoralBoardHashesPayload.getElectoralBoardHashes();

			assertNotNull(loadedElectoralBoardHashesPayload);
			assertEquals(ElectoralBoardHashesPayloadServiceTest.electoralBoardHashesPayload.getElectoralBoardHashes().get(0),
					electoralBoardHashes.get(0));
			assertEquals(ElectoralBoardHashesPayloadServiceTest.electoralBoardHashesPayload.getElectoralBoardHashes().get(1),
					electoralBoardHashes.get(1));
		}

		@Test
		@DisplayName("invalid election event id throws FailedValidationException")
		void loadInvalidElectionEventId() {
			assertThrows(NullPointerException.class, () -> electoralBoardHashesPayloadService.load(null));
			assertThrows(FailedValidationException.class, () -> electoralBoardHashesPayloadService.load(INVALID_ID));
		}

		@Test
		@DisplayName("existing election event with missing payload throws IllegalStateException")
		void loadMissingPayload() {
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> electoralBoardHashesPayloadService.load(MISSING_ELECTION_EVENT_ID));

			final String errorMessage = String.format("Requested electoral board hashes payload is not present. [electionEventId: %s]",
					MISSING_ELECTION_EVENT_ID);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}

	}

}
