/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.requestcckeys;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStepRunner;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

@DisplayName("A RequestCcKeysService")
@ExtendWith(MockitoExtension.class)
class RequestCcKeysServiceTest {
	private static GqGroup gqGroup;
	private static String electionEventId;
	private static ElectionEventContextPayload electionEventContextPayload;

	@Mock
	private WorkflowStepRunner workflowStepRunner;
	@Mock
	private WebClientFactory webClientFactory;
	@Mock
	private RetryBackoffSpec retryBackoffSpecMock;
	@Mock
	private ControlComponentPublicKeysService controlComponentPublicKeysService;
	@Mock
	private ElectionEventContextPayloadService electionEventContextPayloadService;

	private RequestCcKeysService requestCcKeysService;

	@BeforeEach
	void setUp() {
		requestCcKeysService = new RequestCcKeysService(webClientFactory, retryBackoffSpecMock, workflowStepRunner, controlComponentPublicKeysService,
				electionEventContextPayloadService);
	}

	@BeforeAll
	static void setUpAll() {
		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		gqGroup = electionEventContextPayload.getEncryptionGroup();
		electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();
	}

	@DisplayName("executing requestCcKeys(), with an invalid number of keys retrieved, throws an IllegalStateException.")
	@Test
	@SuppressWarnings("unchecked")
	void requestCCKeysInvalidTotalExpectedKeys() {

		when(electionEventContextPayloadService.load(anyString())).thenReturn(electionEventContextPayload);

		final ImmutableList<ControlComponentPublicKeysPayload> controlComponentPublicKeysPayloads = ControlComponentNode.ids().stream()
				.skip(1) // skips the first element to have an invalid number of returned payloads.
				.map(this::createControlComponentPayload)
				.collect(toImmutableList());

		final WebClient webClient = mock(WebClient.class);
		final WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
		final WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
		final Flux<ControlComponentPublicKeysPayload> flux = mock(Flux.class);
		final Mono<ImmutableList<ControlComponentPublicKeysPayload>> mono = mock(Mono.class);
		when(webClientFactory.getWebClient(anyString())).thenReturn(webClient);
		when(webClient.post()).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.accept(any())).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
		when(requestBodyUriSpec.body(any(), any(Class.class))).thenReturn(requestBodyUriSpec);
		when(responseSpec.bodyToFlux(ControlComponentPublicKeysPayload.class)).thenReturn(flux);
		when(flux.retryWhen(any())).thenReturn(flux);
		when(flux.collect(any(Collector.class))).thenReturn(mono);
		when(mono.block()).thenReturn(controlComponentPublicKeysPayloads);

		final IllegalStateException illegalStateException =
				assertThrows(IllegalStateException.class, () -> requestCcKeysService.performRequestCcKeys(electionEventId));

		final String expectedMessage = String.format(
				"There number of Control Component public keys payloads expected is incorrect. [received: %s, expected: %s, electionEventId: %s]",
				controlComponentPublicKeysPayloads.size(), ControlComponentNode.ids().size(), electionEventId);

		assertEquals(expectedMessage, Throwables.getRootCause(illegalStateException).getMessage());
	}

	@DisplayName("executing requestCcKeys(), with keys retrieved from invalid nodes, throws an IllegalStateException.")
	@Test
	@SuppressWarnings("unchecked")
	void requestCCKeysInvalidExpectedKeys() {

		when(electionEventContextPayloadService.load(anyString())).thenReturn(electionEventContextPayload);

		final ImmutableList<ControlComponentPublicKeysPayload> controlComponentPublicKeysPayloads = Stream.concat(
						ControlComponentNode.ids().stream().skip(1).map(this::createControlComponentPayload),
						Stream.of(createControlComponentPayload(ControlComponentNode.last().id())))
				.collect(toImmutableList());

		final WebClient webClient = mock(WebClient.class);
		final WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
		final WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
		final Flux<ControlComponentPublicKeysPayload> flux = mock(Flux.class);
		final Mono<ImmutableList<ControlComponentPublicKeysPayload>> mono = mock(Mono.class);
		when(webClientFactory.getWebClient(anyString())).thenReturn(webClient);
		when(webClient.post()).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.accept(any())).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
		when(requestBodyUriSpec.body(any(), any(Class.class))).thenReturn(requestBodyUriSpec);
		when(responseSpec.bodyToFlux(ControlComponentPublicKeysPayload.class)).thenReturn(flux);
		when(flux.retryWhen(any())).thenReturn(flux);
		when(flux.collect(any(Collector.class))).thenReturn(mono);
		when(mono.block()).thenReturn(controlComponentPublicKeysPayloads);

		final IllegalStateException illegalStateException =
				assertThrows(IllegalStateException.class, () -> requestCcKeysService.performRequestCcKeys(electionEventId));

		final String expectedMessage = "The Control Component public keys payloads node ids do not match the expected node ids.";

		assertTrue(Throwables.getRootCause(illegalStateException).getMessage().startsWith(expectedMessage));
	}

	@DisplayName("executing requestCcKeys(), with valid keys retrieved, does not throw and saves.")
	@Test
	@SuppressWarnings("unchecked")
	void requestCCKeysHappyPath() {

		when(electionEventContextPayloadService.load(anyString())).thenReturn(electionEventContextPayload);

		final ImmutableList<ControlComponentPublicKeysPayload> controlComponentPublicKeysPayloads = ControlComponentNode.ids().stream()
				.map(this::createControlComponentPayload)
				.collect(toImmutableList());

		final WebClient webClient = mock(WebClient.class);
		final WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
		final WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
		final Flux<ControlComponentPublicKeysPayload> flux = mock(Flux.class);
		final Mono<ImmutableList<ControlComponentPublicKeysPayload>> mono = mock(Mono.class);
		when(webClientFactory.getWebClient(anyString())).thenReturn(webClient);
		when(webClient.post()).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.accept(any())).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
		when(requestBodyUriSpec.body(any(), any(Class.class))).thenReturn(requestBodyUriSpec);
		when(responseSpec.bodyToFlux(ControlComponentPublicKeysPayload.class)).thenReturn(flux);
		when(flux.retryWhen(any())).thenReturn(flux);
		when(flux.collect(any(Collector.class))).thenReturn(mono);
		when(mono.block()).thenReturn(controlComponentPublicKeysPayloads);

		assertDoesNotThrow(() -> requestCcKeysService.performRequestCcKeys(electionEventId));

		verify(controlComponentPublicKeysService, times(ControlComponentNode.ids().size())).save(any());
	}

	private ControlComponentPublicKeysPayload createControlComponentPayload(final int nodeId) {
		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);
		final ElGamalMultiRecipientPublicKey ccrChoiceReturnCodesEncryptionPublicKey = elGamalGenerator.genRandomPublicKey(2);
		final ElGamalMultiRecipientPublicKey ccmElectionPublicKey = elGamalGenerator.genRandomPublicKey(2);

		final ZqGroup zqGroup = ZqGroup.sameOrderAs(gqGroup);
		final SchnorrProof schnorrProof = new SchnorrProof(ZqElement.create(2, zqGroup), ZqElement.create(2, zqGroup));
		final GroupVector<SchnorrProof, ZqGroup> schnorrProofs = GroupVector.of(schnorrProof, schnorrProof);

		final ControlComponentPublicKeys controlComponentPublicKeys = new ControlComponentPublicKeys(nodeId, ccrChoiceReturnCodesEncryptionPublicKey,
				schnorrProofs, ccmElectionPublicKey, schnorrProofs);

		return new ControlComponentPublicKeysPayload(gqGroup, electionEventId, controlComponentPublicKeys);
	}
}
