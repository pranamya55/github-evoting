/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.decrypt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.MoreCollectors;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.internal.securitylevel.SecurityLevelConfig;
import ch.post.it.evoting.cryptoprimitives.internal.securitylevel.SecurityLevelInternal;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.mixnet.Mixnet;
import ch.post.it.evoting.cryptoprimitives.mixnet.MixnetFactory;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.configuration.SetupComponentTallyDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.FactorizeAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.writeins.DecodeWriteInsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.writeins.IntegerToWriteInAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.writeins.IsWriteInOptionAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.writeins.QuadraticResidueToWriteInAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.ProcessPlaintextsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.VerifyVotingClientProofsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsAlgorithm;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentBallotBoxPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentShufflePayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentPublicKeysPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentTallyDataPayloadService;
import ch.post.it.evoting.securedatamanager.tally.process.TallyComponentVotesService;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline.MixDecOfflineAlgorithm;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline.MixDecOfflineService;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline.ProcessPlaintextsService;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline.VerifyMixDecOfflineService;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline.VerifyVotingClientProofsService;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixonline.GetMixnetInitialCiphertextsService;

@DisplayName("MixOffline with")
class MixOfflineTest {

	private static final String PASSWORD_ELECTORAL_BOARD_1 = "Password_ElectoralBoard1";
	private static final String PASSWORD_ELECTORAL_BOARD_2 = "Password_ElectoralBoard2";
	private static String electionEventId;
	private static String ballotBoxId;
	private static ElectionEventContextPayload electionEventContextPayload;
	private static SetupComponentPublicKeysPayload setupComponentPublicKeysPayload;
	private static SetupComponentTallyDataPayload setupComponentTallyDataPayload;
	private static ImmutableList<ControlComponentBallotBoxPayload> controlComponentBallotBoxPayloads;
	private static ImmutableList<ControlComponentBallotBoxPayload> invalidVotingClientProofsPayloads;
	private static ImmutableList<ControlComponentBallotBoxPayload> invalidNodeIdsPayloads;
	private static ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads;
	private final ElectionEventService electionEventService = mock(ElectionEventService.class);
	private final BallotBoxService ballotBoxService = mock(BallotBoxService.class);
	private final IdentifierValidationService identifierValidationService = new IdentifierValidationService(ballotBoxService, electionEventService);
	private final ControlComponentBallotBoxPayloadFileRepository controlComponentBallotBoxPayloadFileRepository = mock(
			ControlComponentBallotBoxPayloadFileRepository.class);
	private final TallyComponentShufflePayloadFileRepository tallyComponentShufflePayloadFileRepository = mock(
			TallyComponentShufflePayloadFileRepository.class);
	private final ElectionEventContextPayloadService electionEventContextPayloadService = mock(ElectionEventContextPayloadService.class);
	private final TallyComponentVotesService tallyComponentVotesService = mock(TallyComponentVotesService.class);
	private final SignatureKeystore<Alias> signatureKeystore = mock(SignatureKeystore.class);
	private VerifyMixDecOfflineService verifyMixDecOfflineService;
	private VerifyMixOfflineService verifyMixOfflineService;
	private MixDecOfflineService mixDecOfflineService;
	private ProcessPlaintextsService processPlaintextsService;
	private MixOfflineFacade mixOfflineFacade;

	@BeforeAll
	static void load() throws IOException {
		loadTestData();
	}

	@BeforeEach
	void setUp() throws SignatureException {
		setUpVerifyMixOffline();
		setUpMixDecOffline();
		setUpProcessPlaintexts();
		setUpMixOfflineFacade();
	}

