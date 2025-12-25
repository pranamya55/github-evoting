/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.decrypt;

import static ch.post.it.evoting.securedatamanager.shared.Constants.BALLOT_BOX_CANNOT_BE_MIXED_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.SignatureException;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsAlgorithm;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentBallotBoxPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentShufflePayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentPublicKeysPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentTallyDataPayloadService;
import ch.post.it.evoting.securedatamanager.tally.process.TallyComponentVotesService;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline.MixDecOfflineService;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline.ProcessPlaintextsService;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline.VerifyMixDecOfflineService;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline.VerifyVotingClientProofsService;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixonline.GetMixnetInitialCiphertextsService;

@ExtendWith(MockitoExtension.class)
class MixOfflineFacadeTest {

	private static final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();

	private static final PrimesMappingTableAlgorithms primesMappingTableAlgorithms = new PrimesMappingTableAlgorithms();
	private static ElectionEventContextPayload electionEventContextPayload;
	@Mock
	private IdentifierValidationService identifierValidationService;

	@Mock
	private BallotBoxService ballotBoxService;

	@Mock
	private ControlComponentBallotBoxPayloadFileRepository controlComponentBallotBoxPayloadFileRepository;

	@Mock
	private ControlComponentShufflePayloadFileRepository controlComponentShufflePayloadFileRepository;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ElectionEventContextPayloadService electionEventContextPayloadService;

	@Mock
	private SetupComponentTallyDataPayloadService setupComponentTallyDataPayloadService;

	@Mock
	private SetupComponentPublicKeysPayloadService setupComponentPublicKeysPayloadService;

	@Mock
	private TallyComponentVotesService tallyComponentVotesService;

	@Mock
	private TallyComponentShufflePayloadFileRepository tallyComponentShufflePayloadFileRepository;

	@Mock
	private SignatureKeystore<Alias> signatureKeystoreSdmTallyService;

	@Mock
	private VerifyMixDecOfflineService verifyMixDecOfflineService;

	@Mock
	private VerifyVotingClientProofsService verifyVotingClientProofsService;

	@InjectMocks
	private MixOfflineFacade mixFacade;

	@BeforeAll
	static void setUpAll() {
		electionEventContextPayload = electionEventContextPayloadGenerator.generate();
	}

	@Nested
	class ValidateInputTest {

		private static final ImmutableList<SafePasswordHolder> PASSWORDS = ImmutableList.of(
				new SafePasswordHolder("Password_ElectoralBoard1".toCharArray()),
				new SafePasswordHolder("Password_ElectoralBoard2".toCharArray()));
		private static String electionEventId;
		private static String ballotBoxId;

		@BeforeEach
		void setUp() {
			final GetMixnetInitialCiphertextsAlgorithm getMixnetInitialCiphertextsAlgorithm = new GetMixnetInitialCiphertextsAlgorithm(
					HashFactory.createHash(),
					BaseEncodingFactory.createBase64(),
					ElGamalFactory.createElGamal());
			final GetMixnetInitialCiphertextsService getMixnetInitialCiphertextsService = new GetMixnetInitialCiphertextsService(
					identifierValidationService, primesMappingTableAlgorithms,
					getMixnetInitialCiphertextsAlgorithm);
			final ProcessPlaintextsService processPlaintextsService = mock(ProcessPlaintextsService.class);
			final MixDecOfflineService mixDecOfflineService = mock(MixDecOfflineService.class);
			final VerifyMixOfflineService verifyMixOfflineService = new VerifyMixOfflineService(signatureKeystoreSdmTallyService,
					verifyMixDecOfflineService, identifierValidationService, verifyVotingClientProofsService, getMixnetInitialCiphertextsService,
					setupComponentTallyDataPayloadService, setupComponentPublicKeysPayloadService, controlComponentShufflePayloadFileRepository,
					controlComponentBallotBoxPayloadFileRepository);

			mixFacade = new MixOfflineFacade(ballotBoxService, mixDecOfflineService, verifyMixOfflineService, processPlaintextsService,
					tallyComponentVotesService, signatureKeystoreSdmTallyService, electionEventContextPayloadService,
					tallyComponentShufflePayloadFileRepository);

			electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();
			ballotBoxId = electionEventContextPayload.getElectionEventContext().verificationCardSetContexts().get(0).getBallotBoxId();
		}

		@Test
		void mixFacadeThrowsForInvalidUUID() {
			assertThrows(FailedValidationException.class, () -> mixFacade.mixOffline("", ballotBoxId, PASSWORDS));
			assertThrows(FailedValidationException.class, () -> mixFacade.mixOffline(electionEventId, "", PASSWORDS));
		}

