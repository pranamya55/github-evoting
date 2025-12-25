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

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;

@DisplayName("ElectionEventContextPayloadFileRepository")
class ElectionEventContextPayloadFileRepositoryTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static ObjectMapper objectMapper;
	private static String electionEventId;
	private static String unsignedElectionEventId;
	private static ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator;
	private static ElectionEventContextPayloadFileRepository electionEventContextPayloadFileRepository;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {
		objectMapper = DomainObjectMapper.getNewInstance();
		final GqGroup gqGroup = GroupTestData.getLargeGqGroup();
		electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator(gqGroup);

		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();

		final ElectionEventContextPayload tmpPayload = electionEventContextPayloadGenerator.generate();
		final ElectionEventContextPayload unsignedElectionEventContextPayload = new ElectionEventContextPayload(tmpPayload.getEncryptionGroup(),
				tmpPayload.getSeed(), tmpPayload.getSmallPrimes(), tmpPayload.getElectionEventContext(), tmpPayload.getTenantId());

		unsignedElectionEventId = unsignedElectionEventContextPayload.getElectionEventContext().electionEventId();

		final PathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		electionEventContextPayloadFileRepository = new ElectionEventContextPayloadFileRepository(objectMapper, pathResolver);

		electionEventContextPayloadFileRepository.save(electionEventContextPayload);
		electionEventContextPayloadFileRepository.save(unsignedElectionEventContextPayload);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private ElectionEventContextPayloadFileRepository electionEventContextPayloadFileRepositoryTemp;

		private ElectionEventContextPayload electionEventContextPayload;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			final PathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

			electionEventContextPayloadFileRepositoryTemp = new ElectionEventContextPayloadFileRepository(objectMapper, pathResolver);
		}

		@BeforeEach
		void setUp() {
			electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		}

		@Test
		@DisplayName("valid election event context payload creates file")
		void save() {
			final Path savedPath = electionEventContextPayloadFileRepositoryTemp.save(electionEventContextPayload);

			assertTrue(Files.exists(savedPath));
		}

		@Test
		@DisplayName("null election event context payload throws NullPointerException")
		void saveNullElectionEventContext() {
			assertThrows(NullPointerException.class, () -> electionEventContextPayloadFileRepositoryTemp.save(null));
		}

	}

	@Nested
	@DisplayName("calling existsById")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ExistsByIdTest {

		@Test
		@DisplayName("for existing election event context payload returns true")
		void existingElectionEventContext() {
			assertTrue(electionEventContextPayloadFileRepository.existsById(electionEventId));
		}

		@Test
		@DisplayName("with invalid election event id throws FailedValidationException")
		void invalidElectionEventId() {
			assertThrows(FailedValidationException.class, () -> electionEventContextPayloadFileRepository.existsById("invalidId"));
		}

		@Test
		@DisplayName("for non existing election event context payload returns false")
		void nonExistingElectionEventContext() {
			final String nonExistingElectionEventId = uuidGenerator.generate();
			assertFalse(electionEventContextPayloadFileRepository.existsById(nonExistingElectionEventId));
		}

	}

	@Nested
	@DisplayName("calling findById")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class FindByIdTest {

		@Test
		@DisplayName("for existing election event context payload returns it")
		void existingElectionEventContext() {
			assertTrue(electionEventContextPayloadFileRepository.findById(electionEventId).isPresent());
		}

		@Test
		@DisplayName("for non existing election event context payload return empty optional")
		void nonExistingElectionEventContext() {
			final String nonExistingElectionEventId = uuidGenerator.generate();
			assertFalse(electionEventContextPayloadFileRepository.findById(nonExistingElectionEventId).isPresent());
		}

		@Test
		@DisplayName("for corrupted election event context payload throws NullPointerException")
		void corruptedElectionEventContext() {
			assertThrows(FailedValidationException.class, () -> electionEventContextPayloadFileRepository.findById(unsignedElectionEventId));
		}

	}

}
