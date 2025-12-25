/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

import ch.post.it.evoting.domain.generators.SetupComponentTallyDataPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.configuration.SetupComponentTallyDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;

@DisplayName("A SetupComponentTallyDataPayloadFileRepository")
class SetupComponentTallyDataPayloadFileRepositoryTest {
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final SetupComponentTallyDataPayloadGenerator generator = new SetupComponentTallyDataPayloadGenerator();

	private static final String NON_EXISTING_ELECTION_EVENT_ID = uuidGenerator.generate();

	private static ObjectMapper objectMapper;
	private static SetupComponentTallyDataPayloadFileRepository setupComponentTallyDataPayloadFileRepository;
	private static String existingElectionEventId;
	private static String verificationCardSetId;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {

		objectMapper = DomainObjectMapper.getNewInstance();

		final PathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		setupComponentTallyDataPayloadFileRepository = new SetupComponentTallyDataPayloadFileRepository(objectMapper, pathResolver);

		final SetupComponentTallyDataPayloadFileRepository repository = new SetupComponentTallyDataPayloadFileRepository(objectMapper, pathResolver);

		final SetupComponentTallyDataPayload setupComponentTallyDataPayload = generator.generate();
		existingElectionEventId = setupComponentTallyDataPayload.getElectionEventId();
		verificationCardSetId = setupComponentTallyDataPayload.getVerificationCardSetId();
		repository.save(setupComponentTallyDataPayload);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private SetupComponentTallyDataPayloadFileRepository setupComponentTallyDataPayloadFileRepositoryTemp;
		private SetupComponentTallyDataPayload setupComponentTallyDataPayload;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			final PathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
			setupComponentTallyDataPayloadFileRepositoryTemp = new SetupComponentTallyDataPayloadFileRepository(objectMapper, pathResolver);
		}

		@BeforeEach
		void setUp() {
			setupComponentTallyDataPayload = generator.generate(existingElectionEventId, verificationCardSetId, 3);
		}

		@Test
		@DisplayName("valid setup component tally data payload creates file")
		void save() {
			final Path savedPath = setupComponentTallyDataPayloadFileRepositoryTemp.save(setupComponentTallyDataPayload);

			assertTrue(Files.exists(savedPath));
		}

		@Test
		@DisplayName("null setup component tally data payload throws NullPointerException")
		void saveNullSetupComponentTallyData() {
			assertThrows(NullPointerException.class, () -> setupComponentTallyDataPayloadFileRepositoryTemp.save(null));
		}
	}

	@Nested
	@DisplayName("calling existsById")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ExistsByIdTest {

		@Test
		@DisplayName("for existing setup component tally data payload returns true")
		void existingSetupComponentTallyData() {
			assertTrue(setupComponentTallyDataPayloadFileRepository.existsById(existingElectionEventId, verificationCardSetId));
		}

		@Test
		@DisplayName("with null input throws NullPointerException")
		void nullInput() {
			assertThrows(NullPointerException.class,
					() -> setupComponentTallyDataPayloadFileRepository.existsById(null, verificationCardSetId));
			assertThrows(NullPointerException.class,
					() -> setupComponentTallyDataPayloadFileRepository.existsById(existingElectionEventId, null));
		}

		@Test
		@DisplayName("with invalid input throws FailedValidationException")
		void invalidInput() {
			assertThrows(FailedValidationException.class,
					() -> setupComponentTallyDataPayloadFileRepository.existsById("invalidId", verificationCardSetId));
			assertThrows(FailedValidationException.class,
					() -> setupComponentTallyDataPayloadFileRepository.existsById(existingElectionEventId, "invalidId"));
		}

		@Test
		@DisplayName("for non existing setup component tally data payload returns false")
		void nonExistingSetupComponentTallyData() {
			assertFalse(setupComponentTallyDataPayloadFileRepository.existsById(NON_EXISTING_ELECTION_EVENT_ID, verificationCardSetId));
		}

	}

	@Nested
	@DisplayName("calling findById")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class FindByIdTest {

		@Test
		@DisplayName("for existing setup component tally data payload returns it")
		void existingSetupComponentTallyData() {
			assertTrue(setupComponentTallyDataPayloadFileRepository.findById(existingElectionEventId, verificationCardSetId).isPresent());
		}

		@Test
		@DisplayName("for non existing setup component tally data payload return empty optional")
		void nonExistingSetupComponentTallyData() {
			assertFalse(setupComponentTallyDataPayloadFileRepository.findById(NON_EXISTING_ELECTION_EVENT_ID, verificationCardSetId).isPresent());
		}

	}

}
