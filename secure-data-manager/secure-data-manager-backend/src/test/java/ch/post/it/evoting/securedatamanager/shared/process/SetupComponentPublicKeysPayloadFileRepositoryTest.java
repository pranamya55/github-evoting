/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.securedatamanager.shared.process.SetupComponentPublicKeysPayloadFileRepository.PAYLOAD_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;

@DisplayName("SetupComponentPublicKeysPayloadFileRepository")
class SetupComponentPublicKeysPayloadFileRepositoryTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String NON_EXISTING_ELECTION_EVENT_ID = uuidGenerator.generate();

	private static String existingElectionEventId;
	private static PathResolver pathResolver;
	private static ObjectMapper objectMapper;
	private static SetupComponentPublicKeysPayload setupComponentPublicKeysPayload;
	private static SetupComponentPublicKeysPayloadFileRepository setupComponentPublicKeysPayloadFileRepository;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {
		objectMapper = spy(DomainObjectMapper.getNewInstance());

		pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
		setupComponentPublicKeysPayloadFileRepository = new SetupComponentPublicKeysPayloadFileRepository(objectMapper, pathResolver);

		final SetupComponentPublicKeysPayloadGenerator setupComponentPublicKeysPayloadGenerator = new SetupComponentPublicKeysPayloadGenerator();
		setupComponentPublicKeysPayload = setupComponentPublicKeysPayloadGenerator.generate();
		setupComponentPublicKeysPayloadFileRepository.save(setupComponentPublicKeysPayload);
		existingElectionEventId = setupComponentPublicKeysPayload.getElectionEventId();
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private SetupComponentPublicKeysPayloadFileRepository setupComponentPublicKeysPayloadFileRepository1;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			final PathResolver setupPathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
			setupComponentPublicKeysPayloadFileRepository1 = new SetupComponentPublicKeysPayloadFileRepository(objectMapper, setupPathResolver);
		}

		@Test
		@DisplayName("valid election event context payload creates file")
		void save() {
			final Path savedPath = setupComponentPublicKeysPayloadFileRepository1.save(setupComponentPublicKeysPayload);

			assertTrue(Files.exists(savedPath));
		}

		@Test
		@DisplayName("null election event context payload throws NullPointerException")
		void saveNullElectionEventContext() {
			assertThrows(NullPointerException.class, () -> setupComponentPublicKeysPayloadFileRepository1.save(null));
		}

	}

	@Nested
	@DisplayName("calling existsById")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ExistsByIdTest {

		@Test
		@DisplayName("for existing election event context payload returns true")
		void existingElectionEventContext() {
			assertTrue(setupComponentPublicKeysPayloadFileRepository.existsById(existingElectionEventId));
		}

		@Test
		@DisplayName("with invalid election event id throws FailedValidationException")
		void invalidElectionEventId() {
			assertThrows(FailedValidationException.class, () -> setupComponentPublicKeysPayloadFileRepository.existsById("invalidId"));
		}

		@Test
		@DisplayName("for non existing election event context payload returns false")
		void nonExistingElectionEventContext() {
			assertFalse(setupComponentPublicKeysPayloadFileRepository.existsById(NON_EXISTING_ELECTION_EVENT_ID));
		}

	}

	@Nested
	@DisplayName("calling findById")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class FindByIdTest {

		@Test
		@DisplayName("for existing election event context payload returns it")
		void existingElectionEventContext() throws IOException {
			doCallRealMethod().when(objectMapper).readValue((File) any(), eq(SetupComponentPublicKeysPayload.class));

			assertTrue(setupComponentPublicKeysPayloadFileRepository.findById(existingElectionEventId).isPresent());
		}

		@Test
		@DisplayName("for non existing election event context payload return empty optional")
		void nonExistingElectionEventContext() {
			assertFalse(setupComponentPublicKeysPayloadFileRepository.findById(NON_EXISTING_ELECTION_EVENT_ID).isPresent());
		}

		@Test
		@DisplayName("for corrupted election event context payload throws UncheckedIOException")
		void corruptedElectionEventContext() throws IOException {
			doThrow(IOException.class).when(objectMapper).readValue((File) any(), eq(SetupComponentPublicKeysPayload.class));

			final UncheckedIOException exception = assertThrows(UncheckedIOException.class,
					() -> setupComponentPublicKeysPayloadFileRepository.findById(existingElectionEventId));

			final Path electionEventPath = pathResolver.resolveElectionEventPath(existingElectionEventId);
			final Path setupComponentPublicKeysPath = electionEventPath.resolve(PAYLOAD_FILE_NAME);
			final String errorMessage = String.format("Failed to deserialize setup component public keys payload. [electionEventId: %s, path: %s]",
					existingElectionEventId, setupComponentPublicKeysPath);

			assertEquals(errorMessage, exception.getMessage());
		}

	}

}
