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

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.configuration.SetupComponentVerificationCardKeystoresPayload;
import ch.post.it.evoting.domain.configuration.VerificationCardKeystore;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;

@DisplayName("A SetupComponentVerificationCardKeystoresPayloadFileRepository")
class SetupComponentVerificationCardKeystoresPayloadFileRepositoryTest {
	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final Hash hashService = HashFactory.createHash();

	private static final String NON_EXISTING_ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String EXISTING_ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_SET_ID = uuidGenerator.generate();

	private static ObjectMapper objectMapper;
	private static SetupComponentVerificationCardKeystoresPayloadFileRepository setupComponentVerificationCardKeystoresPayloadFileRepository;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {

		objectMapper = DomainObjectMapper.getNewInstance();

		final PathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		setupComponentVerificationCardKeystoresPayloadFileRepository = new SetupComponentVerificationCardKeystoresPayloadFileRepository(objectMapper,
				pathResolver);

		final SetupComponentVerificationCardKeystoresPayloadFileRepository repository = new SetupComponentVerificationCardKeystoresPayloadFileRepository(
				objectMapper, pathResolver);

		repository.save(validSetupComponentVerificationCardKeystoresPayload(EXISTING_ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));
	}

	private static SetupComponentVerificationCardKeystoresPayload validSetupComponentVerificationCardKeystoresPayload(final String electionEventId,
			final String verificationCardSetId) {

		final ImmutableList<VerificationCardKeystore> verificationCardKeystores = ImmutableList.of(
				new VerificationCardKeystore(uuidGenerator.generate(),
						"%s=".formatted(random.genRandomString(571, base64Alphabet))),
				new VerificationCardKeystore(uuidGenerator.generate(),
						"%s=".formatted(random.genRandomString(571, base64Alphabet))),
				new VerificationCardKeystore(uuidGenerator.generate(),
						"%s=".formatted(random.genRandomString(571, base64Alphabet)))
		);

		final SetupComponentVerificationCardKeystoresPayload setupComponentVerificationCardKeystoresPayload = new SetupComponentVerificationCardKeystoresPayload(
				electionEventId, verificationCardSetId, verificationCardKeystores);

		final ImmutableByteArray payloadHash = hashService.recursiveHash(setupComponentVerificationCardKeystoresPayload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		setupComponentVerificationCardKeystoresPayload.setSignature(signature);

		return setupComponentVerificationCardKeystoresPayload;
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private SetupComponentVerificationCardKeystoresPayloadFileRepository setupComponentVerificationCardKeystoresPayloadFileRepositoryTemp;
		private SetupComponentVerificationCardKeystoresPayload setupComponentVerificationCardKeystoresPayload;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			final PathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
			setupComponentVerificationCardKeystoresPayloadFileRepositoryTemp = new SetupComponentVerificationCardKeystoresPayloadFileRepository(
					objectMapper, pathResolver);
		}

		@BeforeEach
		void setUp() {
			setupComponentVerificationCardKeystoresPayload = validSetupComponentVerificationCardKeystoresPayload(EXISTING_ELECTION_EVENT_ID,
					VERIFICATION_CARD_SET_ID);
		}

		@Test
		@DisplayName("valid setup component verification card keystores payload creates file")
		void save() {
			final Path savedPath = setupComponentVerificationCardKeystoresPayloadFileRepositoryTemp.save(
					setupComponentVerificationCardKeystoresPayload);

			assertTrue(Files.exists(savedPath));
		}

		@Test
		@DisplayName("null setup component verification card keystores payload throws NullPointerException")
		void saveNullSetupComponentVerificationCardKeystoresPayload() {
			assertThrows(NullPointerException.class, () -> setupComponentVerificationCardKeystoresPayloadFileRepositoryTemp.save(null));
		}
	}

	@Nested
	@DisplayName("calling existsById")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ExistsByIdTest {

		@Test
		@DisplayName("for existing setup component verification card keystores payload returns true")
		void existingSetupComponentVerificationCardKeystoresPayload() {
			assertTrue(setupComponentVerificationCardKeystoresPayloadFileRepository.existsById(EXISTING_ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));
		}

		@Test
		@DisplayName("with null input throws NullPointerException")
		void nullInput() {
			assertThrows(NullPointerException.class,
					() -> setupComponentVerificationCardKeystoresPayloadFileRepository.existsById(null, VERIFICATION_CARD_SET_ID));
			assertThrows(NullPointerException.class,
					() -> setupComponentVerificationCardKeystoresPayloadFileRepository.existsById(EXISTING_ELECTION_EVENT_ID, null));
		}

		@Test
		@DisplayName("with invalid input throws FailedValidationException")
		void invalidInput() {
			assertThrows(FailedValidationException.class,
					() -> setupComponentVerificationCardKeystoresPayloadFileRepository.existsById("invalidId", VERIFICATION_CARD_SET_ID));
			assertThrows(FailedValidationException.class,
					() -> setupComponentVerificationCardKeystoresPayloadFileRepository.existsById(EXISTING_ELECTION_EVENT_ID, "invalidId"));
		}

		@Test
		@DisplayName("for non existing setup component verification card keystores payload returns false")
		void nonExistingSetupComponentVerificationCardKeystoresPayload() {
			assertFalse(
					setupComponentVerificationCardKeystoresPayloadFileRepository.existsById(NON_EXISTING_ELECTION_EVENT_ID,
							VERIFICATION_CARD_SET_ID));
		}

	}

	@Nested
	@DisplayName("calling findById")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class FindByIdTest {

		@Test
		@DisplayName("for existing setup component verification card keystores payload returns it")
		void existingSetupComponentVerificationCardKeystoresPayload() {
			assertTrue(setupComponentVerificationCardKeystoresPayloadFileRepository.findById(EXISTING_ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID)
					.isPresent());
		}

		@Test
		@DisplayName("for non existing setup component verification card keystores payload return empty optional")
		void nonExistingSetupComponentVerificationCardKeystoresPayload() {
			assertFalse(
					setupComponentVerificationCardKeystoresPayloadFileRepository.findById(NON_EXISTING_ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID)
							.isPresent());
		}

	}

}