		@Test
		void mixFacadeThrowsWhenBallotBoxIsNotDownloaded() throws SignatureException {
			when(ballotBoxService.isDownloaded(ballotBoxId)).thenReturn(false);
			when(signatureKeystoreSdmTallyService.verifySignature(any(), any(), any(), any())).thenReturn(true);
			assertThrows(IllegalStateException.class, () -> mixFacade.mixOffline(electionEventId, ballotBoxId, PASSWORDS));
		}
	}

	@Nested
	class ValidateMixIsAllowedTest {
		private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		private static final String ANY_ID = uuidGenerator.generate();
		private static final int GRACE_PERIOD = 3600;

		private LocalDateTime electionEndTime;
		private LocalDateTime currentTime;

		@BeforeEach
		void setUp() {
			currentTime = LocalDateTimeUtils.now();
			electionEndTime = currentTime.plusSeconds(3600);

			when(ballotBoxService.getGracePeriod(anyString())).thenReturn(GRACE_PERIOD);
			when(electionEventContextPayloadService.load(anyString()).getElectionEventContext().finishTime()).thenReturn(electionEndTime);

		}

		@Test
		@DisplayName("Test ballot box can be mixed at any time.")
		void testBallotBox() {
			when(ballotBoxService.isTestBallotBox(anyString())).thenReturn(true);

			assertDoesNotThrow(() -> mixFacade.validateMixIsAllowed(ANY_ID, ANY_ID, () -> currentTime));
		}

		@Test
		@DisplayName("Possible to mix after the grace period has expired.")
		void prodBallotBoxWithElectionFinished() {
			when(ballotBoxService.isTestBallotBox(anyString())).thenReturn(false);

			assertDoesNotThrow(() -> mixFacade.validateMixIsAllowed(ANY_ID, ANY_ID, () -> electionEndTime.plusSeconds(GRACE_PERIOD).plusSeconds(1)));
		}

		@Test
		@DisplayName("Not possible to mix before the election finish, including grace period has expired")
		void prodBallotBoxWithElectionNotFinished() {
			when(ballotBoxService.isTestBallotBox(anyString())).thenReturn(false);

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> mixFacade.validateMixIsAllowed(ANY_ID, ANY_ID, () -> electionEndTime.plusSeconds(GRACE_PERIOD)));

