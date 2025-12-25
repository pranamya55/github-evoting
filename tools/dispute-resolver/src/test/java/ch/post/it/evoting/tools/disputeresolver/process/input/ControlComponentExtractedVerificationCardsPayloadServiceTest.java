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
import ch.post.it.evoting.domain.generators.ControlComponentExtractedVerificationCardsPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@DisplayName("ControlComponentExtractedVerificationCardsPayloadService calling loadAll")
class ControlComponentExtractedVerificationCardsPayloadServiceTest {

	private final SignatureKeystore<Alias> signatureKeystoreService = mock(SignatureKeystore.class);
	private final ControlComponentExtractedVerificationCardsPayloadFileRepository controlComponentExtractedVerificationCardsPayloadFileRepository =
			mock(ControlComponentExtractedVerificationCardsPayloadFileRepository.class);

	private ImmutableList<ControlComponentExtractedVerificationCardsPayload> controlComponentExtractedVerificationCardsPayloads;
	private ControlComponentExtractedVerificationCardsPayloadService controlComponentExtractedVerificationCardsPayloadService;

	@BeforeEach
	void setUp() throws SignatureException {
		controlComponentExtractedVerificationCardsPayloads = new ControlComponentExtractedVerificationCardsPayloadGenerator().generate();
		controlComponentExtractedVerificationCardsPayloadService = new ControlComponentExtractedVerificationCardsPayloadService(
				signatureKeystoreService, controlComponentExtractedVerificationCardsPayloadFileRepository);

		// Mock the repository to return the generated payloads.
		when(controlComponentExtractedVerificationCardsPayloadFileRepository.findAll()).thenReturn(controlComponentExtractedVerificationCardsPayloads);

		// Mock the signature verification to return true for all payloads.
		when(signatureKeystoreService.verifySignature(any(), any(), any(), any())).thenReturn(true);
	}

	@Test
	@DisplayName("behaves as expected.")
	void loadAllHappyPath() {

		final ImmutableList<ControlComponentExtractedVerificationCardsPayload> retrievedControlComponentExtractedVerificationCardsPayloads = assertDoesNotThrow(
				() -> controlComponentExtractedVerificationCardsPayloadService.loadAll());

		assertTrue(
				controlComponentExtractedVerificationCardsPayloads.stream()
						.allMatch(retrievedControlComponentExtractedVerificationCardsPayloads::contains));
		assertTrue(
				retrievedControlComponentExtractedVerificationCardsPayloads.stream()
						.allMatch(controlComponentExtractedVerificationCardsPayloads::contains));
		assertEquals(retrievedControlComponentExtractedVerificationCardsPayloads.size(), controlComponentExtractedVerificationCardsPayloads.size());
	}

	@Test
	@DisplayName("throws an exception when the signature verification fails.")
	void loadAllThrowsWhenSignatureVerificationFails() throws SignatureException {

		// Mock the signature verification to return false.
		when(signatureKeystoreService.verifySignature(any(), any(), any(), any())).thenReturn(false);

		final InvalidPayloadSignatureException invalidPayloadSignatureException = assertThrows(InvalidPayloadSignatureException.class,
				() -> controlComponentExtractedVerificationCardsPayloadService.loadAll());

		assertTrue(invalidPayloadSignatureException.getMessage()
				.startsWith("Signature of payload ControlComponentExtractedVerificationCardsPayload is invalid."));
	}

	@Test
	@DisplayName("throws an exception when the signature verification throws an exception.")
	void loadAllThrowsWhenSignatureVerificationThrows() throws SignatureException {

		// Mock the signature verification to return false.
		when(signatureKeystoreService.verifySignature(any(), any(), any(), any())).thenThrow(new SignatureException());

		final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
				() -> controlComponentExtractedVerificationCardsPayloadService.loadAll());

		assertTrue(illegalStateException.getMessage()
				.startsWith("Cannot verify the signature of control component extracted verification cards payload."));
	}

}
