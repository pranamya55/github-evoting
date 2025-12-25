/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.tally.mixdecrypt;

import static ch.post.it.evoting.domain.SharedQueue.CONTROL_COMPONENT_QUEUE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_MESSAGE_TYPE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_TENANT_ID;
import static ch.post.it.evoting.domain.SharedQueue.VOTING_SERVER_ADDRESS;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.votingserver.TestKeyStoreInitializer.KEYSTORE_FILENAME_PATH;
import static ch.post.it.evoting.votingserver.TestKeyStoreInitializer.KEYSTORE_PASSWORD_FILENAME_PATH;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.security.SignatureException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.generators.ControlComponentBallotBoxPayloadGenerator;
import ch.post.it.evoting.domain.generators.ControlComponentShufflePayloadGenerator;
import ch.post.it.evoting.domain.tally.BallotBoxStatus;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineRawPayload;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineRequestPayload;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineResponsePayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentVotesHashPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;
import ch.post.it.evoting.votingserver.ArtemisSupport;
import ch.post.it.evoting.votingserver.BroadcastIntegrationTestService;
import ch.post.it.evoting.votingserver.TestDatabaseCleanUpService;
import ch.post.it.evoting.votingserver.TestSigner;
import ch.post.it.evoting.votingserver.process.ElectionEventService;

