/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.mixdownload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.domain.generators.ControlComponentBallotBoxPayloadGenerator;
import ch.post.it.evoting.domain.generators.ControlComponentShufflePayloadGenerator;
import ch.post.it.evoting.domain.tally.BallotBoxStatus;
import ch.post.it.evoting.domain.tally.MixDecryptOnlinePayload;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineRawPayload;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentBallotBoxPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentShufflePayloadFileRepository;

import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

@ExtendWith(MockitoExtension.class)
@DisplayName("Use MixDecryptOnlineService to ")
class DownloadBallotBoxServiceTest extends TestGroupSetup {

	private static final int N = 2;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final String electionEventId = "426B2DE832AC4CF384AF0F68BB2B5D20";
	private final String ballotBoxId = "64EA41B3881E4BEF81A2CDDAB7597ECB";
	private final int l = RANDOM.nextInt(5) + 1;

	private final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
	private final BallotBoxService ballotBoxService = mock(BallotBoxService.class);
	private final ControlComponentShufflePayloadFileRepository controlComponentShufflePayloadFileRepository = mock(
			ControlComponentShufflePayloadFileRepository.class);
	private final ControlComponentBallotBoxPayloadFileRepository controlComponentBallotBoxPayloadFileRepository = mock(
			ControlComponentBallotBoxPayloadFileRepository.class);

	private final WebClientFactory webClientFactory = mock(WebClientFactory.class);
	private final RetryBackoffSpec retryBackoffSpecMock = mock(RetryBackoffSpec.class);
	private final DownloadBallotBoxService sut = new DownloadBallotBoxService(objectMapper, ballotBoxService, webClientFactory, retryBackoffSpecMock,
			controlComponentShufflePayloadFileRepository, controlComponentBallotBoxPayloadFileRepository);

	@Nested
	@DisplayName("Test downloadOnlineMixnetPayloads calls")
	class DownloadOnlineMixnetPayloads {

		private MixDecryptOnlinePayload mixDecryptOnlinePayload;

		@BeforeEach
		void createData() {
			final int numberOfSelections = l + 1;
			final ImmutableList<ControlComponentBallotBoxPayload> controlComponentBallotBoxPayloads = new ControlComponentBallotBoxPayloadGenerator(
					gqGroup).generate(electionEventId, ballotBoxId, numberOfSelections, l);
			final ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads = new ControlComponentShufflePayloadGenerator(
					gqGroup).generate(electionEventId, ballotBoxId, N, l);

			mixDecryptOnlinePayload = new MixDecryptOnlinePayload(electionEventId, ballotBoxId, controlComponentBallotBoxPayloads,
					controlComponentShufflePayloads);
		}

		@Test
		@DisplayName("downloadOnlineMixnetPayloads with null arguments throws a NullPointerException")
		void downloadOnlineMixnetPayloadsWithNullArgumentsThrows() {
			assertThrows(NullPointerException.class, () -> sut.download(null, ballotBoxId));
			assertThrows(NullPointerException.class, () -> sut.download(electionEventId, null));
		}

		@Test
		@DisplayName("ballotBoxService.hasStatus(ballotBoxId, BallotBoxStatus.MIXED) return false")
		void ballotBoxService_hasStatus_return_false() {
			when(ballotBoxService.hasStatus(any(), any())).thenReturn(false);

			final IllegalArgumentException uncheckedIOException =
					assertThrows(IllegalArgumentException.class, () -> sut.download(electionEventId, ballotBoxId));

			assertEquals("Ballot box is not mixed [ballotBoxId : %s]".formatted(ballotBoxId), uncheckedIOException.getMessage());
		}

		@Test
		@DisplayName("messageBrokerOrchestratorClient.downloadMixDecryptOnline raises IOException")
		void messageBrokerOrchestratorClient_downloadMixDecryptOnline_raises_IOException() {
			final IllegalArgumentException uncheckedIOException =
					assertThrows(IllegalArgumentException.class, () -> sut.download(electionEventId, ballotBoxId));

			assertEquals("Ballot box is not mixed [ballotBoxId : %s]".formatted(ballotBoxId), uncheckedIOException.getMessage());
		}

		@Test
		@DisplayName("happy path")
		@SuppressWarnings({ "unchecked", "rawtypes" })
		void downloadOnlineMixnetPayloadsTest() {

			when(ballotBoxService.hasStatus(any(), any())).thenReturn(true);
			when(ballotBoxService.updateStatus(any(), any())).thenReturn(BallotBoxStatus.DOWNLOADED);

			final WebClient webClient = mock(WebClient.class);
			final WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
			final WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
			final Mono<MixDecryptOnlineRawPayload> rawMono = (Mono<MixDecryptOnlineRawPayload>) mock(Mono.class);
			final Mono<MixDecryptOnlinePayload> mono = (Mono<MixDecryptOnlinePayload>) mock(Mono.class);
			when(webClientFactory.getWebClient(anyString())).thenReturn(webClient);
			when(webClient.get()).thenReturn(requestHeadersUriSpec);
			when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersUriSpec);
			when(requestHeadersUriSpec.accept(any())).thenReturn(requestHeadersUriSpec);
			when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
			when(responseSpec.bodyToMono(MixDecryptOnlineRawPayload.class)).thenReturn(rawMono);
			when(rawMono.retryWhen(any())).thenReturn(rawMono);
			when(rawMono.map(any(Function.class))).thenReturn(mono);
			when(mono.block()).thenReturn(mixDecryptOnlinePayload);

			when(controlComponentShufflePayloadFileRepository.savePayload(any())).thenReturn(Path.of("/controlComponentShufflePayloadFile"));

			assertNotNull(sut);

			sut.download(electionEventId, ballotBoxId);

			verify(ballotBoxService).hasStatus(any(), any());
			final int n_NODE_IDS = ControlComponentNode.ids().size();
			verify(controlComponentShufflePayloadFileRepository, times(n_NODE_IDS)).savePayload(any());
			verify(ballotBoxService).updateStatus(any(), any());
		}
	}
}
