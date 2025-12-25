/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.requestcckeys;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.KEY_LENGTH;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.online.process.OnlinePathResolver;

@DisplayName("ControlComponentPublicKeysPayloadFileRepository")
class ControlComponentPublicKeysPayloadFileRepositoryTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String NON_EXISTING_ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String ELECTION_EVENT_ID = "314BD34DCF6E4DE4B771A92FA3849D3D";
	private static final int NODE_ID = 1;
	private static final int NON_EXISTING_NODE_ID = 5;

	private static GqGroup gqGroup;
	private static ObjectMapper objectMapper;
	private static ElGamalGenerator elGamalGenerator;
	private static ControlComponentPublicKeysPayloadFileRepository controlComponentPublicKeysPayloadFileRepository;

	@BeforeAll
	static void setUpAll() throws URISyntaxException, IOException {
		gqGroup = SerializationUtils.getGqGroup();
		elGamalGenerator = new ElGamalGenerator(gqGroup);
		objectMapper = DomainObjectMapper.getNewInstance();

		final Path path = Paths.get(
				Objects.requireNonNull(ControlComponentPublicKeysPayloadFileRepositoryTest.class.getResource("/controlComponentPublicKeysTest/"))
						.toURI());
		final OnlinePathResolver pathResolver = new OnlinePathResolver(path, Path.of(""), Path.of(""));
		controlComponentPublicKeysPayloadFileRepository = new ControlComponentPublicKeysPayloadFileRepository(objectMapper, pathResolver);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private ControlComponentPublicKeysPayloadFileRepository controlComponentPublicKeysPayloadFileRepositoryTemp;

		private ControlComponentPublicKeysPayload controlComponentPublicKeysPayload;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {
			final OnlinePathResolver pathResolver = new OnlinePathResolver(tempDir, Path.of(""), Path.of(""));
			controlComponentPublicKeysPayloadFileRepositoryTemp = new ControlComponentPublicKeysPayloadFileRepository(objectMapper, pathResolver);
		}

		@BeforeEach
		void setUp() {
			// Create keys.
			final ElGamalMultiRecipientPublicKey ccrChoiceReturnCodesEncryptionPublicKey = elGamalGenerator.genRandomPublicKey(KEY_LENGTH);
			final ElGamalMultiRecipientPublicKey ccmElectionPublicKey = elGamalGenerator.genRandomPublicKey(KEY_LENGTH);
			final GroupVector<SchnorrProof, ZqGroup> schnorrProofs = SerializationUtils.getSchnorrProofs(KEY_LENGTH);
			final ControlComponentPublicKeys controlComponentPublicKeys = new ControlComponentPublicKeys(NODE_ID,
					ccrChoiceReturnCodesEncryptionPublicKey, schnorrProofs, ccmElectionPublicKey, schnorrProofs);

			// Create payload.
			controlComponentPublicKeysPayload = new ControlComponentPublicKeysPayload(gqGroup, ELECTION_EVENT_ID, controlComponentPublicKeys);
		}

		@Test
		@DisplayName("valid payload creates file")
		void save() {
			final Path savedPath = controlComponentPublicKeysPayloadFileRepositoryTemp.save(controlComponentPublicKeysPayload);

			assertTrue(Files.exists(savedPath));
		}

		@Test
		@DisplayName("null payload throws NullPointerException")
		void saveNullPayload() {
			assertThrows(NullPointerException.class, () -> controlComponentPublicKeysPayloadFileRepositoryTemp.save(null));
		}

	}

	@Nested
	@DisplayName("calling existsById")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ExistsByIdTest {

		@Test
		@DisplayName("for existing payload returns true")
		void existingPayload() {
			assertTrue(controlComponentPublicKeysPayloadFileRepository.existsById(ELECTION_EVENT_ID, 1));
		}

		@Test
		@DisplayName("with invalid election event id throws FailedValidationException")
		void invalidElectionEventId() {
			assertThrows(FailedValidationException.class, () -> controlComponentPublicKeysPayloadFileRepository.existsById("invalidId", 1));
		}

		@Test
		@DisplayName("with invalid node id throws IllegalArgumentException")
		void invalidNodeId() {
			assertThrows(IllegalArgumentException.class,
					() -> controlComponentPublicKeysPayloadFileRepository.existsById(ELECTION_EVENT_ID, NON_EXISTING_NODE_ID));
		}

		@Test
		@DisplayName("for non existing payload returns false")
		void nonExistingPayload() {
			assertFalse(controlComponentPublicKeysPayloadFileRepository.existsById(NON_EXISTING_ELECTION_EVENT_ID, NODE_ID));
		}

	}

}
