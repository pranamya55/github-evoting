/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.sendvote;

import static ch.post.it.evoting.domain.multitenancy.TenantConstants.TEST_TENANT_ID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.SignatureException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentPartialDecryptPayload;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentlCCSharePayload;
import ch.post.it.evoting.domain.voting.sendvote.VotingServerEncryptedVotePayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.votingserver.ArtemisSupport;
import ch.post.it.evoting.votingserver.BroadcastIntegrationTestService;
import ch.post.it.evoting.votingserver.messaging.MessageHandler;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionService;
import ch.post.it.evoting.votingserver.messaging.Serializer;
import ch.post.it.evoting.votingserver.process.ElectionEventService;
import ch.post.it.evoting.votingserver.process.VerificationCardService;
import ch.post.it.evoting.votingserver.protocol.voting.sendvote.ExtractCRCOutput;
import ch.post.it.evoting.votingserver.protocol.voting.sendvote.ExtractCRCService;
import ch.post.it.evoting.votingserver.shelf.WorkflowShelfService;

@DisplayName("ReturnCodesPartialDecryptContributionsServiceIT end to end integration test")
class ReturnCodesPartialDecryptContributionsServiceIT extends ArtemisSupport {

	@MockitoSpyBean
	private ExtractCRCService extractCRCService;
	private ElectionEventService electionEventServiceMock;
	@MockitoSpyBean
	private VerificationCardService verificationCardService;
	@MockitoSpyBean
	private SignatureKeystore<Alias> signatureKeystoreService;
	@MockitoSpyBean
	private PrimesMappingTableAlgorithms primesMappingTableAlgorithms;
	@Autowired
	private ResponseCompletionService responseCompletionService;
	@Autowired
	private MessageHandler messageHandler;
	@Autowired
	private Serializer serializer;
	@Autowired
	private WorkflowShelfService workflowShelfService;
	private ChoiceReturnCodesService choiceReturnCodesService;
	@Autowired
	private BroadcastIntegrationTestService broadcastIntegrationTestService;
	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void beforeEach() {
		electionEventServiceMock = mock(ElectionEventService.class);
		choiceReturnCodesService = new ChoiceReturnCodesService(serializer, messageHandler, extractCRCService, electionEventServiceMock,
				workflowShelfService, verificationCardService, signatureKeystoreService, responseCompletionService);
	}

	@Test
	@DisplayName("Process VotingServerEncryptedVotePayload, happy path")
	void firstTimeCommand() throws IOException, InterruptedException, SignatureException {
		final String electionEventId = "E5E61F25642CDCA4394DCFCB65CBEC19";
		final String verificationCardSetId = "7554BA2D79F3D89364D64F719FBD0E3B";
		final String verificationCardId = "564D8D5170B612056B7C5CF8B5C4209B";
		final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
		final String credentialId = "D82843687647805B09E514C46C776FAD";

		final CountDownLatch webClientCountDownLatch = new CountDownLatch(1);

		final Resource payloadsResource = new ClassPathResource("/process/voting/sendvote/voting-server-encrypted-vote-payload.json");

		final VotingServerEncryptedVotePayload requestPayload = objectMapper.readValue(payloadsResource.getFile(),
				VotingServerEncryptedVotePayload.class);

		final ExecutorService executorService = Executors.newFixedThreadPool(1, new CustomizableThreadFactory("http-pool-"));

		when(electionEventServiceMock.getEncryptionGroup(any())).thenReturn(requestPayload.getEncryptionGroup());

		doReturn(true).when(signatureKeystoreService).verifySignature(any(), any(), any(), any());
		doReturn(ImmutableByteArray.of((byte) 1, (byte) 2, (byte) 3, (byte) 4)).when(signatureKeystoreService).generateSignature(any(), any());

		doReturn(null).when(verificationCardService).getPrimesMappingTable(anyString());
		doReturn(ImmutableList.of("1")).when(primesMappingTableAlgorithms).getBlankCorrectnessInformation(any());
		doReturn(new ExtractCRCOutput(ImmutableList.of("1234", "4567"))).when(extractCRCService).extractCRC(any(), any());
		doNothing().when(verificationCardService).saveSentState(anyString(), any());

		// Send the HTTP request in a separate thread and wait for the results.
		executorService.execute(() -> {
			contextHolder.setTenantId(TEST_TENANT_ID);
			choiceReturnCodesService.retrieveShortChoiceReturnCodes(contextIds, credentialId, requestPayload.getEncryptedVerifiableVote());
			webClientCountDownLatch.countDown();
		});

		broadcastIntegrationTestService.awaitBroadcastRequestsSaved(30, SECONDS);

		final Resource ccPartialDecryptPayloadsResource = new ClassPathResource(
				"/process/voting/sendvote/control-component-partial-decrypt-payloads.json");
		final ImmutableList<ControlComponentPartialDecryptPayload> ccPartialDecryptPayloads = objectMapper
				.readValue(ccPartialDecryptPayloadsResource.getFile(), new TypeReference<>() {
				});

		broadcastIntegrationTestService.respondWith(nodeId -> ccPartialDecryptPayloads.get(nodeId - 1));

		broadcastIntegrationTestService.awaitBroadcastRequestsSaved(120, SECONDS);

		final Resource ccLCCSharePayloadsResource = new ClassPathResource("/process/voting/sendvote/control-component-LCC-share-payloads.json");
		final ImmutableList<ControlComponentlCCSharePayload> ccLCCSharePayloads = objectMapper
				.readValue(ccLCCSharePayloadsResource.getFile(), new TypeReference<>() {
				});

		broadcastIntegrationTestService.respondWith(nodeId -> ccLCCSharePayloads.get(nodeId - 1));

		assertTrue(webClientCountDownLatch.await(30, SECONDS));
	}

}
