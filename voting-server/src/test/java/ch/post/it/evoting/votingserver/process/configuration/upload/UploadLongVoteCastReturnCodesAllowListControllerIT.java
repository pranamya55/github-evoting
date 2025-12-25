/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.upload;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.test.web.reactive.server.WebTestClient;

import ch.post.it.evoting.domain.configuration.setupvoting.LongVoteCastReturnCodesAllowListResponsePayload;
import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;
import ch.post.it.evoting.domain.generators.SetupComponentLVCCAllowListPayloadGenerator;
import ch.post.it.evoting.votingserver.ArtemisSupport;
import ch.post.it.evoting.votingserver.BroadcastIntegrationTestService;

@DisplayName("A UploadLongVoteCastReturnCodesAllowListController")
class UploadLongVoteCastReturnCodesAllowListControllerIT extends ArtemisSupport {

	@Autowired
	private BroadcastIntegrationTestService broadcastIntegrationTestService;

	@Autowired
	private WebTestClient webTestClient;

	private String electionEventId;
	private String verificationCardSetId;
	private SetupComponentLVCCAllowListPayload setupComponentLVCCAllowListPayload;

	@BeforeEach
	void setUp() {
		final SetupComponentLVCCAllowListPayloadGenerator setupComponentLVCCAllowListPayloadGenerator = new SetupComponentLVCCAllowListPayloadGenerator();
		setupComponentLVCCAllowListPayload = setupComponentLVCCAllowListPayloadGenerator.generate();
		electionEventId = setupComponentLVCCAllowListPayload.getElectionEventId();
		verificationCardSetId = setupComponentLVCCAllowListPayload.getVerificationCardSetId();
	}

	@Test
	@DisplayName("processing a request behaves as expected.")
	void process() throws InterruptedException {
		final CountDownLatch webClientCountDownLatch = new CountDownLatch(1);

		final ExecutorService executorService = Executors.newFixedThreadPool(1, new CustomizableThreadFactory("http-pool-"));

		//send request to controller
		executorService.execute(() -> {
			webTestClient
					.post()
					.uri(uriBuilder -> uriBuilder
							.path("/api/v1/configuration/setupvoting/longvotecastreturncodesallowlist/electionevent/")
							.pathSegment(electionEventId, "verificationcardset", verificationCardSetId)
							.build(1L))
					.accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
					.bodyValue(setupComponentLVCCAllowListPayload)
					.exchange()
					.expectStatus().isOk();
			webClientCountDownLatch.countDown();
		});

		broadcastIntegrationTestService.awaitBroadcastRequestsSaved(30, SECONDS);

		broadcastIntegrationTestService.respondWith(
				nodeId -> new LongVoteCastReturnCodesAllowListResponsePayload(nodeId, electionEventId, verificationCardSetId));

		assertTrue(webClientCountDownLatch.await(30, SECONDS));
	}
}
