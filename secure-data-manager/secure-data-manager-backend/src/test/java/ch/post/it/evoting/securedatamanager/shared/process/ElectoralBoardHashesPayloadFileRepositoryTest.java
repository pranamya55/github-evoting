/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoardHashesPayloadFileRepository.PAYLOAD_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
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

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.domain.configuration.ElectoralBoardHashesPayload;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;

@DisplayName("An ElectoralBoardHashesPayloadFileRepository")
class ElectoralBoardHashesPayloadFileRepositoryTest {

	private static final String NON_EXISTING_ELECTION_EVENT_ID = "414BD34DCF6E4DE4B771A92FA3849D3D";
	private static final String EXISTING_ELECTION_EVENT_ID = "DF384F4747A0819718C22CC57ED8183D";
	private static final String CORRUPTED_ELECTION_EVENT_ID = "514BD34DCF6E4DE4B771A92FA3849D3D";
	private static final ImmutableList<ImmutableByteArray> ELECTORAL_BOARD_HASHES = ImmutableList.of(
			new ImmutableByteArray("Password_ElectoralBoard1_2".getBytes(StandardCharsets.UTF_8)),
			new ImmutableByteArray("Password_ElectoralBoard2_2".getBytes(StandardCharsets.UTF_8)));

	private static PathResolver pathResolver;
	private static ObjectMapper objectMapper;
	private static ElectoralBoardHashesPayloadFileRepository electoralBoardHashesPayloadFileRepository;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {

		objectMapper = DomainObjectMapper.getNewInstance();

		pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		electoralBoardHashesPayloadFileRepository = new ElectoralBoardHashesPayloadFileRepository(objectMapper, pathResolver);

		final ElectoralBoardHashesPayloadFileRepository repository = new ElectoralBoardHashesPayloadFileRepository(objectMapper, pathResolver);

		repository.save(validElectoralBoardHashesPayload());
		repository.save(unsignedElectoralBoardHashesPayload());
	}

	private static ElectoralBoardHashesPayload validElectoralBoardHashesPayload() {
		final ElectoralBoardHashesPayload electoralBoardHashesPayload =
				new ElectoralBoardHashesPayload(EXISTING_ELECTION_EVENT_ID, ELECTORAL_BOARD_HASHES);

		final ImmutableByteArray signature = ImmutableByteArray.of((byte) 1, (byte) 2);
		final CryptoPrimitivesSignature electoralBoardHashesPayloadSignature = new CryptoPrimitivesSignature(signature);
		electoralBoardHashesPayload.setSignature(electoralBoardHashesPayloadSignature);

		return electoralBoardHashesPayload;
	}

	private static ElectoralBoardHashesPayload unsignedElectoralBoardHashesPayload() {
		return new ElectoralBoardHashesPayload(CORRUPTED_ELECTION_EVENT_ID, ELECTORAL_BOARD_HASHES);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private ElectoralBoardHashesPayloadFileRepository electoralBoardHashesPayloadFileRepositoryTemp;

		private ElectoralBoardHashesPayload electoralBoardHashesPayload;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			final PathResolver setupPathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
			electoralBoardHashesPayloadFileRepositoryTemp = new ElectoralBoardHashesPayloadFileRepository(objectMapper, setupPathResolver);
		}

		@BeforeEach
		void setUp() {
			electoralBoardHashesPayload = getValidElectoralBoardHashesPayload();
		}

		@Test
		@DisplayName("valid electoral board hashes payload creates file")
		void save() {
			final Path savedPath = electoralBoardHashesPayloadFileRepositoryTemp.save(electoralBoardHashesPayload);

			assertTrue(Files.exists(savedPath));
		}

		@Test
		@DisplayName("null electoral board hashes payload throws NullPointerException")
		void saveNullElectionEventContext() {
			assertThrows(NullPointerException.class, () -> electoralBoardHashesPayloadFileRepositoryTemp.save(null));
		}

		private ElectoralBoardHashesPayload getValidElectoralBoardHashesPayload() {
			final ElectoralBoardHashesPayload validElectoralBoardHashesPayload = new ElectoralBoardHashesPayload(EXISTING_ELECTION_EVENT_ID,
					ELECTORAL_BOARD_HASHES);

			final ImmutableByteArray payloadHash = HashFactory.createHash().recursiveHash(validElectoralBoardHashesPayload);
			final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
			validElectoralBoardHashesPayload.setSignature(signature);

			return validElectoralBoardHashesPayload;
		}
	}

	@Nested
	@DisplayName("calling existsById")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ExistsByIdTest {

		@Test
		@DisplayName("for existing electoral board hashes payload returns true")
		void existingElectionEventContext() {
			assertTrue(electoralBoardHashesPayloadFileRepository.existsById(EXISTING_ELECTION_EVENT_ID));
		}

		@Test
		@DisplayName("with invalid election event id throws FailedValidationException")
		void invalidElectionEventId() {
			assertThrows(FailedValidationException.class, () -> electoralBoardHashesPayloadFileRepository.existsById("invalidId"));
		}

		@Test
		@DisplayName("for non existing electoral board hashes payload returns false")
		void nonExistingElectionEventContext() {
			assertFalse(electoralBoardHashesPayloadFileRepository.existsById(NON_EXISTING_ELECTION_EVENT_ID));
		}

	}

	@Nested
	@DisplayName("calling findById")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class FindByIdTest {

		@Test
		@DisplayName("for existing electoral board hashes payload returns it")
		void existingElectionEventContext() {
			assertTrue(electoralBoardHashesPayloadFileRepository.findById(EXISTING_ELECTION_EVENT_ID).isPresent());
		}

		@Test
		@DisplayName("for non existing electoral board hashes payload return empty optional")
		void nonExistingElectionEventContext() {
			assertFalse(electoralBoardHashesPayloadFileRepository.findById(NON_EXISTING_ELECTION_EVENT_ID).isPresent());
		}

		@Test
		@DisplayName("for corrupted electoral board hashes payload throws UncheckedIOException")
		void corruptedElectionEventContext() {
			final UncheckedIOException exception = assertThrows(UncheckedIOException.class,
					() -> electoralBoardHashesPayloadFileRepository.findById(CORRUPTED_ELECTION_EVENT_ID));

			final Path electionEventPath = pathResolver.resolveElectionEventPath(CORRUPTED_ELECTION_EVENT_ID);
			final Path electionEventContextPath = electionEventPath.resolve(PAYLOAD_FILE_NAME);
			final String errorMessage = String.format("Failed to deserialize electoral board hashes payload. [electionEventId: %s, path: %s]",
					CORRUPTED_ELECTION_EVENT_ID, electionEventContextPath);

			assertEquals(errorMessage, exception.getMessage());
		}

	}

}