	@Test
	@DisplayName("invalid control component ballot box payload throws IllegalStateException.")
	void failedConsistencyOfPayloadsThrows() {
		ControlComponentNode.ids().forEach(nodeId -> when(
				controlComponentBallotBoxPayloadFileRepository.getPayload(electionEventId, ballotBoxId, nodeId)).thenReturn(
				invalidNodeIdsPayloads.get(nodeId - 1)));

		final ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords = ImmutableList.of(
				new SafePasswordHolder(PASSWORD_ELECTORAL_BOARD_1.toCharArray()),
				new SafePasswordHolder(PASSWORD_ELECTORAL_BOARD_2.toCharArray()));
		final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
				() -> mixOfflineFacade.mixOffline(electionEventId, ballotBoxId, electoralBoardMembersPasswords));

		final String errorMessage = "Wrong number of control component ballot box payloads.";
		assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
	}

	@Test
	@DisplayName("invalid voting client proofs throws IllegalStateException.")
	void failedVerifyVotingClientProofsThrows() {
		ControlComponentNode.ids().forEach(nodeId -> when(
				controlComponentBallotBoxPayloadFileRepository.getPayload(electionEventId, ballotBoxId, nodeId)).thenReturn(
				invalidVotingClientProofsPayloads.get(nodeId - 1)));

		final ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords = ImmutableList.of(
				new SafePasswordHolder(PASSWORD_ELECTORAL_BOARD_1.toCharArray()),
				new SafePasswordHolder(PASSWORD_ELECTORAL_BOARD_2.toCharArray()));
		final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
				() -> mixOfflineFacade.mixOffline(electionEventId, ballotBoxId, electoralBoardMembersPasswords));

		final String errorMessage = String.format(
				"The voting client's zero-knowledge proofs are invalid. [electionEventId: %s, ballotBoxId: %s]", electionEventId, ballotBoxId);
		assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
	}

	@Test
	@DisplayName("invalid mixing and decryption proofs throws IllegalStateException.")
	void failedVerifyMixDecOfflineThrows() {
		when(verifyMixDecOfflineService.verifyMixDecOffline(any(), any(), any(), any(), any())).thenReturn(false);

		final ImmutableList<SafePasswordHolder> electoralBoardPasswords = ImmutableList.of(
				new SafePasswordHolder(PASSWORD_ELECTORAL_BOARD_1.toCharArray()),
				new SafePasswordHolder(PASSWORD_ELECTORAL_BOARD_2.toCharArray()));
		final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
				() -> mixOfflineFacade.mixOffline(electionEventId, ballotBoxId, electoralBoardPasswords));

		final String errorMessage = String.format(
				"The online control-component's mixing and decryption proofs are invalid. [electionEventId: %s, ballotBoxId: %s]", electionEventId,
				ballotBoxId);
		assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
	}

	@Test
	@DisplayName("ballot box already decrypted before mix decrypt throws IllegalStateException.")
	void failedMixDecOfflineThrows() {
		when(ballotBoxService.isDecrypted(ballotBoxId)).thenReturn(true);

		final ImmutableList<SafePasswordHolder> electoralBoardPasswords = ImmutableList.of(
				new SafePasswordHolder(PASSWORD_ELECTORAL_BOARD_1.toCharArray()),
				new SafePasswordHolder(PASSWORD_ELECTORAL_BOARD_2.toCharArray()));
		final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
				() -> mixOfflineFacade.mixOffline(electionEventId, ballotBoxId, electoralBoardPasswords));

		final String errorMessage = String.format(String.format("The ballot box has already been decrypted. [ballotBoxId: %s]", ballotBoxId));
		assertEquals(errorMessage, Throwables.getRootCause(illegalArgumentException).getMessage());

		verify(tallyComponentShufflePayloadFileRepository, times(0)).savePayload(any(), any(), any());
	}

	@Test
	@DisplayName("happy path does not throw.")
	void happyPathDoesNotThrow() {
		final ImmutableList<SafePasswordHolder> passwords = ImmutableList.of(
				new SafePasswordHolder(PASSWORD_ELECTORAL_BOARD_1.toCharArray()),
				new SafePasswordHolder(PASSWORD_ELECTORAL_BOARD_2.toCharArray()));
		try (final MockedStatic<SecurityLevelConfig> mockedSecurityLevel = mockStatic(SecurityLevelConfig.class)) {
			mockedSecurityLevel.when(SecurityLevelConfig::getSystemSecurityLevel).thenReturn(SecurityLevelInternal.STANDARD);
			mixOfflineFacade.mixOffline(electionEventId, ballotBoxId, passwords);
		}

		passwords.forEach(pw -> assertArrayEquals(
				new char[] { '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0' },
				pw.get()));
		verify(tallyComponentShufflePayloadFileRepository, times(1)).savePayload(any(), any(), any());
		verify(tallyComponentVotesService, times(1)).save(any());
	}

	private static void loadTestData() throws IOException {
		final ObjectMapper mapper = DomainObjectMapper.getNewInstance();

		final InputStream electionContextPayloadInputStream = MixOfflineFacadeTest.class.getResourceAsStream(
				"/" + MixOfflineFacadeTest.class.getSimpleName() + "/electionEventContextPayload.json");
		electionEventContextPayload = mapper.readValue(electionContextPayloadInputStream, ElectionEventContextPayload.class);
		electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();
		final ImmutableList<VerificationCardSetContext> verificationCardSetContexts = electionEventContextPayload.getElectionEventContext()
				.verificationCardSetContexts();
		// The test files controlComponentBallotBoxPayloads and controlComponentShufflePayloads corresponds to this id.
		ballotBoxId = verificationCardSetContexts.get(0).getBallotBoxId();

		final InputStream setupComponentPublicKeysPayloadInputStream = MixOfflineFacadeTest.class.getResourceAsStream(
				"/" + MixOfflineFacadeTest.class.getSimpleName() + "/setupComponentPublicKeysPayload.json");
		setupComponentPublicKeysPayload = mapper.readValue(setupComponentPublicKeysPayloadInputStream, SetupComponentPublicKeysPayload.class);

		final Resource invalidVotingClientProofsResource = new ClassPathResource(
				"/" + MixOfflineFacadeTest.class.getSimpleName() + "/controlComponentBallotBoxPayloads_invalidVotingClientProofs.json");
		invalidVotingClientProofsPayloads = mapper.readValue(invalidVotingClientProofsResource.getFile(), new TypeReference<>() {
		});

		final Resource invalidNodeIdsResource = new ClassPathResource(
				"/" + MixOfflineFacadeTest.class.getSimpleName() + "/controlComponentBallotBoxPayloads_invalidNodeIds.json");
		invalidNodeIdsPayloads = mapper.readValue(invalidNodeIdsResource.getFile(), new TypeReference<>() {
		});

		final Resource ballotBoxPayloadsResource = new ClassPathResource(
				"/" + MixOfflineFacadeTest.class.getSimpleName() + "/controlComponentBallotBoxPayloads.json");
		controlComponentBallotBoxPayloads = mapper.readValue(ballotBoxPayloadsResource.getFile(), new TypeReference<>() {
		});

		final Resource shufflePayloadsResource = new ClassPathResource(
				"/" + MixOfflineFacadeTest.class.getSimpleName() + "/controlComponentShufflePayloads.json");
		controlComponentShufflePayloads = mapper.readValue(shufflePayloadsResource.getFile(), new TypeReference<>() {
		});

		final Resource setupComponentTallyDataPayloadResource = new ClassPathResource(
				"/" + MixOfflineFacadeTest.class.getSimpleName() + "/setupComponentTallyDataPayload.json");
		setupComponentTallyDataPayload = mapper.readValue(setupComponentTallyDataPayloadResource.getFile(), SetupComponentTallyDataPayload.class);
	}

	void setUpVerifyMixOffline() {
		ControlComponentNode.ids().forEach(nodeId -> when(
				controlComponentBallotBoxPayloadFileRepository.getPayload(electionEventId, ballotBoxId, nodeId))
				.thenReturn(controlComponentBallotBoxPayloads.get(nodeId - 1)));

		final ControlComponentShufflePayloadFileRepository controlComponentShufflePayloadFileRepository = mock(
				ControlComponentShufflePayloadFileRepository.class);
		ControlComponentNode.ids().forEach(
				nodeId -> when(
						controlComponentShufflePayloadFileRepository.getPayload(electionEventId, ballotBoxId, nodeId))
						.thenReturn(controlComponentShufflePayloads.get(nodeId - 1)));

		final ZeroKnowledgeProof zeroKnowledgeProof = ZeroKnowledgeProofFactory.createZeroKnowledgeProof();
		final PrimesMappingTableAlgorithms primesMappingTableAlgorithms = new PrimesMappingTableAlgorithms();
		final GetHashContextAlgorithm getHashContextAlgorithm = new GetHashContextAlgorithm(BaseEncodingFactory.createBase64(),
				HashFactory.createHash(), primesMappingTableAlgorithms);
		final VerifyVotingClientProofsAlgorithm verifyVotingClientProofsAlgorithm = new VerifyVotingClientProofsAlgorithm(zeroKnowledgeProof,
				getHashContextAlgorithm, primesMappingTableAlgorithms);

		final SetupComponentTallyDataPayloadService setupComponentTallyDataPayloadService = mock(SetupComponentTallyDataPayloadService.class);
		final String verificationCardSetId = electionEventContextPayload.getElectionEventContext().verificationCardSetContexts().stream()
				.filter(verificationCardSetContext -> verificationCardSetContext.getBallotBoxId().equals(ballotBoxId))
				.collect(MoreCollectors.onlyElement())
				.getVerificationCardSetId();
		when(setupComponentTallyDataPayloadService.load(electionEventId, verificationCardSetId)).thenReturn(setupComponentTallyDataPayload);

		final VerifyVotingClientProofsService verifyVotingClientProofsService = new VerifyVotingClientProofsService(
				identifierValidationService, verifyVotingClientProofsAlgorithm
		);

		verifyMixDecOfflineService = mock(VerifyMixDecOfflineService.class);
		when(verifyMixDecOfflineService.verifyMixDecOffline(any(), any(), any(), any(), any())).thenReturn(true);

		final GetMixnetInitialCiphertextsAlgorithm getMixnetInitialCiphertextsAlgorithm = new GetMixnetInitialCiphertextsAlgorithm(
				HashFactory.createHash(),
				BaseEncodingFactory.createBase64(),
				ElGamalFactory.createElGamal());
		final GetMixnetInitialCiphertextsService getMixnetInitialCiphertextsService = new GetMixnetInitialCiphertextsService(
				identifierValidationService,
				primesMappingTableAlgorithms, getMixnetInitialCiphertextsAlgorithm);

		when(electionEventService.exists(electionEventId)).thenReturn(true);

		final SetupComponentPublicKeysPayloadService setupComponentPublicKeysPayloadService = mock(SetupComponentPublicKeysPayloadService.class);
		when(setupComponentPublicKeysPayloadService.load(electionEventId)).thenReturn(setupComponentPublicKeysPayload);

		verifyMixOfflineService = new VerifyMixOfflineService(signatureKeystore, verifyMixDecOfflineService, identifierValidationService,
				verifyVotingClientProofsService, getMixnetInitialCiphertextsService, setupComponentTallyDataPayloadService,
				setupComponentPublicKeysPayloadService, controlComponentShufflePayloadFileRepository, controlComponentBallotBoxPayloadFileRepository);
	}

	void setUpMixDecOffline() {
		final Hash hash = HashFactory.createHash();
		final Mixnet mixnet = MixnetFactory.createMixnet();
		final ZeroKnowledgeProof zeroKnowledgeProof = ZeroKnowledgeProofFactory.createZeroKnowledgeProof();
		final MixDecOfflineAlgorithm mixDecOfflineAlgorithm = new MixDecOfflineAlgorithm(hash, mixnet, ballotBoxService, zeroKnowledgeProof);
		final PrimesMappingTableAlgorithms primesMappingTableAlgorithms = new PrimesMappingTableAlgorithms();

		mixDecOfflineService = new MixDecOfflineService(mixDecOfflineAlgorithm, identifierValidationService,
				primesMappingTableAlgorithms);
	}

	void setUpProcessPlaintexts() {
		final FactorizeAlgorithm factorizeService = new FactorizeAlgorithm();
		final IntegerToWriteInAlgorithm integerToWriteInAlgorithm = new IntegerToWriteInAlgorithm();
		final QuadraticResidueToWriteInAlgorithm quadraticResidueToWriteInAlgorithm = new QuadraticResidueToWriteInAlgorithm(
				integerToWriteInAlgorithm);
		final ElGamal elGamal = ElGamalFactory.createElGamal();
		final DecodeWriteInsAlgorithm decodeWriteInsAlgorithm = new DecodeWriteInsAlgorithm(new IsWriteInOptionAlgorithm(),
				quadraticResidueToWriteInAlgorithm);
		final PrimesMappingTableAlgorithms primesMappingTableAlgorithms = new PrimesMappingTableAlgorithms();
		final ProcessPlaintextsAlgorithm processPlaintextsAlgorithm = new ProcessPlaintextsAlgorithm(elGamal, factorizeService,
				decodeWriteInsAlgorithm, primesMappingTableAlgorithms);

		when(electionEventContextPayloadService.loadEncryptionGroup(electionEventId)).thenReturn(electionEventContextPayload.getEncryptionGroup());

		processPlaintextsService = new ProcessPlaintextsService(processPlaintextsAlgorithm);
	}

	void setUpMixOfflineFacade() throws SignatureException {
		when(ballotBoxService.isDownloaded(ballotBoxId)).thenReturn(true);
		when(ballotBoxService.isTestBallotBox(ballotBoxId)).thenReturn(true);

		final int gracePeriod = electionEventContextPayload.getElectionEventContext().verificationCardSetContexts().stream()
				.filter(verificationCardSetContext -> verificationCardSetContext.getBallotBoxId().equals(ballotBoxId))
				.map(VerificationCardSetContext::getGracePeriod)
				.findFirst()
				.orElse(0);
		when(ballotBoxService.getGracePeriod(ballotBoxId)).thenReturn(gracePeriod);

		final ImmutableList<VerificationCardSetContext> verificationCardSetContexts = electionEventContextPayload.getElectionEventContext()
				.verificationCardSetContexts();
		when(ballotBoxService.getBallotBoxIds(electionEventId)).thenReturn(ImmutableList.of(
				verificationCardSetContexts.get(0).getBallotBoxId(),
				verificationCardSetContexts.get(1).getBallotBoxId(),
				verificationCardSetContexts.get(2).getBallotBoxId(),
				verificationCardSetContexts.get(3).getBallotBoxId()));

		final ElectionEventContextPayloadService mockElectionEventContextPayloadService = mock(ElectionEventContextPayloadService.class);
		when(mockElectionEventContextPayloadService.load(electionEventId)).thenReturn(electionEventContextPayload);

		when(signatureKeystore.verifySignature(any(), any(), any(), any())).thenReturn(true);
		when(signatureKeystore.generateSignature(any(), any())).thenReturn(electionEventContextPayload.getSignature().signatureContents());

		mixOfflineFacade = new MixOfflineFacade(ballotBoxService, mixDecOfflineService, verifyMixOfflineService, processPlaintextsService,
				tallyComponentVotesService, signatureKeystore, mockElectionEventContextPayloadService, tallyComponentShufflePayloadFileRepository);
	}

}
