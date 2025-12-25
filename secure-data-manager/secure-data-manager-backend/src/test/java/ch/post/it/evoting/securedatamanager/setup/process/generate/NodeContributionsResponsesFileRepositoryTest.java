/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;
import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentCodeSharesPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

@DisplayName("NodeContributionsResponsesFileRepository")
class NodeContributionsResponsesFileRepositoryTest {

	private static final String WRONG_ID = "0123456789ABCDEF0123456789ABCDEF";
	private static final String ELECTION_EVENT_ID = "7E6225DF3A10F4A5D63D76EA7E0E7916";
	private static final String VERIFICATION_CARD_SET_ID = "5B78F34995FAE5EA69DBD3A7608F5397";

	private static final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();

	private static ControlComponentCodeSharesPayloadFileRepository nodeContributionsResponsesRepository;

	@BeforeAll
	static void setUpAll() throws URISyntaxException, IOException {

		final Path path = Paths.get(
				ControlComponentCodeSharesPayloadFileRepository.class.getResource("/nodeContributionsResponsesFileRepositoryTest/valid").toURI());
		final PathResolver pathResolver = new SetupPathResolver(path, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		nodeContributionsResponsesRepository = new ControlComponentCodeSharesPayloadFileRepository(objectMapper, pathResolver);
	}

	@Test
	@DisplayName("Find all")
	void findAll() {
		final ImmutableList<Path> nodeContributionsPath = nodeContributionsResponsesRepository.findAllPathsOrderedByChunkId(
				ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID);

		assertEquals(1, nodeContributionsPath.size());
	}

	@Test
	@DisplayName("Find all with invalid ids throws")
	void findAllWithInvalidIds() {
		assertAll(
				() -> assertThrows(FailedValidationException.class,
						() -> nodeContributionsResponsesRepository.findAllPathsOrderedByChunkId("invalidId", VERIFICATION_CARD_SET_ID)),
				() -> assertThrows(FailedValidationException.class,
						() -> nodeContributionsResponsesRepository.findAllPathsOrderedByChunkId(ELECTION_EVENT_ID, "invalidId"))
		);
	}

	@Test
	@DisplayName("Find all with wrong path return empty list")
	void findAllWithWrongPath() {
		assertAll(
				() -> assertEquals(ImmutableList.emptyList(),
						nodeContributionsResponsesRepository.findAllPathsOrderedByChunkId(WRONG_ID, VERIFICATION_CARD_SET_ID)),
				() -> assertEquals(ImmutableList.emptyList(),
						nodeContributionsResponsesRepository.findAllPathsOrderedByChunkId(ELECTION_EVENT_ID, WRONG_ID))
		);
	}

	@Test
	@DisplayName("get payload with invalid node contribution throws")
	void getPayloadsWithInvalidNodeContributions() throws URISyntaxException, IOException {
		final Path path = Paths.get(
				ControlComponentCodeSharesPayloadFileRepository.class.getResource("/nodeContributionsResponsesFileRepositoryTest/invalid").toURI());
		final PathResolver pathResolver = new SetupPathResolver(path, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		final ControlComponentCodeSharesPayloadFileRepository repository = new ControlComponentCodeSharesPayloadFileRepository(objectMapper,
				pathResolver);

		final Path verificationCardSetPath = pathResolver.resolveVerificationCardSetPath(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID);
		final Path controlComponentCodeSharesPayloadPath = verificationCardSetPath.resolve("controlComponentCodeSharesPayload.0.json");
		final UncheckedIOException exception = assertThrows(UncheckedIOException.class,
				() -> repository.load(controlComponentCodeSharesPayloadPath));

		assertTrue(exception.getMessage().startsWith("Failed to deserialize the ControlComponentCodeShares payloads."));
	}

}
