/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process;

import static ch.post.it.evoting.domain.Constants.DISPUTE_RESOLVER_RESOLVED_CONFIRMED_VOTES_PAYLOAD_NAME;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.tally.disputeresolver.DisputeResolverResolvedConfirmedVotesPayload;
import ch.post.it.evoting.tools.disputeresolver.DisputeResolverApplication;

@SpringBootTest
@ActiveProfiles("test")
@MockitoBean(types = { DisputeResolverApplication.class }) // Mocked application to prevent the service from starting automatically.
@DisplayName("DisputeResolverService")
class DisputeResolverServiceIT {

	@TempDir
	private static Path tempDir;

	@Autowired
	private DisputeResolverService disputeResolverService;

	@Autowired
	private ObjectMapper objectMapper;

	@DynamicPropertySource
	private static void setup(final DynamicPropertyRegistry registry) {
		// Dynamically configures the output directory for the dispute resolver service.
		registry.add("output.directory", () -> tempDir.toString());
	}

	@Test
	@DisplayName("runs as expected and produces the expected payload.")
	void runHappyPath() throws URISyntaxException {

		assertDoesNotThrow(() -> disputeResolverService.run());

		final Path generatedPayloadPath = tempDir.resolve(DISPUTE_RESOLVER_RESOLVED_CONFIRMED_VOTES_PAYLOAD_NAME);
		final Path expectedPayloadPath = Path.of(DisputeResolverServiceIT.class.getClassLoader()
				.getResource(DISPUTE_RESOLVER_RESOLVED_CONFIRMED_VOTES_PAYLOAD_NAME).toURI());

		final DisputeResolverResolvedConfirmedVotesPayload generatedPayload = loadDisputeResolverResolvedConfirmedVotesPayload(generatedPayloadPath);
		final DisputeResolverResolvedConfirmedVotesPayload expectedPayload = loadDisputeResolverResolvedConfirmedVotesPayload(expectedPayloadPath);

		assertTrue(Objects.nonNull(generatedPayload.getSignature()), "The signature of the generated payload must not be null.");

		// As the signature has a random component, we set it to the expected value for comparison.
		generatedPayload.setSignature(expectedPayload.getSignature());

		assertEquals(expectedPayload, generatedPayload, "The generated payload must match the expected payload.");
	}

	private DisputeResolverResolvedConfirmedVotesPayload loadDisputeResolverResolvedConfirmedVotesPayload(
			final Path disputeResolverResolvedConfirmedVotesPayloadPath) {
		try {
			return objectMapper.readValue(disputeResolverResolvedConfirmedVotesPayloadPath.toFile(),
					DisputeResolverResolvedConfirmedVotesPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Unable to deserialize dispute resolver resolved confirmed votes payload. [path: %s]",
					disputeResolverResolvedConfirmedVotesPayloadPath), e);
		}
	}

}
