/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.configuration.setupvoting.ComputingStatus;
import ch.post.it.evoting.domain.generators.ControlComponentCodeSharesPayloadGenerator;
import ch.post.it.evoting.domain.generators.SetupComponentVerificationDataPayloadGenerator;
import ch.post.it.evoting.domain.reactor.Box;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;
import ch.post.it.evoting.votingserver.ArtemisSupport;
import ch.post.it.evoting.votingserver.BroadcastIntegrationTestService;
import ch.post.it.evoting.votingserver.process.ElectionEventEntity;
import ch.post.it.evoting.votingserver.process.ElectionEventService;
import ch.post.it.evoting.votingserver.process.VerificationCardSetEntity;
import ch.post.it.evoting.votingserver.process.VerificationCardSetRepository;

import reactor.core.publisher.Flux;

@DisplayName("ComputeDownloadEncryptedLongCodeSharesController integration test")
class ComputeDownloadEncryptedLongCodeSharesControllerIT extends ArtemisSupport {

	private static SetupComponentVerificationDataPayload requestPayload;
	private final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
	@Autowired
	private BroadcastIntegrationTestService broadcastIntegrationTestService;
	@Autowired
	private WebTestClient webTestClient;

	@BeforeAll
	static void setUpElection(
			@Autowired
			final ElectionEventService electionEventService,
			@Autowired
			final VerificationCardSetRepository verificationCardSetRepository) {

		final SetupComponentVerificationDataPayloadGenerator setupComponentVerificationDataPayloadGenerator = new SetupComponentVerificationDataPayloadGenerator();
		requestPayload = setupComponentVerificationDataPayloadGenerator.generate();

		final String electionEventId = requestPayload.getElectionEventId();
		final String verificationCardSetId = requestPayload.getVerificationCardSetId();
		final GqGroup encryptionGroup = requestPayload.getEncryptionGroup();

		// Save election event.
		final ElectionEventEntity savedElectionEventEntity = electionEventService.save(electionEventId, encryptionGroup);

		final VerificationCardSetEntity verificationCardSetEntity = new VerificationCardSetEntity.Builder()
				.setVerificationCardSetId(verificationCardSetId)
				.setElectionEventEntity(savedElectionEventEntity)
				.setVerificationCardSetDescription("description")
				.setVerificationCardSetAlias("alias-123")
				.setDomainsOfInfluence(ImmutableList.of("domain1", "domain2"))
				.build();

		verificationCardSetRepository.save(verificationCardSetEntity);
	}

	@Test
	@DisplayName("Simulate Compute and Download EncryptedLongReturnCodeShares Request ")
	void firstTimeCommand() throws InterruptedException, TimeoutException {
		final CountDownLatch webClientCountDownLatch = new CountDownLatch(1);

		final ExecutorService executorService = Executors.newFixedThreadPool(1, new CustomizableThreadFactory("http-pool-"));

		final String electionEventId = requestPayload.getElectionEventId();
		final String verificationCardSetId = requestPayload.getVerificationCardSetId();

		//Send the HTTP request in a separate thread and wait for the results.
		executorService.execute(() -> {
			webTestClient
					.put()
					.uri(uriBuilder -> uriBuilder
							.path("/api/v1/configuration/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}/computegenenclongcodeshares")
							.build(electionEventId, verificationCardSetId))
					.contentType(MediaType.APPLICATION_NDJSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(Flux.just(requestPayload), SetupComponentVerificationDataPayload.class)
					.exchange()
					.expectStatus().isCreated();

			webClientCountDownLatch.countDown();
		});

		if (!webClientCountDownLatch.await(30, SECONDS)) {
			throw new TimeoutException("Timed out waiting for request to be sent. ");
		}

		final ControlComponentCodeSharesPayloadGenerator controlComponentCodeSharesPayloadGenerator = new ControlComponentCodeSharesPayloadGenerator(
				requestPayload.getEncryptionGroup());
		final int numberOfEligibleVoters = requestPayload.getSetupComponentVerificationData().size();
		final int numberOfVotingOptions = requestPayload.getSetupComponentVerificationData().getFirst().encryptedHashedSquaredConfirmationKey()
				.size();
		final ImmutableList<ControlComponentCodeSharesPayload> controlComponentCodeSharesPayloads = controlComponentCodeSharesPayloadGenerator.generate(
				electionEventId, verificationCardSetId, requestPayload.getChunkId(), numberOfEligibleVoters, numberOfVotingOptions);

		broadcastIntegrationTestService.respondWith(nodeId -> controlComponentCodeSharesPayloads.get(nodeId - 1));

		final String chunkCount = "1";

		// Wait till the status is COMPUTED
		final CompletableFuture<Boolean> future = new CompletableFuture<>();
		try (final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {
			final ScheduledFuture<?> poller = scheduler.scheduleAtFixedRate(() -> {
				if (Objects.equals((webTestClient.get()
						.uri(uriBuilder -> uriBuilder
								.path("/api/v1/configuration/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}/chunkcount/{chunkCount}/status")
								.build(electionEventId, verificationCardSetId, chunkCount))
						.accept(MediaType.APPLICATION_JSON)
						.exchange()
						.expectBody(ComputingStatus.class)
						.returnResult()
						.getResponseBody()
				), ComputingStatus.COMPUTED)) {
					future.complete(true);
				}
			}, 0, 100, TimeUnit.MILLISECONDS);

			try {
				future.orTimeout(30, TimeUnit.SECONDS).join();
			} finally {
				poller.cancel(true);
				scheduler.shutdown();
			}
		}

		final ImmutableList<ControlComponentCodeSharesPayload> downloadedControlComponentCodeSharesPayloads = webTestClient
				.post()
				.uri(uriBuilder -> uriBuilder
						.path("/api/v1/configuration/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}/download")
						.build(electionEventId, verificationCardSetId))
				.contentType(MediaType.APPLICATION_NDJSON)
				.body(Flux.just(0), Integer.class)
				.accept(MediaType.APPLICATION_NDJSON)
				.exchange()
				.expectStatus()
				.isOk()
				.returnResult(new ParameterizedTypeReference<Box<ImmutableList<ImmutableByteArray>>>() {
				})
				.getResponseBody()
				.next()
				.map(Box::boxed)
				.map(controlComponentCodeSharesPayloadBytes -> controlComponentCodeSharesPayloadBytes.stream().map(immutableByteArray -> {
					try {
						return objectMapper.readValue(immutableByteArray.elements(), ControlComponentCodeSharesPayload.class);
					} catch (final IOException e) {
						throw new UncheckedIOException(e);
					}
				}).collect(toImmutableList()))
				.block();

		assertEquals(controlComponentCodeSharesPayloads, downloadedControlComponentCodeSharesPayloads);

	}
}
