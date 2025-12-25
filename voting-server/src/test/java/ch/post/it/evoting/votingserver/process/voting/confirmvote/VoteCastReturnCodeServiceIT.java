/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.confirmvote;

import static ch.post.it.evoting.domain.multitenancy.TenantConstants.TEST_TENANT_ID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.domain.voting.confirmvote.ConfirmationKey;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponentlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.LongVoteCastReturnCodeShare;
import ch.post.it.evoting.domain.voting.confirmvote.VotingServerConfirmPayload;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.votingserver.ArtemisSupport;
import ch.post.it.evoting.votingserver.BroadcastIntegrationTestService;
import ch.post.it.evoting.votingserver.messaging.MessageHandler;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionCompletableFuture;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionService;
import ch.post.it.evoting.votingserver.messaging.Serializer;
import ch.post.it.evoting.votingserver.process.ElectionEventService;
import ch.post.it.evoting.votingserver.process.VerificationCardService;
import ch.post.it.evoting.votingserver.process.VerificationCardStateService;
import ch.post.it.evoting.votingserver.process.voting.ConfirmationKeyInvalidException;
import ch.post.it.evoting.votingserver.protocol.voting.confirmvote.ExtractVCCOutput;
import ch.post.it.evoting.votingserver.protocol.voting.confirmvote.ExtractVCCService;
import ch.post.it.evoting.votingserver.shelf.WorkflowShelfService;

class VoteCastReturnCodeServiceIT extends ArtemisSupport {

	private static GqGroup encryptionGroup;
	private static ConfirmationKey confirmationKey;
	private static String electionEventId;
	private static String verificationCardSetId;
	private static String verificationCardId;
	private static ContextIds contextIds;

	@MockitoSpyBean
	SignatureKeystore<Alias> signatureKeystore;
	@MockitoSpyBean
	ExtractVCCService extractVCCService;
	@MockitoSpyBean
	VerificationCardService verificationCardService;
	@MockitoSpyBean
	VerificationCardStateService verificationCardStateService;
	private VoteCastReturnCodeService voteCastReturnCodeService;
	@Autowired
	private BroadcastIntegrationTestService broadcastIntegrationTestService;
	@Autowired
	private MessageHandler messageHandler;
	@Autowired
	private Serializer serializer;
	@Autowired
	private WorkflowShelfService workflowShelfService;
	@Autowired
	private ResponseCompletionService responseCompletionService;

	@BeforeAll
	static void setUpAll() {
		encryptionGroup = GroupTestData.getGqGroup();
		final GqElement randomGqElement = new GqGroupGenerator(encryptionGroup).genMember();

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();
		verificationCardId = uuidGenerator.generate();
		contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);

