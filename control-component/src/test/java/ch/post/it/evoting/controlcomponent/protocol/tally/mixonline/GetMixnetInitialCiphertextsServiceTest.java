/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.mixonline;

import static ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory.createElGamal;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory.createBase64;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import ch.post.it.evoting.controlcomponent.process.BallotBoxEntity;
import ch.post.it.evoting.controlcomponent.process.BallotBoxService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetEntity;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.generators.ControlComponentBallotBoxPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsAlgorithm;

@DisplayName("getMixnetInitialCiphertexts called with")
class GetMixnetInitialCiphertextsServiceTest {

	private static final ElectionEventContextService electionEventContextService = mock(ElectionEventContextService.class);
	private static final BallotBoxService ballotBoxService = mock(BallotBoxService.class);

	private static GetMixnetInitialCiphertextsService getMixnetInitialCiphertextsService;
	private static GqGroup encryptionGroup;
	private static String electionEventId;
	private static BallotBoxEntity.Builder ballotBoxEntityBuilder;
	private static BallotBoxEntity ballotBoxEntity;
	private static VerificationCardSetContext verificationCardSetContext;
	private static ElGamalMultiRecipientPublicKey electionPublicKey;
	private static ImmutableList<EncryptedVerifiableVote> confirmedVotes;

	@BeforeAll
	static void setUpAll() throws JsonProcessingException {
		final GetMixnetInitialCiphertextsAlgorithm getMixnetInitialCiphertextsAlgorithm = new GetMixnetInitialCiphertextsAlgorithm(createHash(),
				createBase64(), createElGamal());
		final PrimesMappingTableAlgorithms primesMappingTableAlgorithms = new PrimesMappingTableAlgorithms();
		getMixnetInitialCiphertextsService = new GetMixnetInitialCiphertextsService(ballotBoxService, electionEventContextService,
				primesMappingTableAlgorithms, getMixnetInitialCiphertextsAlgorithm);

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();
		verificationCardSetContext = electionEventContextPayload.getElectionEventContext()
				.verificationCardSetContexts()
				.get(0);
		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		ballotBoxEntityBuilder = new BallotBoxEntity.Builder()
				.setBallotBoxId(verificationCardSetContext.getBallotBoxId())
				.setVerificationCardSetEntity(new VerificationCardSetEntity())
				.setBallotBoxStartTime(verificationCardSetContext.getBallotBoxStartTime())
				.setBallotBoxFinishTime(verificationCardSetContext.getBallotBoxFinishTime())
				.setTestBallotBox(verificationCardSetContext.isTestBallotBox())
				.setNumberOfEligibleVoters(verificationCardSetContext.getNumberOfEligibleVoters())
				.setGracePeriod(verificationCardSetContext.getGracePeriod())
				.setPrimesMappingTable(new ImmutableByteArray(objectMapper.writeValueAsBytes(verificationCardSetContext.getPrimesMappingTable())));
		ballotBoxEntity = ballotBoxEntityBuilder.build();

		final int psi = primesMappingTableAlgorithms.getPsi(verificationCardSetContext.getPrimesMappingTable());
		final int delta = primesMappingTableAlgorithms.getDelta(verificationCardSetContext.getPrimesMappingTable());
		electionPublicKey = new SetupComponentPublicKeysPayloadGenerator(encryptionGroup).generate(psi, delta)
				.getSetupComponentPublicKeys()
				.electionPublicKey();
		confirmedVotes = new ControlComponentBallotBoxPayloadGenerator(encryptionGroup).generate(electionEventId,
						verificationCardSetContext.getBallotBoxId(), psi, delta)
				.get(0)
				.getConfirmedEncryptedVotes();
	}

	@BeforeEach
	void setUp() {
		when(electionEventContextService.getElectionEventFinishTime(electionEventId))
				.thenReturn(LocalDateTimeUtils.now().minusSeconds(20 + ballotBoxEntity.getGracePeriod()));
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, electionEventId, ballotBoxEntity, electionPublicKey, confirmedVotes),
				Arguments.of(encryptionGroup, electionEventId, null, electionPublicKey, confirmedVotes),
				Arguments.of(encryptionGroup, electionEventId, ballotBoxEntity, null, confirmedVotes),
				Arguments.of(encryptionGroup, electionEventId, ballotBoxEntity, electionPublicKey, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void getMixnetInitialCiphertextsWithNullParametersThrows(final GqGroup encryptionGroup, final String electionEventId,
			final BallotBoxEntity ballotBoxEntity, final ElGamalMultiRecipientPublicKey electionPublicKey,
			final ImmutableList<EncryptedVerifiableVote> confirmedVotes) {
		assertThrows(NullPointerException.class,
				() -> getMixnetInitialCiphertextsService.getMixnetInitialCiphertexts(encryptionGroup, electionEventId, ballotBoxEntity,
						electionPublicKey, confirmedVotes));
	}

	@Test
	@DisplayName("invalid election event id throws FailedValidationException")
	void getMixnetInitialCiphertextsWithInvalidElectionEventIdThrows() {
		assertThrows(FailedValidationException.class,
				() -> getMixnetInitialCiphertextsService.getMixnetInitialCiphertexts(encryptionGroup, "InvalidElectionEventId", ballotBoxEntity,
						electionPublicKey, confirmedVotes));
	}

	@Test
	@DisplayName("non-test ballot box and election event period not ended throws IllegalStateException")
	void getMixnetInitialCiphertextsWithNonTestBallotBoxAndElectionEventPeriodNotEndedThrows() {
		final BallotBoxEntity nonTestBallotBoxEntity = ballotBoxEntityBuilder
				.setTestBallotBox(false)
				.build();

		final LocalDateTime electionEndTime = LocalDateTimeUtils.now().plusDays(10);
		when(electionEventContextService.getElectionEventFinishTime(electionEventId)).thenReturn(electionEndTime);

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> getMixnetInitialCiphertextsService.getMixnetInitialCiphertexts(encryptionGroup, electionEventId, nonTestBallotBoxEntity,
						electionPublicKey, confirmedVotes));

		final String expected = String.format(
				"Cannot produce mix net initial ciphertexts for the ballot box. [isTestBallotBox: %s, finishTime: %s, electionEventId: %s, ballotBoxId: %s]",
				nonTestBallotBoxEntity.isTestBallotBox(), electionEndTime, electionEventId, nonTestBallotBoxEntity.getBallotBoxId());
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void getMixnetInitialCiphertextsWithValidParametersDoesNotThrow() {
		when(ballotBoxService.getPrimesMappingTableByBallotBoxId(ballotBoxEntity.getBallotBoxId()))
				.thenReturn(verificationCardSetContext.getPrimesMappingTable());
		assertDoesNotThrow(() -> getMixnetInitialCiphertextsService.getMixnetInitialCiphertexts(encryptionGroup, electionEventId, ballotBoxEntity,
				electionPublicKey, confirmedVotes));
	}
}
