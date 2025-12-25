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
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;

@DisplayName("An SetupComponentPublicKeysPayloadService")
class SetupComponentPublicKeysPayloadServiceTest {

	private static final String WRONG_ELECTION_EVENT_ID = "414BD34DCF6E4DE4B771A92FA3849D3D";
	private static final String NOT_EXISTING_ELECTION_EVENT_PAYLOAD = "614BD34DCF6E4DE4B771A92FA3849D3D";
	private static final String INVALID_ID = "invalidId";

	private static String electionEventId;
	private static ObjectMapper objectMapper;
	private static PathResolver pathResolver;
	private static SetupComponentPublicKeysPayload setupComponentPublicKeysPayload;
	private static SetupComponentPublicKeysPayloadService setupComponentPublicKeysPayloadService;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {
		objectMapper = DomainObjectMapper.getNewInstance();

		final PathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		final SetupComponentPublicKeysPayloadFileRepository setupComponentPublicKeysPayloadFileRepository = new SetupComponentPublicKeysPayloadFileRepository(
				objectMapper, pathResolver);

		setupComponentPublicKeysPayloadService = new SetupComponentPublicKeysPayloadService(setupComponentPublicKeysPayloadFileRepository);

		final SetupComponentPublicKeysPayloadGenerator setupComponentPublicKeysPayloadGenerator = new SetupComponentPublicKeysPayloadGenerator();
		setupComponentPublicKeysPayload = setupComponentPublicKeysPayloadGenerator.generate();
		electionEventId = setupComponentPublicKeysPayload.getElectionEventId();
		setupComponentPublicKeysPayloadService.save(setupComponentPublicKeysPayload);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private SetupComponentPublicKeysPayloadService setupComponentPublicKeysPayloadServiceTemp;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
			final SetupComponentPublicKeysPayloadFileRepository electionEventContextPayloadFileRepositoryTemp = new SetupComponentPublicKeysPayloadFileRepository(
					objectMapper, pathResolver);

			setupComponentPublicKeysPayloadServiceTemp = new SetupComponentPublicKeysPayloadService(electionEventContextPayloadFileRepositoryTemp);
		}

		@Test
		@DisplayName("a valid payload does not throw")
		void saveValidPayload() {
			assertDoesNotThrow(() -> setupComponentPublicKeysPayloadServiceTemp.save(setupComponentPublicKeysPayload));

			assertTrue(Files.exists(
					pathResolver.resolveElectionEventPath(electionEventId)
							.resolve(SetupComponentPublicKeysPayloadFileRepository.PAYLOAD_FILE_NAME)));
		}

		@Test
		@DisplayName("a null payload throws NullPointerException")
		void saveNullPayload() {
			assertThrows(NullPointerException.class, () -> setupComponentPublicKeysPayloadServiceTemp.save(null));
		}

	}

	@Nested
	@DisplayName("calling exist")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ExistTest {

		@Test
		@DisplayName("for valid election event returns true")
		void existValidElectionEvent() {
			assertTrue(setupComponentPublicKeysPayloadService.exist(electionEventId));
		}

		@Test
		@DisplayName("for invalid election event id throws FailedValidationException")
		void existInvalidElectionEvent() {
			assertThrows(FailedValidationException.class, () -> setupComponentPublicKeysPayloadService.exist(INVALID_ID));
		}

		@Test
		@DisplayName("for non existing election event returns false")
		void existNonExistingElectionEvent() {
			assertFalse(setupComponentPublicKeysPayloadService.exist(WRONG_ELECTION_EVENT_ID));
		}

	}

	@Nested
	@DisplayName("loading")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LoadTest {

		@Test
		@DisplayName("existing election event returns expected election event context payload")
		void loadExistingElectionEvent() {
			assertNotNull(setupComponentPublicKeysPayloadService.load(electionEventId));
		}

		@Test
		@DisplayName("invalid election event id throws FailedValidationException")
		void loadInvalidElectionEventId() {
			assertThrows(FailedValidationException.class, () -> setupComponentPublicKeysPayloadService.load(INVALID_ID));
		}

		@Test
		@DisplayName("existing election event with missing payload throws IllegalStateException")
		void loadMissingPayload() {
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> setupComponentPublicKeysPayloadService.load(NOT_EXISTING_ELECTION_EVENT_PAYLOAD));

			final String errorMessage = String.format("Requested setup component public keys payload is not present. [electionEventId: %s]",
					NOT_EXISTING_ELECTION_EVENT_PAYLOAD);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}

	}

}
