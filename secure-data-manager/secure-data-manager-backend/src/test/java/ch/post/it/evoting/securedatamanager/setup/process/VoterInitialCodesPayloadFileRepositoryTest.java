/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodes;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodesPayload;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

@DisplayName("A VoterInitialCodesPayloadFileRepository")
class VoterInitialCodesPayloadFileRepositoryTest {
	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static final String NON_EXISTING_ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String EXISTING_ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String EXISTING_VERIFICATION_CARD_SET_ID = uuidGenerator.generate();
	private static final int NUMBER_OF_VOTER_INITIAL_CODES = 3;

	private static ObjectMapper objectMapper;
	private static VoterInitialCodesPayloadFileRepository voterInitialCodesPayloadFileRepository;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {

		objectMapper = DomainObjectMapper.getNewInstance();

		final SetupPathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		voterInitialCodesPayloadFileRepository = new VoterInitialCodesPayloadFileRepository(objectMapper, pathResolver);

		final VoterInitialCodesPayloadFileRepository repository = new VoterInitialCodesPayloadFileRepository(objectMapper, pathResolver);

		repository.save(validVoterInitialCodesPayload(), EXISTING_VERIFICATION_CARD_SET_ID);
	}

	@SuppressWarnings("java:S117")
	private static VoterInitialCodesPayload validVoterInitialCodesPayload() {
		final ImmutableList<String> voterIdentifications = random.genUniqueDecimalStrings(8, NUMBER_OF_VOTER_INITIAL_CODES);
		final ImmutableList<String> UUIDs = ImmutableList.of(
				uuidGenerator.generate(),
				uuidGenerator.generate(),
				uuidGenerator.generate()
		);
		final ImmutableList<String> SVKs = ImmutableList.of(
				ElectionSetupUtils.genStartVotingKey(),
				ElectionSetupUtils.genStartVotingKey(),
				ElectionSetupUtils.genStartVotingKey());
		final ImmutableList<String> extendedAuthenticationFactor = random.genUniqueDecimalStrings(8, NUMBER_OF_VOTER_INITIAL_CODES);
		final ImmutableList<String> BCKs = random.genUniqueDecimalStrings(9, NUMBER_OF_VOTER_INITIAL_CODES);

		final ImmutableList<VoterInitialCodes> voterInitialCodes = IntStream.range(0, NUMBER_OF_VOTER_INITIAL_CODES)
				.mapToObj(i -> new VoterInitialCodes(
						voterIdentifications.get(i), UUIDs.get(i), UUIDs.get(i), SVKs.get(i), extendedAuthenticationFactor.get(i), BCKs.get(i))
				).collect(toImmutableList());

		return new VoterInitialCodesPayload(EXISTING_ELECTION_EVENT_ID, EXISTING_VERIFICATION_CARD_SET_ID, voterInitialCodes);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private VoterInitialCodesPayloadFileRepository voterInitialCodesPayloadFileRepositoryTemp;
		private VoterInitialCodesPayload voterInitialCodesPayload;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			final SetupPathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
			voterInitialCodesPayloadFileRepositoryTemp = new VoterInitialCodesPayloadFileRepository(objectMapper, pathResolver);
		}

		@BeforeEach
		void setUp() {
			voterInitialCodesPayload = validVoterInitialCodesPayload();
		}

		@Test
		@DisplayName("valid voter initial codes payload creates file")
		void save() {
			final Path savedPath = voterInitialCodesPayloadFileRepositoryTemp.save(voterInitialCodesPayload, EXISTING_VERIFICATION_CARD_SET_ID);

			assertTrue(Files.exists(savedPath));
		}

		@Test
		@DisplayName("null parameters throws NullPointerException")
		void saveNullVoterInitialCodes() {
			assertThrows(NullPointerException.class, () -> voterInitialCodesPayloadFileRepositoryTemp.save(null, EXISTING_VERIFICATION_CARD_SET_ID));
			assertThrows(NullPointerException.class, () -> voterInitialCodesPayloadFileRepositoryTemp.save(voterInitialCodesPayload, null));
		}
	}

	@Nested
	@DisplayName("calling findByElectionEventIdAndVerificationCardSetId")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class FindByIdTest {

		@Test
		@DisplayName("for existing voter initial codes payload returns it")
		void existingVoterInitialCodes() {
			assertTrue(voterInitialCodesPayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(EXISTING_ELECTION_EVENT_ID,
					EXISTING_VERIFICATION_CARD_SET_ID).isPresent());
		}

		@Test
		@DisplayName("for non existing voter initial codes payload return empty optional")
		void nonExistingVoterInitialCodes() {
			assertFalse(voterInitialCodesPayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(NON_EXISTING_ELECTION_EVENT_ID,
					EXISTING_VERIFICATION_CARD_SET_ID).isPresent());
		}

	}

}
