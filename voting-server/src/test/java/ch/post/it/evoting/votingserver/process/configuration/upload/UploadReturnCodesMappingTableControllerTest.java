/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.security.SignatureException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.internal.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.configuration.SetupComponentCMTablePayload;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceContext;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceService;
import ch.post.it.evoting.votingserver.idempotence.IdempotentExecution;
import ch.post.it.evoting.votingserver.idempotence.IdempotentExecutionRepository;
import ch.post.it.evoting.votingserver.process.ReturnCodesMappingTableService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@DisplayName("UploadReturnCodesMappingTableController")
class UploadReturnCodesMappingTableControllerTest {

	private static SetupComponentCMTablePayload setupComponentCMTablePayload;
	private static String electionEventId;
	private static String verificationCardSetId;
	private static SignatureKeystore<Alias> signatureKeystoreService;
	private static IdempotentExecutionRepository idempotentExecutionRepository;
	private static ReturnCodesMappingTableService returnCodesMappingTableService;
	private static UploadReturnCodesMappingTableController uploadReturnCodesMappingTableController;

	@BeforeAll
	static void setUpAll() throws IOException {
		signatureKeystoreService = mock(SignatureKeystore.class);
		idempotentExecutionRepository = mock(IdempotentExecutionRepository.class);
		final ObjectMapper mapper = DomainObjectMapper.getNewInstance();
		final IdempotenceService<IdempotenceContext> idempotenceService = new IdempotenceService<>(HashService.getInstance(),
				idempotentExecutionRepository
		);
		returnCodesMappingTableService = mock(ReturnCodesMappingTableService.class);
		uploadReturnCodesMappingTableController = new UploadReturnCodesMappingTableController(
				signatureKeystoreService, returnCodesMappingTableService, idempotenceService);

		final InputStream returnCodesMappingTablePayloadInputStream = UploadReturnCodesMappingTableControllerTest.class.getResourceAsStream(
				"/process/returnCodesMappingTableResourceTest/setupComponentCMTablePayload.0.json");
		setupComponentCMTablePayload = mapper.readValue(returnCodesMappingTablePayloadInputStream, SetupComponentCMTablePayload.class);
		electionEventId = setupComponentCMTablePayload.getElectionEventId();
		verificationCardSetId = setupComponentCMTablePayload.getVerificationCardSetId();
	}

	@Test
	@DisplayName("calling save with valid parameters")
	void saveReturnCodesMappingTableHappyPath() throws SignatureException {
		when(signatureKeystoreService.verifySignature(any(), any(), any(), any())).thenReturn(true);
		when(idempotentExecutionRepository.existsById(any())).thenReturn(false);
		uploadReturnCodesMappingTableController.upload(electionEventId, verificationCardSetId, Flux.just(setupComponentCMTablePayload)).block();

		verify(returnCodesMappingTableService, times(1)).save(any());
	}

	@Test
	@DisplayName("calling save with valid parameters but idempotent service say it was already executed")
	void saveReturnCodesMappingTableIdempotentAlreadyExist() throws SignatureException {
		when(signatureKeystoreService.verifySignature(any(), any(), any(), any())).thenReturn(true);
		when(idempotentExecutionRepository.existsById(any())).thenReturn(true);
		final Hash hash = HashFactory.createHash();
		final ImmutableByteArray payloadBytes = hash.recursiveHash(setupComponentCMTablePayload);
		final Optional<IdempotentExecution> execution = Optional.of(
				new IdempotentExecution(IdempotenceContext.SAVE_RETURN_CODES_MAPPING_TABLE.name(),
						String.format("%s-%s-%s", electionEventId, verificationCardSetId, setupComponentCMTablePayload.getChunkId()), payloadBytes));
		when(idempotentExecutionRepository.findById(any())).thenReturn(execution);
		uploadReturnCodesMappingTableController.upload(electionEventId, verificationCardSetId, Flux.just(setupComponentCMTablePayload)).block();

		verify(returnCodesMappingTableService, times(0)).save(any());
		verify(idempotentExecutionRepository, times(1)).existsById(any());
	}

	@Test
	@DisplayName("calling save with invalid signature")
	void saveReturnCodesMappingTableWithInvalidSignature() throws SignatureException {
		when(signatureKeystoreService.verifySignature(any(), any(), any(), any())).thenReturn(false);

		final Mono<Void> saveReturnCodesMappingTableMono = uploadReturnCodesMappingTableController.upload(electionEventId, verificationCardSetId,
				Flux.just(setupComponentCMTablePayload));
		final InvalidPayloadSignatureException exception = assertThrows(InvalidPayloadSignatureException.class,
				saveReturnCodesMappingTableMono::block);

		final String errorMessage = String.format("Signature of payload %s is invalid. [electionEventId: %s, verificationCardSetId: %s]",
				SetupComponentCMTablePayload.class.getSimpleName(), setupComponentCMTablePayload.getElectionEventId(),
				setupComponentCMTablePayload.getVerificationCardSetId());

		assertEquals(errorMessage, exception.getMessage());
	}

	@Test
	@DisplayName("calling save with invalid signature's content")
	void saveReturnCodesMappingTableWithInvalidSignatureContent() throws SignatureException {
		when(signatureKeystoreService.verifySignature(any(), any(), any(), any())).thenThrow(SignatureException.class);

		final Mono<Void> saveReturnCodesMappingTableMono = uploadReturnCodesMappingTableController.upload(electionEventId, verificationCardSetId,
				Flux.just(setupComponentCMTablePayload));
		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				saveReturnCodesMappingTableMono::block);

		final String errorMessage = String.format(
				"Couldn't verify the signature of the setup component CMTable payload. [electionEventId: %s, verificationCardSetId: %s]",
				electionEventId, verificationCardSetId);

		assertEquals(errorMessage, exception.getMessage());
	}
}
