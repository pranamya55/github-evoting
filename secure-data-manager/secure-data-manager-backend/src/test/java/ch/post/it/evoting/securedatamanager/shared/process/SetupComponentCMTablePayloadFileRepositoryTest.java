/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.configuration.SetupComponentCMTablePayload;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;

@DisplayName("SetupComponentCMTablePayloadFileRepository")
class SetupComponentCMTablePayloadFileRepositoryTest {

	private static final String ELECTION_EVENT_ID = "8B733B29BE224C01B4D1F82FE2A5FBEA";
	private static final String CORRUPTED_ELECTION_EVENT_ID = "1B733B29BE224C01B4D1F82FE2A5FBEA";
	private static final String VERIFICATION_CARD_SET_ID = "0B5BF763C0D44D66B775399D08AE4811";
	private static final String CORRUPTED_VERIFICATION_CARD_SET_ID = "1B5BF763C0D44D66B775399D08AE4811";
	private static final String NON_EXISTING_ID = "ABCDEF0123456789ABCDEF0123456789";

	private static ObjectMapper objectMapper;

	private static SetupComponentCMTablePayloadFileRepository setupComponentCMTablePayloadFileRepository;

	@BeforeAll
	static void setUpAll() throws URISyntaxException, IOException {
		objectMapper = DomainObjectMapper.getNewInstance();
		final Path path = Paths.get(
				SetupComponentCMTablePayloadFileRepositoryTest.class.getResource("/returnCodesMappingTablePayloadFileRepositoryTest/").toURI());
		final PathResolver pathResolver = new SetupPathResolver(path, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
		setupComponentCMTablePayloadFileRepository = new SetupComponentCMTablePayloadFileRepository(objectMapper, pathResolver);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private SetupComponentCMTablePayloadFileRepository setupComponentCMTablePayloadFileRepositoryTemp;

		private SetupComponentCMTablePayload setupComponentCMTablePayload;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			final PathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
			setupComponentCMTablePayloadFileRepositoryTemp = new SetupComponentCMTablePayloadFileRepository(objectMapper, pathResolver);
		}

		@BeforeEach
		void setUp() {
			final Random random = RandomFactory.createRandom();
			final Alphabet base64Alphabet = Base64Alphabet.getInstance();
			final ImmutableMap<String, String> map = ImmutableMap.of(
					random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet),
					random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet)
			);

			// Create payload.
			setupComponentCMTablePayload = new SetupComponentCMTablePayload.Builder()
					.setElectionEventId(ELECTION_EVENT_ID)
					.setVerificationCardSetId(VERIFICATION_CARD_SET_ID)
					.setReturnCodesMappingTable(map)
					.build();
		}

		@Test
		@DisplayName("valid payload creates file")
		void save() {
			final Path savedPath = setupComponentCMTablePayloadFileRepositoryTemp.save(setupComponentCMTablePayload);

			assertTrue(Files.exists(savedPath));
		}

		@Test
		@DisplayName("null payload throws NullPointerException")
		void saveNullPayload() {
			assertThrows(NullPointerException.class, () -> setupComponentCMTablePayloadFileRepositoryTemp.save(null));
		}

	}

	@Nested
	@DisplayName("calling findById")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class FindByIdTest {

		@Test
		@DisplayName("for existing payload returns it")
		void existingPayload() {
			assertTrue(setupComponentCMTablePayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(ELECTION_EVENT_ID,
					VERIFICATION_CARD_SET_ID).isPresent());
		}

		@Test
		@DisplayName("for not existing payload return empty optional")
		void nonExistingPayload() {
			assertAll(
					() -> assertFalse(setupComponentCMTablePayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(NON_EXISTING_ID,
							VERIFICATION_CARD_SET_ID).isPresent()),
					() -> assertFalse(setupComponentCMTablePayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(ELECTION_EVENT_ID,
							NON_EXISTING_ID).isPresent())
			);
		}

		@Test
		@DisplayName("for corrupted payload throws UncheckedIOException")
		void corruptedPayload() {
			final UncheckedIOException exception = assertThrows(UncheckedIOException.class,
					() -> setupComponentCMTablePayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(CORRUPTED_ELECTION_EVENT_ID,
							CORRUPTED_VERIFICATION_CARD_SET_ID));

			final String errorMessage = String.format(
					"Failed to deserialize setup component CMTable payload. [electionEventId: %s, verificationCardSetId: %s]",
					CORRUPTED_ELECTION_EVENT_ID, CORRUPTED_VERIFICATION_CARD_SET_ID);

			assertEquals(errorMessage, exception.getMessage());
		}

	}

}
