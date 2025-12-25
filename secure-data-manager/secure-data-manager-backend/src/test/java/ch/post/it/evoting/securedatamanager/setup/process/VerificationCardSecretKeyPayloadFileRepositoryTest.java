/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

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

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

@DisplayName("A VerificationCardSecretKeyFileRepository")
class VerificationCardSecretKeyPayloadFileRepositoryTest {
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static final String NON_EXISTING_ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String EXISTING_ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_SET_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_ID = uuidGenerator.generate();

	private static ObjectMapper objectMapper;
	private static VerificationCardSecretKeyPayloadFileRepository verificationCardSecretKeyPayloadFileRepository;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {

		objectMapper = DomainObjectMapper.getNewInstance();

		final SetupPathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
		pathResolver.resolveVerificationCardSetPath(EXISTING_ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID);

		verificationCardSecretKeyPayloadFileRepository = new VerificationCardSecretKeyPayloadFileRepository(objectMapper, pathResolver);

		final VerificationCardSecretKeyPayloadFileRepository repository = new VerificationCardSecretKeyPayloadFileRepository(objectMapper,
				pathResolver);

		repository.save(validVerificationCardSecretKeyPayload(EXISTING_ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, VERIFICATION_CARD_ID));
	}

	private static VerificationCardSecretKeyPayload validVerificationCardSecretKeyPayload(final String electionEventId,
			final String verificationCardSetId, final String verificationCardId) {
		final ImmutableList<VerificationCardSecretKey> verificationCardSecretKeys = ImmutableList.of(
				new VerificationCardSecretKey(verificationCardId, SerializationUtils.getPrivateKey()));
		return new VerificationCardSecretKeyPayload(SerializationUtils.getGqGroup(), electionEventId, verificationCardSetId,
				verificationCardSecretKeys);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private VerificationCardSecretKeyPayloadFileRepository verificationCardSecretKeyPayloadFileRepositoryTemp;
		private VerificationCardSecretKeyPayload verificationCardSecretKeyPayload;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			final SetupPathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
			pathResolver.resolveVerificationCardSetPath(EXISTING_ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID);

			verificationCardSecretKeyPayloadFileRepositoryTemp = new VerificationCardSecretKeyPayloadFileRepository(objectMapper, pathResolver);
		}

		@BeforeEach
		void setUp() {
			verificationCardSecretKeyPayload = validVerificationCardSecretKeyPayload(EXISTING_ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID,
					VERIFICATION_CARD_ID);
		}

		@Test
		@DisplayName("valid verification card secret key creates file")
		void save() {
			final Path savedPath = verificationCardSecretKeyPayloadFileRepositoryTemp.save(verificationCardSecretKeyPayload);

			assertTrue(Files.exists(savedPath));
		}

		@Test
		@DisplayName("null verification card secret key throws NullPointerException")
		void saveNullVerificationCardSecretKeyPayload() {
			assertThrows(NullPointerException.class, () -> verificationCardSecretKeyPayloadFileRepositoryTemp.save(null));
		}
	}

	@Nested
	@DisplayName("calling findById")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class FindByIdTest {

		@Test
		@DisplayName("for existing verification card secret key returns it")
		void existingVerificationCardSecretKeyPayload() {
			assertTrue(verificationCardSecretKeyPayloadFileRepository.findById(EXISTING_ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID
			).isPresent());
		}

		@Test
		@DisplayName("for non existing verification card secret key return empty optional")
		void nonExistingVerificationCardSecretKeyPayload() {
			assertFalse(
					verificationCardSecretKeyPayloadFileRepository.findById(NON_EXISTING_ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID
					).isPresent());
		}

	}
}
