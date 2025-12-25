/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.mixdownload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.domain.tally.BallotBoxStatus;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;

import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

@ExtendWith(MockitoExtension.class)
@DisplayName("Use MixDecryptOnlineService to ")
class MixDecryptServiceTest extends TestGroupSetup {

	private final String electionEventId = "426B2DE832AC4CF384AF0F68BB2B5D20";
	private final String ballotBoxId = "64EA41B3881E4BEF81A2CDDAB7597ECB";

	private final BallotBoxService ballotBoxService = mock(BallotBoxService.class);
	private final WebClientFactory webClientFactory = mock(WebClientFactory.class);
	private final RetryBackoffSpec retryBackoffSpecMock = mock(RetryBackoffSpec.class);
	private final MixDecryptService sut = new MixDecryptService(ballotBoxService, webClientFactory, retryBackoffSpecMock);

	@Nested
	@DisplayName("Test startOnlineMixing calls")
	class StartOnlineMixing {

		@Test
		@DisplayName("startOnlineMixing with null arguments throws a NullPointerException")
		void startOnlineMixingWithNullArgumentsThrows() {
			assertThrows(NullPointerException.class, () -> sut.mix(null, ballotBoxId));
			assertThrows(NullPointerException.class, () -> sut.mix(electionEventId, null));
		}

		@Test
		@SuppressWarnings("unchecked")
		@DisplayName("startOnlineMixing response is unsuccessful")
		void startOnlineMixingTest_response_unsuccessful() {
			when(ballotBoxService.getFinishTime(ballotBoxId)).thenReturn(LocalDateTimeUtils.now().minusDays(1));

			final WebClient webClient = mock(WebClient.class);
			final WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
			final WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
			final Mono<ResponseEntity<Void>> mono = mock(Mono.class);
			when(webClientFactory.getWebClient(anyString())).thenReturn(webClient);
			when(webClient.put()).thenReturn(requestBodyUriSpec);
			when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodyUriSpec);
			when(requestBodyUriSpec.accept(any())).thenReturn(requestBodyUriSpec);
			when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
			when(responseSpec.toBodilessEntity()).thenReturn(mono);
			when(mono.retryWhen(any())).thenReturn(mono);
			when(mono.block()).thenThrow(IllegalStateException.class);

			assertThrows(IllegalStateException.class, () -> sut.mix(electionEventId, ballotBoxId));

			verify(ballotBoxService, times(0)).updateStatus(ballotBoxId, BallotBoxStatus.MIXING);
		}

		@Test
		@DisplayName("happyPath")
		@SuppressWarnings("unchecked")
		void startOnlineMixingTest_happyPath() {
			when(ballotBoxService.getFinishTime(ballotBoxId)).thenReturn(LocalDateTimeUtils.now().minusDays(1));

			final WebClient webClient = mock(WebClient.class);
			final WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
			final WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
			final Mono<ResponseEntity<Void>> mono = mock(Mono.class);
			when(webClientFactory.getWebClient(anyString())).thenReturn(webClient);
			when(webClient.put()).thenReturn(requestBodyUriSpec);
			when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodyUriSpec);
			when(requestBodyUriSpec.accept(any())).thenReturn(requestBodyUriSpec);
			when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
			when(responseSpec.toBodilessEntity()).thenReturn(mono);
			when(mono.retryWhen(any())).thenReturn(mono);
			when(mono.block()).thenReturn(new ResponseEntity<>(HttpStatus.OK));

			when(ballotBoxService.updateStatus(any(), any())).thenReturn(BallotBoxStatus.MIXING);

			sut.mix(electionEventId, ballotBoxId);

			verify(ballotBoxService, times(1)).updateStatus(ballotBoxId, BallotBoxStatus.MIXING);
		}
	}

	@Nested
	@DisplayName("Test startOnlineMixing calls")
	class GetOnlineStatus {

		@Test
		@DisplayName("getOnlineStatus with null arguments throws a NullPointerException")
		void getOnlineStatusWithNullArgumentsThrows() {
			assertThrows(NullPointerException.class, () -> sut.getMixingStatus(null, ballotBoxId));
			assertThrows(NullPointerException.class, () -> sut.getMixingStatus(electionEventId, null));
		}

		@Test
		@DisplayName("happy path")
		@SuppressWarnings({ "unchecked", "rawtypes" })
		void getOnlineStatus_happyPath() {

			final BallotBoxStatus ballotBoxStatus = BallotBoxStatus.MIXED;

			final WebClient webClient = mock(WebClient.class);
			final WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
			final WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
			final Mono<BallotBoxStatus> mono = mock(Mono.class);
			when(webClientFactory.getWebClient(anyString())).thenReturn(webClient);
			when(webClient.get()).thenReturn(requestHeadersUriSpec);
			when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersUriSpec);
			when(requestHeadersUriSpec.accept(any())).thenReturn(requestHeadersUriSpec);
			when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
			when(responseSpec.bodyToMono(BallotBoxStatus.class)).thenReturn(mono);
			when(mono.block()).thenReturn(ballotBoxStatus);

			assertEquals(ballotBoxStatus, sut.getMixingStatus(electionEventId, ballotBoxId));
		}
	}
}
