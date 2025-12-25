/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.upload;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;
import ch.post.it.evoting.domain.generators.SetupComponentLVCCAllowListPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;
import ch.post.it.evoting.securedatamanager.online.process.OnlinePathResolver;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentLVCCAllowListPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentLVCCAllowListPayloadService;

import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

@DisplayName("A UploadLongVoteCastReturnCodesAllowListService")
@ExtendWith(MockitoExtension.class)
class UploadLongVoteCastReturnCodesAllowListServiceTest {

	private static String electionEventId;
	private static String verificationCardSetId;
	private static SetupComponentLVCCAllowListPayloadService setupComponentLVCCAllowListPayloadService;
	private final WebClientFactory webClientFactory = mock(WebClientFactory.class);
	private final RetryBackoffSpec retryBackoffSpecMock = mock(RetryBackoffSpec.class);

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {
		final OnlinePathResolver pathResolver = new OnlinePathResolver(tempDir, Path.of(""), Path.of(""));
		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		final SetupComponentLVCCAllowListPayloadFileRepository setupComponentLVCCAllowListPayloadFileRepository = new SetupComponentLVCCAllowListPayloadFileRepository(
				objectMapper, pathResolver);

		setupComponentLVCCAllowListPayloadService =
				new SetupComponentLVCCAllowListPayloadService(setupComponentLVCCAllowListPayloadFileRepository);

		final SetupComponentLVCCAllowListPayloadGenerator setupComponentLVCCAllowListPayloadGenerator = new SetupComponentLVCCAllowListPayloadGenerator();
		final SetupComponentLVCCAllowListPayload setupComponentLVCCAllowListPayload = setupComponentLVCCAllowListPayloadGenerator.generate();
		electionEventId = setupComponentLVCCAllowListPayload.getElectionEventId();
		verificationCardSetId = setupComponentLVCCAllowListPayload.getVerificationCardSetId();

		setupComponentLVCCAllowListPayloadService.save(setupComponentLVCCAllowListPayload);
	}

	@DisplayName("uploads successfully and does not throw.")
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void uploadHappyPath() {

		final WebClient webClient = mock(WebClient.class);
		final WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
		final WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
		final WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
		final Mono<ResponseEntity<Void>> mono = mock(Mono.class);
		when(webClientFactory.getWebClient(anyString())).thenReturn(webClient);
		when(webClient.post()).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.body(any(), any(Class.class))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.toBodilessEntity()).thenReturn(mono);
		when(mono.retryWhen(any())).thenReturn(mono);
		when(mono.block()).thenReturn(new ResponseEntity<>(HttpStatus.OK));

		final UploadLongVoteCastReturnCodesAllowListService uploadLongVoteCastReturnCodesAllowListService = new UploadLongVoteCastReturnCodesAllowListService(
				setupComponentLVCCAllowListPayloadService, webClientFactory, retryBackoffSpecMock);

		assertDoesNotThrow(() -> uploadLongVoteCastReturnCodesAllowListService.upload(electionEventId, verificationCardSetId));
	}

	@DisplayName("uploading throws upon unsuccessful response.")
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void uploadUnsuccessfulThrows() {

		final WebClient webClient = mock(WebClient.class);
		final WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
		final WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
		final WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
		final Mono<ResponseEntity<Void>> mono = mock(Mono.class);
		when(webClientFactory.getWebClient(anyString())).thenReturn(webClient);
		when(webClient.post()).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.body(any(), any(Class.class))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.toBodilessEntity()).thenReturn(mono);
		when(mono.retryWhen(any())).thenReturn(mono);
		when(mono.block()).thenThrow(IllegalStateException.class);

		final UploadLongVoteCastReturnCodesAllowListService uploadLongVoteCastReturnCodesAllowListService = new UploadLongVoteCastReturnCodesAllowListService(
				setupComponentLVCCAllowListPayloadService, webClientFactory, retryBackoffSpecMock);

		assertThrows(IllegalStateException.class,
				() -> uploadLongVoteCastReturnCodesAllowListService.upload(electionEventId, verificationCardSetId));
	}

	@DisplayName("provided with different inputs behaves as expected.")
	@Test
	void differentInputsThrowing() {

		final UploadLongVoteCastReturnCodesAllowListService uploadLongVoteCastReturnCodesAllowListService = new UploadLongVoteCastReturnCodesAllowListService(
				mock(SetupComponentLVCCAllowListPayloadService.class), webClientFactory, retryBackoffSpecMock);

		assertAll(
				() -> assertThrows(NullPointerException.class,
						() -> uploadLongVoteCastReturnCodesAllowListService.upload(null, verificationCardSetId)),
				() -> assertThrows(FailedValidationException.class,
						() -> uploadLongVoteCastReturnCodesAllowListService.upload("invalidElectionEventId", verificationCardSetId)),
				() -> assertThrows(NullPointerException.class,
						() -> uploadLongVoteCastReturnCodesAllowListService.upload(electionEventId, null)),
				() -> assertThrows(FailedValidationException.class,
						() -> uploadLongVoteCastReturnCodesAllowListService.upload(electionEventId, "invalidVerificationCardSetId")));
	}
}