			final String errorMessage = String.format(
					BALLOT_BOX_CANNOT_BE_MIXED_MESSAGE + "[isTestBallotBox: %s, finishTime: %s, electionEventId: %s, ballotBoxId: %s]",
					ballotBoxService.isTestBallotBox(anyString()), electionEndTime, ANY_ID, ANY_ID);
			assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
		}
	}

	@Nested
	class ValidateBallotBoxConsistencyTest {

		private static final String ID_0 = "00000000000000000000000000000000";
		private static final String ID_1 = "11111111111111111111111111111111";
		private static final String ID_2 = "22222222222222222222222222222222";
		private static final String ID_3 = "33333333333333333333333333333333";

		private VerificationCardSetContext.Builder verificationCardSetContextBuilder;
		private String electionEventId;

		@BeforeEach
		void setUp() {
			final ElectionEventContext electionEventContext = electionEventContextPayloadGenerator.generate().getElectionEventContext();
			electionEventId = electionEventContext.electionEventId();
			final VerificationCardSetContext verificationCardSetContext = electionEventContext
					.verificationCardSetContexts()
					.get(0);
			verificationCardSetContextBuilder = new VerificationCardSetContext.Builder()
					.setVerificationCardSetId(verificationCardSetContext.getVerificationCardSetId())
					.setVerificationCardSetAlias(verificationCardSetContext.getVerificationCardSetAlias())
					.setVerificationCardSetDescription(verificationCardSetContext.getVerificationCardSetDescription())
					.setBallotBoxId(verificationCardSetContext.getBallotBoxId())
					.setBallotBoxStartTime(verificationCardSetContext.getBallotBoxStartTime())
					.setBallotBoxFinishTime(verificationCardSetContext.getBallotBoxFinishTime())
					.setTestBallotBox(verificationCardSetContext.isTestBallotBox())
					.setNumberOfEligibleVoters(verificationCardSetContext.getNumberOfEligibleVoters())
					.setGracePeriod(verificationCardSetContext.getGracePeriod())
					.setPrimesMappingTable(verificationCardSetContext.getPrimesMappingTable())
					.setDomainsOfInfluence(verificationCardSetContext.getDomainsOfInfluence());
		}

		@Test
		@DisplayName("Same unordered content in DB and in context pass validation.")
		void eventAndDbContainSameIds() {
			when(ballotBoxService.getBallotBoxIds(any())).thenReturn(ImmutableList.of(ID_1, ID_2, ID_3));
			when(electionEventContextPayloadService.load(anyString()).getElectionEventContext().verificationCardSetContexts())
					.thenReturn(ImmutableList.of(
							verificationCardSetContextBuilder.setBallotBoxId(ID_3).build(),
							verificationCardSetContextBuilder.setBallotBoxId(ID_1).build(),
							verificationCardSetContextBuilder.setBallotBoxId(ID_2).build()
					));

			assertDoesNotThrow(() -> mixFacade.validateBallotBoxConsistency(electionEventId));
		}

		@Test
		@DisplayName("Break if there is a count mismatch between the DB and context.")
		void eventAndDbContainDifferentIdsCount() {
			when(ballotBoxService.getBallotBoxIds(any())).thenReturn(ImmutableList.of(ID_1, ID_2, ID_3, ID_3));
			when(electionEventContextPayloadService.load(anyString()).getElectionEventContext().verificationCardSetContexts()).thenReturn(
					ImmutableList.of(
							verificationCardSetContextBuilder.setBallotBoxId(ID_3).build(),
							verificationCardSetContextBuilder.setBallotBoxId(ID_1).build(),
							verificationCardSetContextBuilder.setBallotBoxId(ID_2).build()
					));

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> mixFacade.validateBallotBoxConsistency(electionEventId));

			final String errorMessage = String.format(
					"The number of ballot boxes in the DB and in the context mismatch. [electionEventId: %s, dbCount: %s, contextCount: %s]",
					electionEventId, 4, 3);
			assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
		}

		@Test
		@DisplayName("Break if there is a different ID between the DB and context.")
		void eventAndDbContainDifferentIds() {
			when(ballotBoxService.getBallotBoxIds(any())).thenReturn(ImmutableList.of(ID_1, ID_0, ID_3));
			when(electionEventContextPayloadService.load(anyString()).getElectionEventContext().verificationCardSetContexts()).thenReturn(
					ImmutableList.of(
							verificationCardSetContextBuilder.setBallotBoxId(ID_3).build(),
							verificationCardSetContextBuilder.setBallotBoxId(ID_1).build(),
							verificationCardSetContextBuilder.setBallotBoxId(ID_2).build()
					));

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> mixFacade.validateBallotBoxConsistency(electionEventId));

			final String errorMessage = String.format(
					"The ballot boxes are not the same in the DB and in the context. [electionEventId: %s, dbContent: %s, contextContent: %s]",
					electionEventId, ImmutableSet.of(ID_1, ID_3, ID_0), ImmutableSet.of(ID_1, ID_3, ID_2));
			assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
		}

		@Test
		@DisplayName("Break if there is a duplicate value in the DB.")
		void dDbContainDuplicate() {
			when(ballotBoxService.getBallotBoxIds(any())).thenReturn(ImmutableList.of(ID_1, ID_3, ID_3, ID_2));
			when(electionEventContextPayloadService.load(anyString()).getElectionEventContext().verificationCardSetContexts()).thenReturn(
					ImmutableList.of(
							verificationCardSetContextBuilder.setBallotBoxId(ID_3).build(),
							verificationCardSetContextBuilder.setBallotBoxId(ID_1).build(),
							verificationCardSetContextBuilder.setBallotBoxId(ID_0).build(),
							verificationCardSetContextBuilder.setBallotBoxId(ID_2).build()
					));

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> mixFacade.validateBallotBoxConsistency(electionEventId));

			final String errorMessage = String.format("There are duplicate values. [electionEventId: %s, dbContent: %s]", electionEventId,
					ImmutableList.of(ID_1, ID_3, ID_3, ID_2));
			assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
		}

		@Test
		@DisplayName("Break if there is a duplicate value in the context.")
		void eventContainDuplicate() {
			when(ballotBoxService.getBallotBoxIds(any())).thenReturn(ImmutableList.of(ID_1, ID_3, ID_0, ID_2));
			when(electionEventContextPayloadService.load(anyString()).getElectionEventContext().verificationCardSetContexts()).thenReturn(
					ImmutableList.of(
							verificationCardSetContextBuilder.setBallotBoxId(ID_3).build(),
							verificationCardSetContextBuilder.setBallotBoxId(ID_1).build(),
							verificationCardSetContextBuilder.setBallotBoxId(ID_3).build(),
							verificationCardSetContextBuilder.setBallotBoxId(ID_2).build()
					));

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> mixFacade.validateBallotBoxConsistency(electionEventId));

			final String errorMessage = String.format("There are duplicate values. [electionEventId: %s, contextContent: %s]", electionEventId,
					ImmutableList.of(ID_3, ID_1, ID_3, ID_2));
			assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
		}
	}

}
