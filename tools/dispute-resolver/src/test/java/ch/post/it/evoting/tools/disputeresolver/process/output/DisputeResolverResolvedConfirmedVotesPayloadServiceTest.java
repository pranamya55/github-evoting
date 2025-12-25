/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.output;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.generators.DisputeResolverResolvedConfirmedVotesPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.DisputeResolverResolvedConfirmedVotesPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@DisplayName("DisputeResolverResolvedConfirmedVotesPayloadService calling save with")
class DisputeResolverResolvedConfirmedVotesPayloadServiceTest {
	private final SignatureKeystore<Alias> signatureKeystoreService = mock(SignatureKeystore.class);
	private final DisputeResolverResolvedConfirmedVotesPayloadFileRepository disputeResolverResolvedConfirmedVotesPayloadFileRepository =
			mock(DisputeResolverResolvedConfirmedVotesPayloadFileRepository.class);

	private DisputeResolverResolvedConfirmedVotesPayload disputeResolverResolvedConfirmedVotesPayload;
	private DisputeResolverResolvedConfirmedVotesPayloadService disputeResolverResolvedConfirmedVotesPayloadService;

	@BeforeEach
	void setUp() throws SignatureException {
		disputeResolverResolvedConfirmedVotesPayload = new DisputeResolverResolvedConfirmedVotesPayloadGenerator().generate();

		disputeResolverResolvedConfirmedVotesPayloadService = new DisputeResolverResolvedConfirmedVotesPayloadService(signatureKeystoreService,
				disputeResolverResolvedConfirmedVotesPayloadFileRepository);

		// Mock the signature generation to return a fixed signature for testing purposes.
		when(signatureKeystoreService.generateSignature(any(), any()))
				.thenReturn(new ImmutableByteArray("signature".getBytes(StandardCharsets.UTF_8)));

		// Mock the save method to do nothing, as we are testing the service logic, not the repository.
		doNothing().when(disputeResolverResolvedConfirmedVotesPayloadFileRepository).save(any(DisputeResolverResolvedConfirmedVotesPayload.class));
	}

	@Test
	@DisplayName("a valid input behaves as expected.")
	void saveHappyPath() {
		assertDoesNotThrow(() -> disputeResolverResolvedConfirmedVotesPayloadService.save(disputeResolverResolvedConfirmedVotesPayload));

		verify(disputeResolverResolvedConfirmedVotesPayloadFileRepository, times(1)).save(disputeResolverResolvedConfirmedVotesPayload);
	}

	@Test
	@DisplayName("a null input throws a NullPointerException.")
	void saveThrowsWhenGivenNullInput() {
		assertThrows(NullPointerException.class, () -> disputeResolverResolvedConfirmedVotesPayloadService.save(null));
	}

	@Test
	@DisplayName("calling save throws an IllegalStateException when signing of the dispute resolver resolved confirmed votes payload fails.")
	void saveThrowsWhenSigningFails() throws SignatureException {
		when(signatureKeystoreService.generateSignature(any(), any())).thenThrow(new SignatureException("Signing failed"));

		final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
				() -> disputeResolverResolvedConfirmedVotesPayloadService.save(disputeResolverResolvedConfirmedVotesPayload));

		assertEquals("Failed to generate dispute resolver resolved confirmed votes payload signature.", illegalStateException.getMessage());
	}
}