		confirmationKey = new ConfirmationKey(contextIds, randomGqElement);
	}

	@BeforeEach
	void setup() throws SignatureException {
		final ElectionEventService electionEventService = mock(ElectionEventService.class);

		when(electionEventService.getEncryptionGroup(electionEventId)).thenReturn(encryptionGroup);

		doReturn(true).when(signatureKeystore).verifySignature(any(), any(), any(), any());
		doReturn(ImmutableByteArray.of((byte) 1, (byte) 2, (byte) 3)).when(signatureKeystore).generateSignature(any(), any());
		doReturn(new ExtractVCCOutput("12345678")).when(extractVCCService).extractVCC(any(), any());
		doNothing().when(verificationCardService).saveConfirmingState(any());
		doNothing().when(verificationCardService).saveConfirmedState(any(), any());
		doReturn(5).when(verificationCardService).incrementConfirmationAttempts(any());
		doReturn(0).when(verificationCardStateService).getNextConfirmationAttemptId(anyString());

		voteCastReturnCodeService = new VoteCastReturnCodeService(serializer, messageHandler, extractVCCService, electionEventService,
				workflowShelfService, verificationCardService, signatureKeystore, responseCompletionService);
	}

	@Test
	@DisplayName("Process create LVCC Share Contributions")
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void happyPath() throws InterruptedException {

		final CountDownLatch serviceCountDownLatch = new CountDownLatch(1);

		final VotingServerConfirmPayload votingServerConfirmPayload = genRequestPayload();

		final ExecutorService executorService = Executors.newFixedThreadPool(1, new CustomizableThreadFactory("service-pool-"));

		// Call the service in a separate thread and wait for the results.
		executorService.execute(() -> {
			contextHolder.setTenantId(TEST_TENANT_ID);
			voteCastReturnCodeService.retrieveShortVoteCastCode(contextIds, votingServerConfirmPayload.getConfirmationKey().element()).get();
			serviceCountDownLatch.countDown();
		});
		broadcastIntegrationTestService.awaitBroadcastRequestsSaved(30, SECONDS);

		broadcastIntegrationTestService.respondWith(this::getLongVoteCastReturnCodesShareHashResponsePayload);

		broadcastIntegrationTestService.awaitBroadcastRequestsSaved(30, SECONDS);

		broadcastIntegrationTestService.respondWith(this::generateControlComponentlVCCSharePayload);

		assertTrue(serviceCountDownLatch.await(30, SECONDS));
	}

	@Test
	@DisplayName("Process create LVCC Share Contributions with wrong verification")
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void verifylVCCShareWithControlComponentlVCCSharePayloadNotVerifiedReturnsBadRequest() throws InterruptedException {
		final CountDownLatch serviceCountDownLatch = new CountDownLatch(1);

		final VotingServerConfirmPayload votingServerConfirmPayload = genRequestPayload();

		final GqElement confirmationKeyElement = votingServerConfirmPayload.getConfirmationKey().element();

		final ExecutorService executorService = Executors.newFixedThreadPool(1, new CustomizableThreadFactory("http-pool-"));

		//Send the HTTP request in a separate thread and wait for the results.
		executorService.execute(() -> {
			contextHolder.setTenantId(TEST_TENANT_ID);
			final ResponseCompletionCompletableFuture<String> retrieveShortVoteCastCodeFuture = voteCastReturnCodeService.retrieveShortVoteCastCode(
					contextIds, confirmationKeyElement);
			assertThrows(ConfirmationKeyInvalidException.class, retrieveShortVoteCastCodeFuture::get);
			serviceCountDownLatch.countDown();
		});

		broadcastIntegrationTestService.awaitBroadcastRequestsSaved(30, SECONDS);

		broadcastIntegrationTestService.respondWith(this::getLongVoteCastReturnCodesShareHashResponsePayload);

		broadcastIntegrationTestService.awaitBroadcastRequestsSaved(30, SECONDS);

		broadcastIntegrationTestService.respondWith(this::generateControlComponentlVCCSharePayloadNotVerified);

		assertTrue(serviceCountDownLatch.await(30, SECONDS));
	}

	private ControlComponenthlVCCSharePayload getLongVoteCastReturnCodesShareHashResponsePayload(final Integer nodeId) {
		final int confirmationAttemptId = 0;
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(
				new ImmutableByteArray("randomSignatureContents".getBytes(StandardCharsets.UTF_8)));
		return new ControlComponenthlVCCSharePayload(encryptionGroup, nodeId, "vC2rMev+BFaS8l2apryuSJEEmV5B/9xU0DeYuxpKUrc=", confirmationKey,
				confirmationAttemptId, signature);
	}

	private VotingServerConfirmPayload genRequestPayload() {
		final int confirmationAttemptId = 0;
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(new ImmutableByteArray("".getBytes(StandardCharsets.UTF_8)));
		return new VotingServerConfirmPayload(encryptionGroup, confirmationKey, confirmationAttemptId, signature);
	}

	private ControlComponentlVCCSharePayload generateControlComponentlVCCSharePayload(final Integer nodeId) {
		final GqElement randomGqElement = new GqGroupGenerator(encryptionGroup).genMember();
		final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare = new LongVoteCastReturnCodeShare(electionEventId,
				verificationCardSetId, verificationCardId, nodeId, randomGqElement);
		final ControlComponentlVCCSharePayload longReturnCodesSharePayload = new ControlComponentlVCCSharePayload(electionEventId,
				verificationCardSetId, verificationCardId, nodeId, encryptionGroup, longVoteCastReturnCodeShare, confirmationKey, true);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(
				new ImmutableByteArray("randomSignatureContents".getBytes(StandardCharsets.UTF_8)));
		longReturnCodesSharePayload.setSignature(signature);
		return longReturnCodesSharePayload;
	}

	private ControlComponentlVCCSharePayload generateControlComponentlVCCSharePayloadNotVerified(final Integer nodeId) {
		final ControlComponentlVCCSharePayload longReturnCodesSharePayload = new ControlComponentlVCCSharePayload(electionEventId,
				verificationCardSetId, verificationCardId, nodeId, encryptionGroup, confirmationKey, false);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(
				new ImmutableByteArray("randomSignatureContents".getBytes(StandardCharsets.UTF_8)));
		longReturnCodesSharePayload.setSignature(signature);
		return longReturnCodesSharePayload;
	}
}
