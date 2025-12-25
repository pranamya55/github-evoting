/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.compute;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.generators.SetupComponentVerificationDataPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationDataPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

@ExtendWith(MockitoExtension.class)
class ComputeVerificationCardSetServiceTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	@Spy
	// Injected in the service.
	private ExecutorService executorService = Executors.newFixedThreadPool(10);

	@Mock
	private VerificationCardSetService verificationCardSetServiceMock;

	@Mock
	private ComputeEncryptedLongReturnCodeSharesService computeEncryptedLongReturnCodeSharesServiceMock;

	@Mock
	private SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepositoryMock;

	@Mock
	private SignatureKeystore<Alias> signatureKeystoreService;

	@Mock
	private WebClientFactory webClientFactoryMock;

	@Mock
	private RetryBackoffSpec retryBackoffSpecMock;

	private ComputeVerificationCardSetService computeVerificationCardSetService;
	private String electionEventId;

	@BeforeEach
	void beforeEach() {
		electionEventId = uuidGenerator.generate();

		computeVerificationCardSetService = new ComputeVerificationCardSetService(26214400, webClientFactoryMock, retryBackoffSpecMock,
				verificationCardSetServiceMock, computeEncryptedLongReturnCodeSharesServiceMock,
				setupComponentVerificationDataPayloadFileRepositoryMock);
	}

	@Test
	@DisplayName("compute verification card sets with invalid election event id throws FailedValidationException")
	void computeInvalidParamsThrows() {
		assertThrows(FailedValidationException.class, () -> computeVerificationCardSetService.compute("", ""));
	}

	@Test
	@DisplayName("compute verification card sets with valid input does not throw")
	void computeVerificationCardSetsHappyPath() {

		when(setupComponentVerificationDataPayloadFileRepositoryMock.getCount(eq(electionEventId), anyString())).thenReturn(1);
		configureWebClientFactoryMock();

		final SetupComponentVerificationDataPayloadGenerator setupComponentVerificationDataPayloadGenerator = new SetupComponentVerificationDataPayloadGenerator();
		when(setupComponentVerificationDataPayloadFileRepositoryMock.retrieve(eq(electionEventId), anyString(), anyInt()))
				.then(p -> setupComponentVerificationDataPayloadGenerator.generate(electionEventId, p.getArgument(1), 4));

		assertDoesNotThrow(
				() -> computeVerificationCardSetService.compute(electionEventId, uuidGenerator.generate()));
		verify(computeEncryptedLongReturnCodeSharesServiceMock, times(1)).compute(eq(electionEventId), anyString(), any());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void configureWebClientFactoryMock() {
		final WebClient webClient = mock(WebClient.class);
		final WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
		final WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
		final Flux<Integer> flux = mock(Flux.class);
		final Mono<ImmutableList<Integer>> mono = mock(Mono.class);
		final ImmutableList<Integer> broadcastChunkIds = ImmutableList.emptyList();

		when(webClientFactoryMock.getWebClient(anyString())).thenReturn(webClient);
		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.accept(any())).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToFlux(Integer.class)).thenReturn(flux);
		when(flux.collect(any(Collector.class))).thenReturn(mono);
		when(mono.retryWhen(any())).thenReturn(mono);
		when(mono.block()).thenReturn(broadcastChunkIds);
	}
}
