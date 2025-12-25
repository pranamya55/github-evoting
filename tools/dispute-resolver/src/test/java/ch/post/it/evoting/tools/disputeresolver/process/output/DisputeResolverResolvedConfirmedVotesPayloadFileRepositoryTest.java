/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.output;

import static ch.post.it.evoting.domain.Constants.DISPUTE_RESOLVER_RESOLVED_CONFIRMED_VOTES_PAYLOAD_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.generators.DisputeResolverResolvedConfirmedVotesPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.DisputeResolverResolvedConfirmedVotesPayload;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.tools.disputeresolver.process.PathService;

@DisplayName("DisputeResolverResolvedConfirmedVotesPayloadFileRepository calling save")
class DisputeResolverResolvedConfirmedVotesPayloadFileRepositoryTest {

	private static final ObjectMapper mapper = DomainObjectMapper.getNewInstance();

	private Path output;
	private final PathService pathService = mock(PathService.class);
	private DisputeResolverResolvedConfirmedVotesPayload disputeResolverResolvedConfirmedVotesPayload;
	private DisputeResolverResolvedConfirmedVotesPayloadFileRepository disputeResolverResolvedConfirmedVotesPayloadFileRepository;

	@BeforeEach
	void setUp(
			@TempDir
			final Path output) {
		this.output = output;
		disputeResolverResolvedConfirmedVotesPayload = new DisputeResolverResolvedConfirmedVotesPayloadGenerator().generate();
		disputeResolverResolvedConfirmedVotesPayloadFileRepository = new DisputeResolverResolvedConfirmedVotesPayloadFileRepository(mapper,
				pathService);

		when(pathService.getDisputeResolverResolvedConfirmedVotesPayloadPath())
				.thenReturn(this.output.resolve(DISPUTE_RESOLVER_RESOLVED_CONFIRMED_VOTES_PAYLOAD_NAME));
	}

	@Test
	@DisplayName("with a valid input behaves as expected.")
	void saveHappyPath() {
		disputeResolverResolvedConfirmedVotesPayloadFileRepository.save(disputeResolverResolvedConfirmedVotesPayload);

		assertTrue(Files.exists(output.resolve(DISPUTE_RESOLVER_RESOLVED_CONFIRMED_VOTES_PAYLOAD_NAME)));
	}

	@Test
	@DisplayName("with a null input throws a NullPointerException.")
	void saveThrowsWhenGivenNullInput() {
		assertThrows(NullPointerException.class, () -> disputeResolverResolvedConfirmedVotesPayloadFileRepository.save(null));
	}

	@Test
	@DisplayName("when the file already exists throws an UncheckedIOException.")
	void saveThrowsWhenFileAlreadyExists() throws IOException {

		Files.write(output.resolve(DISPUTE_RESOLVER_RESOLVED_CONFIRMED_VOTES_PAYLOAD_NAME), new byte[0]);

		final UncheckedIOException uncheckedIOException = assertThrows(UncheckedIOException.class,
				() -> disputeResolverResolvedConfirmedVotesPayloadFileRepository.save(disputeResolverResolvedConfirmedVotesPayload));

		assertEquals("Failed to write dispute resolver resolved confirmed votes payload to file system.", uncheckedIOException.getMessage());
	}

}