@DisplayName("MixDecryptOnlineController end to end integration test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MixDecryptControllerIT extends ArtemisSupport {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixDecryptControllerIT.class);
	private static final Random random = RandomFactory.createRandom();
	private static final String MIX_DEC_ONLINE_PATH = "tally/mixonline";
	private static final String BASE_URL = "/api/v1/" + MIX_DEC_ONLINE_PATH + "/electionevent/{electionevent}/ballotbox/{ballotboxid}";
	private static final String MIX_URL = BASE_URL + "/mix";
	private static final String STATUS_URL = BASE_URL + "/status";
	private static final String DOWNLOAD_URL = BASE_URL + "/download";

	private static String ballotBoxId;
	private static String electionEventId;
	private static ObjectMapper domainMapper;
	private static ImmutableList<ControlComponentBallotBoxPayload> controlComponentBallotBoxPayloads;
	private static ImmutableList<ControlComponentShufflePayload> responseControlComponentShufflePayloads;

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private MixDecryptOnlinePayloadService mixDecryptOnlinePayloadService;

	@Autowired
	private ControlComponentBallotBoxPayloadRepository controlComponentBallotBoxPayloadRepository;

	@Autowired
	private ControlComponentShufflePayloadRepository controlComponentShufflePayloadRepository;

	@Autowired
	private BroadcastIntegrationTestService broadcastIntegrationTestService;

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private ElectionEventService electionEventService;

	@BeforeAll
	static void setup(
			@Autowired
			final ElectionEventService electionEventService) {

		domainMapper = DomainObjectMapper.getNewInstance();

		// Responses for each node
		final ControlComponentBallotBoxPayloadGenerator controlComponentBallotBoxPayloadGenerator = new ControlComponentBallotBoxPayloadGenerator();
		controlComponentBallotBoxPayloads = controlComponentBallotBoxPayloadGenerator.generate();
		final ControlComponentBallotBoxPayload controlComponentBallotBoxPayload = controlComponentBallotBoxPayloads.get(0);
		electionEventId = controlComponentBallotBoxPayload.getElectionEventId();
		ballotBoxId = controlComponentBallotBoxPayload.getBallotBoxId();

		final ControlComponentShufflePayloadGenerator controlComponentShufflePayloadGenerator = new ControlComponentShufflePayloadGenerator();
		final int numberOfMixedVotes = controlComponentBallotBoxPayload.getConfirmedEncryptedVotes().size();
		final int numberOfWriteInsPlusOne = controlComponentBallotBoxPayload.getConfirmedEncryptedVotes().get(0).encryptedVote().size();
		responseControlComponentShufflePayloads = controlComponentShufflePayloadGenerator.generate(electionEventId, ballotBoxId,
				numberOfMixedVotes, numberOfWriteInsPlusOne);

		electionEventService.save(electionEventId, controlComponentBallotBoxPayload.getEncryptionGroup());
	}

	@AfterEach
	void cleanUpDatabase() {
		controlComponentShufflePayloadRepository.deleteAll();
		controlComponentBallotBoxPayloadRepository.deleteAll();
	}

	@AfterAll
	static void cleanUpElectionEvent(
			@Autowired
			final TestDatabaseCleanUpService testDatabaseCleanUpService) {
		testDatabaseCleanUpService.cleanUp();
	}

	@Test
	@Order(1)
	@DisplayName("mix happy path")
	void happyPath() throws Exception {
		final CompletableFuture<WebTestClient.ResponseSpec> resultFuture = new CompletableFuture<>();

		final ExecutorService executorService = Executors.newFixedThreadPool(1, new CustomizableThreadFactory("http-pool-"));

		//Send the HTTP request in a separate thread and wait for the results.
		executorService.execute(() -> {
			try {
				final WebTestClient.ResponseSpec response = webTestClient.put()
						.uri(uriBuilder -> uriBuilder
								.path(MIX_URL)
								.build(electionEventId, ballotBoxId))
						.accept(MediaType.APPLICATION_JSON)
						.exchange();

				resultFuture.complete(response);

			} catch (final Exception ex) {
				resultFuture.completeExceptionally(ex);
			}
		});

		// Waits for requests to be sent to each Control Component and provides the 4 answers.
		getMixnetInitialCiphertextsProcessor();

		IntStream.rangeClosed(1, 4).forEach(listenAndWriteToQueues(responseControlComponentShufflePayloads));

		final WebTestClient.ResponseSpec response = resultFuture.get();

		response.expectStatus().isAccepted();

		assertNull(response.expectBody(MixDecryptOnlineRequestPayload.class).returnResult().getResponseBody());

		// Wait till the status is MIXED
		final CompletableFuture<Boolean> future = new CompletableFuture<>();
		try (final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {
			final ScheduledFuture<?> poller = scheduler.scheduleAtFixedRate(() -> {
				if (Objects.equals((webTestClient.get()
						.uri(uriBuilder -> uriBuilder
								.path(STATUS_URL)
								.build(electionEventId, ballotBoxId))
						.accept(MediaType.APPLICATION_JSON)
						.exchange()
						.expectBody(BallotBoxStatus.class)
						.returnResult()
						.getResponseBody()
				), BallotBoxStatus.MIXED)) {
					future.complete(true);
				}
			}, 0, 100, TimeUnit.MILLISECONDS);

			try {
				future.orTimeout(3, TimeUnit.SECONDS).join();
			} finally {
				poller.cancel(true);
				scheduler.shutdown();
			}
		}

		// Download the processed MixDecryptOnlineRequestPayload
		final WebTestClient.ResponseSpec responseSpec = webTestClient.get()
				.uri(uriBuilder -> uriBuilder
						.path(DOWNLOAD_URL)
						.build(electionEventId, ballotBoxId))
				.accept(MediaType.APPLICATION_JSON)
				.exchange();

		responseSpec.expectStatus().isOk();

		final MixDecryptOnlineRawPayload payload = responseSpec.expectBody(MixDecryptOnlineRawPayload.class).returnResult().getResponseBody();

		assertNotNull(payload);
		assertEquals(electionEventId, payload.electionEventId());
		assertEquals(ballotBoxId, payload.ballotBoxId());
		assertNotNull(payload.controlComponentShuffleRawPayloads());
		assertEquals(4, payload.controlComponentShuffleRawPayloads().size());
	}

	private void getMixnetInitialCiphertextsProcessor() throws InterruptedException {
		final String encryptedConfirmedVotesHash = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, Base64Alphabet.getInstance());

		broadcastIntegrationTestService.awaitBroadcastRequestsSaved(30, SECONDS);

		// Simulates Control Components answers, one for each node.
		broadcastIntegrationTestService.respondWith(
				nodeId -> {
					final ControlComponentVotesHashPayload controlComponentVotesHashPayload = new ControlComponentVotesHashPayload(electionEventId,
							ballotBoxId, nodeId, encryptedConfirmedVotesHash);
					try {
						final TestSigner controlComponentSigner = new TestSigner(KEYSTORE_FILENAME_PATH, KEYSTORE_PASSWORD_FILENAME_PATH,
								Alias.getControlComponentByNodeId(nodeId));
						controlComponentSigner.sign(controlComponentVotesHashPayload,
								ChannelSecurityContextData.controlComponentVotesHash(nodeId, electionEventId, ballotBoxId));
					} catch (final IOException | SignatureException e) {
						throw new IllegalStateException("Failed to test sign control component votes hash payload", e);
					}

					return controlComponentVotesHashPayload;
				});
	}

	private IntConsumer listenAndWriteToQueues(final ImmutableList<ControlComponentShufflePayload> mixnetShufflePayloads) {

		return nodeId -> {

			// Wait for request
			final String queueName = CONTROL_COMPONENT_QUEUE + nodeId;
			final Message requestMessage = jmsTemplate.receive(queueName);

			LOGGER.debug("Message[nodeId: {}, queueName: {}]: {}", nodeId, queueName, requestMessage);

			// Check request
			assertNotNull(requestMessage);

			final String correlationId;
			try {
				correlationId = requestMessage.getJMSCorrelationID();
			} catch (final JMSException e) {
				throw new RuntimeException(e);
			}
			assertNotNull(correlationId);

			final byte[] body;
			try {
				body = requestMessage.getBody(byte[].class);
				assertNotNull(body);
			} catch (final JMSException e) {
				throw new RuntimeException(e);
			}
			LOGGER.debug("Message received. [correlationId: {}]", correlationId);

			// Create response
			try {
				final MixDecryptOnlineRequestPayload payload = domainMapper.readValue(body, MixDecryptOnlineRequestPayload.class);

				assert (payload.controlComponentShufflePayloads().size() == nodeId - 1);

				final ControlComponentBallotBoxPayload controlComponentBallotBoxPayload = controlComponentBallotBoxPayloads.get(nodeId - 1);
				final ControlComponentShufflePayload controlComponentShufflePayload = mixnetShufflePayloads.get(nodeId - 1);

				final byte[] responsePayload = domainMapper.writeValueAsBytes(
						new MixDecryptOnlineResponsePayload(controlComponentBallotBoxPayload, controlComponentShufflePayload));

				// Send response
				jmsTemplate.convertAndSend(VOTING_SERVER_ADDRESS, responsePayload, jmsMessage -> {
					jmsMessage.setJMSCorrelationID(correlationId);
					jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, MixDecryptOnlineResponsePayload.class.getName());
					jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
					return jmsMessage;
				});
				LOGGER.debug("Response sent. [correlationId: {}]", correlationId);
			} catch (final IOException e) {
				LOGGER.error("Unexpected error.", e);
			}
		};
	}

	@Test
	@Order(2)
	@DisplayName("check status when not yet Started")
	void checkStatusWhenNotStarted() {

		//Send the HTTP request in a separate thread and wait for the results.
		final BallotBoxStatus result = webTestClient.get()
				.uri(uriBuilder -> uriBuilder
						.path(STATUS_URL)
						.build(electionEventId, ballotBoxId))
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectBody(BallotBoxStatus.class)
				.returnResult()
				.getResponseBody();

		assertEquals(BallotBoxStatus.MIXING_NOT_STARTED, result);
	}

	@Test
	@Order(3)
	@DisplayName("check status when uncompleted")
	void checkStatusWhenUncompleted() {

		IntStream.rangeClosed(1, 2).forEach(node ->
				mixDecryptOnlinePayloadService.saveControlComponentShufflePayload(responseControlComponentShufflePayloads.get(node - 1))
		);

		//Send the HTTP request in a separate thread and wait for the results.
		final BallotBoxStatus result = webTestClient.get()
				.uri(uriBuilder -> uriBuilder
						.path(STATUS_URL)
						.build(electionEventId, ballotBoxId))
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectBody(BallotBoxStatus.class)
				.returnResult()
				.getResponseBody();

		assertEquals(BallotBoxStatus.MIXING, result);
	}

	@Test
	@Order(4)
	@DisplayName("check status when done processing")
	void checkStatusWhenDoneProcessing() {

		IntStream.rangeClosed(1, responseControlComponentShufflePayloads.size()).forEach(node ->
				mixDecryptOnlinePayloadService.saveControlComponentShufflePayload(responseControlComponentShufflePayloads.get(node - 1))
		);

		//Send the HTTP request in a separate thread and wait for the results.
		final BallotBoxStatus result = webTestClient.get()
				.uri(uriBuilder -> uriBuilder
						.path(STATUS_URL)
						.build(electionEventId, ballotBoxId))
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectBody(BallotBoxStatus.class)
				.returnResult()
				.getResponseBody();

		assertEquals(BallotBoxStatus.MIXED, result);
	}

	@Test
	@Order(5)
	@DisplayName("download MixDecryptOnlineRequestPayload when status is uncompleted")
	void downloadPayloadWhenUncompleted() throws Exception {

		final CompletableFuture<WebTestClient.ResponseSpec> resultFuture = new CompletableFuture<>();

		final ExecutorService executorService = Executors.newFixedThreadPool(1, new CustomizableThreadFactory("http-pool-"));

		IntStream.rangeClosed(1, 2).forEach(node ->
				mixDecryptOnlinePayloadService.saveControlComponentShufflePayload(responseControlComponentShufflePayloads.get(node - 1))
		);

		//Send the HTTP request in a separate thread and wait for the results.
		executorService.execute(() -> {

			try {
				final WebTestClient.ResponseSpec response = webTestClient.get()
						.uri(uriBuilder -> uriBuilder
								.path(DOWNLOAD_URL)
								.build(electionEventId, ballotBoxId))
						.accept(MediaType.APPLICATION_JSON)
						.exchange();

				resultFuture.complete(response);

			} catch (final Exception ex) {
				resultFuture.completeExceptionally(ex);
			}
		});

		final WebTestClient.ResponseSpec response = resultFuture.get();

		response.expectStatus().isNotFound();

		final MixDecryptOnlineRequestPayload payload = response.expectBody(MixDecryptOnlineRequestPayload.class).returnResult().getResponseBody();

		assertNull(payload);
	}

	@Test
	@Order(6)
	@DisplayName("download MixDecryptOnlineRequestPayload when status completed")
	void downloadPayloadWhenCompleted() throws Exception {

		final CompletableFuture<WebTestClient.ResponseSpec> resultFuture = new CompletableFuture<>();

		final ExecutorService executorService = Executors.newFixedThreadPool(1, new CustomizableThreadFactory("http-pool-"));

		IntStream.rangeClosed(1, controlComponentBallotBoxPayloads.size()).forEach(node ->
				mixDecryptOnlinePayloadService.saveControlComponentBallotBoxPayload(controlComponentBallotBoxPayloads.get(node - 1))
		);

		IntStream.rangeClosed(1, responseControlComponentShufflePayloads.size()).forEach(node ->
				mixDecryptOnlinePayloadService.saveControlComponentShufflePayload(responseControlComponentShufflePayloads.get(node - 1))
		);

		//Send the HTTP request in a separate thread and wait for the results.
		executorService.execute(() -> {

			try {
				final WebTestClient.ResponseSpec response = webTestClient.get()
						.uri(uriBuilder -> uriBuilder
								.path(DOWNLOAD_URL)
								.build(electionEventId, ballotBoxId))
						.accept(MediaType.APPLICATION_JSON)
						.exchange();

				resultFuture.complete(response);

			} catch (final Exception ex) {
				resultFuture.completeExceptionally(ex);
			}
		});

		final WebTestClient.ResponseSpec response = resultFuture.get();

		response.expectStatus().isOk();

		final MixDecryptOnlineRawPayload payload = response.expectBody(MixDecryptOnlineRawPayload.class).returnResult().getResponseBody();

		assertNotNull(payload);
		assertEquals(electionEventId, payload.electionEventId());
		assertEquals(ballotBoxId, payload.ballotBoxId());
		assertNotNull(payload.controlComponentBallotBoxRawPayloads());
		assertNotNull(payload.controlComponentShuffleRawPayloads());
		assertEquals(4, payload.controlComponentShuffleRawPayloads().size());
	}

}
