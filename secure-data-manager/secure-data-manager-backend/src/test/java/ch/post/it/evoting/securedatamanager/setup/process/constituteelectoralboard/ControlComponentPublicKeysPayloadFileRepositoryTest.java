/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.constituteelectoralboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;

@DisplayName("ControlComponentPublicKeysPayloadFileRepository")
class ControlComponentPublicKeysPayloadFileRepositoryTest {
	private static final String ELECTION_EVENT_ID = "314BD34DCF6E4DE4B771A92FA3849D3D";
	private static final String WRONG_ELECTION_EVENT_ID = "414BD34DCF6E4DE4B771A92FA3849D3D";
	private static final String CORRUPTED_ELECTION_EVENT_ID = "514BD34DCF6E4DE4B771A92FA3849D3D";
	private static ControlComponentPublicKeysPayloadFileRepository controlComponentPublicKeysPayloadFileRepository;

	@BeforeAll
	static void setUpAll() throws URISyntaxException, IOException {
		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();

		final Path path = Paths.get(
				Objects.requireNonNull(ControlComponentPublicKeysPayloadFileRepositoryTest.class.getResource("/controlComponentPublicKeysTest/"))
						.toURI());
		final Path emptyPath = Path.of("");
		final SetupPathResolver pathResolver = new SetupPathResolver(path, emptyPath, emptyPath, emptyPath, emptyPath);
		controlComponentPublicKeysPayloadFileRepository = new ControlComponentPublicKeysPayloadFileRepository(objectMapper, pathResolver);
	}

	@Nested
	@DisplayName("calling findAllOrderByNodeId")
	class FindAllTest {

		@Test
		@DisplayName("returns all payloads")
		void allPayloads() {
			final ImmutableList<ControlComponentPublicKeysPayload> publicKeys = controlComponentPublicKeysPayloadFileRepository.findAllOrderByNodeId(
					ELECTION_EVENT_ID);

			assertEquals(publicKeys.size(), ControlComponentNode.ids().size());
		}

		@Test
		@DisplayName("for non existing election event returns empty list")
		void nonExistingElectionEvent() {
			final ImmutableList<ControlComponentPublicKeysPayload> payloads = controlComponentPublicKeysPayloadFileRepository.findAllOrderByNodeId(
					WRONG_ELECTION_EVENT_ID);

			assertEquals(ImmutableList.emptyList(), payloads);
		}

		@Test
		@DisplayName("with one corrupted payload throws UncheckedIOException")
		void corruptedPayload() {
			final UncheckedIOException exception = assertThrows(UncheckedIOException.class,
					() -> controlComponentPublicKeysPayloadFileRepository.findAllOrderByNodeId(CORRUPTED_ELECTION_EVENT_ID));

			assertEquals("Failed to deserialize encryption group.", exception.getMessage());
		}

	}

}
