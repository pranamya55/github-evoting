/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.input;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedElectionEventPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@DisplayName("ControlComponentExtractedElectionEventPayloadService calling loadAll")
class ControlComponentExtractedElectionEventPayloadServiceTest {

	private final SignatureKeystore<Alias> signatureKeystoreService = mock(SignatureKeystore.class);
	private final ControlComponentExtractedElectionEventPayloadFileRepository controlComponentExtractedElectionEventPayloadFileRepository =
			mock(ControlComponentExtractedElectionEventPayloadFileRepository.class);

	private ImmutableList<ControlComponentExtractedElectionEventPayload> controlComponentExtractedElectionEventPayloads;
	private ControlComponentExtractedElectionEventPayloadService controlComponentExtractedElectionEventPayloadService;

	@BeforeEach
	void setUp() throws SignatureException {
		controlComponentExtractedElectionEventPayloads = new ControlComponentExtractedElectionEventPayloadGenerator().generate();
		controlComponentExtractedElectionEventPayloadService = new ControlComponentExtractedElectionEventPayloadService(signatureKeystoreService,
				controlComponentExtractedElectionEventPayloadFileRepository);

		// Mock the repository to return the generated payloads.
		when(controlComponentExtractedElectionEventPayloadFileRepository.findAll()).thenReturn(controlComponentExtractedElectionEventPayloads);

		// Mock the signature verification to return true for all payloads.
		when(signatureKeystoreService.verifySignature(any(), any(), any(), any())).thenReturn(true);
	}

	@Test
	@DisplayName("behaves as expected.")
	void loadAllHappyPath() {

		final ImmutableList<ControlComponentExtractedElectionEventPayload> retrievedControlComponentExtractedElectionEventPayloads = assertDoesNotThrow(
				() -> controlComponentExtractedElectionEventPayloadService.loadAll());

		assertTrue(
				controlComponentExtractedElectionEventPayloads.stream().allMatch(retrievedControlComponentExtractedElectionEventPayloads::contains));
		assertTrue(
				retrievedControlComponentExtractedElectionEventPayloads.stream().allMatch(controlComponentExtractedElectionEventPayloads::contains));
		assertEquals(retrievedControlComponentExtractedElectionEventPayloads.size(), controlComponentExtractedElectionEventPayloads.size());
	}

	@Test
	@DisplayName("throws an exception when the signature verification fails.")
	void loadAllThrowsWhenSignatureVerificationFails() throws SignatureException {

		// Mock the signature verification to return false.
		when(signatureKeystoreService.verifySignature(any(), any(), any(), any())).thenReturn(false);

		final InvalidPayloadSignatureException invalidPayloadSignatureException = assertThrows(InvalidPayloadSignatureException.class,
				() -> controlComponentExtractedElectionEventPayloadService.loadAll());

		assertTrue(invalidPayloadSignatureException.getMessage()
				.startsWith("Signature of payload ControlComponentExtractedElectionEventPayload is invalid."));
	}

	@Test
	@DisplayName("throws an exception when the signature verification throws an exception.")
	void loadAllThrowsWhenSignatureVerificationThrows() throws SignatureException {

		// Mock the signature verification to return false.
		when(signatureKeystoreService.verifySignature(any(), any(), any(), any())).thenThrow(new SignatureException());

		final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
				() -> controlComponentExtractedElectionEventPayloadService.loadAll());

		assertTrue(
				illegalStateException.getMessage().startsWith("Cannot verify the signature of control component extracted election event payload."));
	}

}
