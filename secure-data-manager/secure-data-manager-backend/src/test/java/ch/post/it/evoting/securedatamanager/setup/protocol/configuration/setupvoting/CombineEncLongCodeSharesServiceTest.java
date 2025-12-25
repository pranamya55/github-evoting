/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory.createElGamal;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory.createBase64;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.domain.generators.ControlComponentCodeSharesPayloadGenerator;
import ch.post.it.evoting.domain.generators.SetupComponentVerificationDataPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationData;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.generate.EncryptedNodeLongReturnCodeSharesChunk;
import ch.post.it.evoting.securedatamanager.setup.process.generate.EncryptedSingleNodeLongReturnCodeSharesChunk;

@DisplayName("combineEncLongCodeShares called with")
class CombineEncLongCodeSharesServiceTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static CombineEncLongCodeSharesService combineEncLongCodeSharesService;
	private static ElectionEventContextPayload electionEventContextPayload;
	private static ElectionEventContextPayload electionEventContextPayloadMock;
	private static String electionEventId;
	private static PrimesMappingTable primesMappingTable;
	private static String verificationCardSetId;
	private static VerificationCardSetContext verificationCardSetContext;
	private static EncryptedNodeLongReturnCodeSharesChunk.Builder encryptedNodeLongReturnCodeSharesChunkBuilder;
	private static EncryptedNodeLongReturnCodeSharesChunk encryptedNodeLongReturnCodeSharesChunk;
	private static ElGamalMultiRecipientPrivateKey setupSecretKey;

	@BeforeAll
	static void setUpAll() {
		final VerifyEncryptedPCCExponentiationProofsAlgorithm verifyEncryptedPCCExponentiationProofsAlgorithm = mock(
				VerifyEncryptedPCCExponentiationProofsAlgorithm.class);
		final VerifyEncryptedCKExponentiationProofsAlgorithm verifyEncryptedCKExponentiationProofsAlgorithm = mock(
				VerifyEncryptedCKExponentiationProofsAlgorithm.class);
		when(verifyEncryptedPCCExponentiationProofsAlgorithm.verifyEncryptedPCCExponentiationProofs(any(), any())).thenReturn(true);
		when(verifyEncryptedCKExponentiationProofsAlgorithm.verifyEncryptedCKExponentiationProofs(any(), any())).thenReturn(true);

		final CombineEncLongCodeSharesAlgorithm combineEncLongCodeSharesAlgorithm = new CombineEncLongCodeSharesAlgorithm(createHash(),
				createElGamal(), createBase64(), verifyEncryptedPCCExponentiationProofsAlgorithm, verifyEncryptedCKExponentiationProofsAlgorithm);
		combineEncLongCodeSharesService = new CombineEncLongCodeSharesService(combineEncLongCodeSharesAlgorithm);

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		electionEventContextPayload = electionEventContextPayloadGenerator.generate();

		final ElectionEventContext electionEventContext = electionEventContextPayload.getElectionEventContext();
		electionEventId = electionEventContext.electionEventId();
		verificationCardSetContext = electionEventContext.verificationCardSetContexts().get(0);
		verificationCardSetId = verificationCardSetContext.getVerificationCardSetId();
		primesMappingTable = verificationCardSetContext.getPrimesMappingTable();

		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(electionEventContextPayload.getEncryptionGroup());
		setupSecretKey = elGamalGenerator.genRandomKeyPair(electionEventContext.maximumNumberOfVotingOptions()).getPrivateKey();

		electionEventContextPayloadMock = mock(ElectionEventContextPayload.class);
		when(electionEventContextPayloadMock.getElectionEventContext()).thenReturn(electionEventContext);
		when(electionEventContextPayloadMock.getEncryptionGroup()).thenReturn(
				GroupTestData.getDifferentGqGroup(electionEventContext.encryptionGroup()));
	}

	@BeforeEach
	void setUp() {
		final GqGroup gqGroup = verificationCardSetContext.getPrimesMappingTable().getEncryptionGroup();
		final int chunkId = 1;
		final ImmutableList<String> verificationCardIds = Stream.generate(uuidGenerator::generate)
				.limit(verificationCardSetContext.getNumberOfEligibleVoters())
				.collect(ImmutableList.toImmutableList());
		final int numberOfVotingOptions = verificationCardSetContext.getNumberOfVotingOptions();

		final ControlComponentCodeSharesPayloadGenerator controlComponentCodeSharesPayloadGenerator = new ControlComponentCodeSharesPayloadGenerator(
				gqGroup);
		final ImmutableList<ControlComponentCodeSharesPayload> controlComponentCodeSharesPayloads = controlComponentCodeSharesPayloadGenerator.generate(
				electionEventId, verificationCardSetId, chunkId, verificationCardIds, numberOfVotingOptions);
		final ImmutableList<EncryptedSingleNodeLongReturnCodeSharesChunk> controlComponentCodeSharesChunks = controlComponentCodeSharesPayloads.stream()
				.map(EncryptedSingleNodeLongReturnCodeSharesChunk::new)
				.collect(toImmutableList());

		final SetupComponentVerificationDataPayloadGenerator setupComponentVerificationDataPayloadGenerator = new SetupComponentVerificationDataPayloadGenerator(
				gqGroup);
		final ImmutableList<SetupComponentVerificationData> setupComponentVerificationData = setupComponentVerificationDataPayloadGenerator.generate(
						electionEventId, verificationCardSetId, chunkId, verificationCardIds, numberOfVotingOptions)
				.getSetupComponentVerificationData();

		encryptedNodeLongReturnCodeSharesChunkBuilder = new EncryptedNodeLongReturnCodeSharesChunk.Builder()
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setChunkId(chunkId)
				.setControlComponentCodeSharesChunks(controlComponentCodeSharesChunks)
				.setSetupComponentVerificationData(setupComponentVerificationData);
		encryptedNodeLongReturnCodeSharesChunk = encryptedNodeLongReturnCodeSharesChunkBuilder.build();
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, primesMappingTable, verificationCardSetId, encryptedNodeLongReturnCodeSharesChunk, setupSecretKey),
				Arguments.of(electionEventContextPayload, null, verificationCardSetId, encryptedNodeLongReturnCodeSharesChunk, setupSecretKey),
				Arguments.of(electionEventContextPayload, primesMappingTable, null, encryptedNodeLongReturnCodeSharesChunk, setupSecretKey),
				Arguments.of(electionEventContextPayload, primesMappingTable, verificationCardSetId, null, setupSecretKey),
				Arguments.of(electionEventContextPayload, primesMappingTable, verificationCardSetId, encryptedNodeLongReturnCodeSharesChunk, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void combineEncLongCodeSharesWithNullParametersThrows(final ElectionEventContextPayload electionEventContextPayload,
			final PrimesMappingTable primesMappingTable, final String verificationCardSetId,
			final EncryptedNodeLongReturnCodeSharesChunk encryptedNodeLongReturnCodeSharesChunk,
			final ElGamalMultiRecipientPrivateKey setupSecretKey) {
		assertThrows(NullPointerException.class,
				() -> combineEncLongCodeSharesService.combineEncLongCodeShares(electionEventContextPayload, primesMappingTable, verificationCardSetId,
						encryptedNodeLongReturnCodeSharesChunk, setupSecretKey));
	}

	@Test
	@DisplayName("invalid verification card set id throws FailedValidationException")
	void combineEncLongCodeSharesWithInvalidVerificationCardSetIdThrows() {
		assertThrows(FailedValidationException.class,
				() -> combineEncLongCodeSharesService.combineEncLongCodeShares(electionEventContextPayload, primesMappingTable,
						"InvalidVerificationCardSetId", encryptedNodeLongReturnCodeSharesChunk, setupSecretKey));
	}

	@Test
	@DisplayName("encrypted node long return code shares chunk of other election event throws IllegalArgumentException")
	void combineEncLongCodeSharesWithOtherElectionEventChunkThrows() {
		final String otherElectionEventId = uuidGenerator.generate();
		final EncryptedNodeLongReturnCodeSharesChunk nonConsistentChunk = encryptedNodeLongReturnCodeSharesChunkBuilder
				.setElectionEventId(otherElectionEventId)
				.build();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> combineEncLongCodeSharesService.combineEncLongCodeShares(electionEventContextPayload, primesMappingTable,
						verificationCardSetId, nonConsistentChunk, setupSecretKey));

		final String expected = String.format(
				"The encrypted node long return code shares chunk does not correspond to the expected election event id. [expected: %s, actual: %s]",
				electionEventId, nonConsistentChunk.getElectionEventId());
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("encrypted node long return code shares chunk of other verification card set throws IllegalArgumentException")
	void combineEncLongCodeSharesWithOtherVerificationCardSetChunkThrows() {
		final String otherVerificationCardSetId = uuidGenerator.generate();
		final EncryptedNodeLongReturnCodeSharesChunk nonConsistentChunk = encryptedNodeLongReturnCodeSharesChunkBuilder
				.setVerificationCardSetId(otherVerificationCardSetId)
				.build();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> combineEncLongCodeSharesService.combineEncLongCodeShares(electionEventContextPayload, primesMappingTable,
						verificationCardSetId, nonConsistentChunk, setupSecretKey));

		final String expected = String.format(
				"The encrypted node long return code shares chunk does not correspond to the expected verification card set id. [expected: %s, actual: %s]",
				verificationCardSetId, nonConsistentChunk.getVerificationCardSetId());
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("non existent verification card set id throws IllegalArgumentException")
	void combineEncLongCodeSharesWithNonExistentVerificationCardSetIdThrows() {
		final String nonExistentVerificationCardSetId = uuidGenerator.generate();
		final EncryptedNodeLongReturnCodeSharesChunk nonExistentVerificationCardSetIdChunk = encryptedNodeLongReturnCodeSharesChunkBuilder
				.setVerificationCardSetId(nonExistentVerificationCardSetId)
				.build();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> combineEncLongCodeSharesService.combineEncLongCodeShares(electionEventContextPayload, primesMappingTable,
						nonExistentVerificationCardSetId, nonExistentVerificationCardSetIdChunk, setupSecretKey));
		final String expected = String.format("The given verification card set id does not exist. [verificationCardSetId: %s]",
				nonExistentVerificationCardSetId);
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("encryption group of election event context payload and primes mapping table must be equal")
	void combineEncLongCodeSharesWithDifferentEncryptionGroupThrows() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> combineEncLongCodeSharesService.combineEncLongCodeShares(electionEventContextPayloadMock, primesMappingTable,
						verificationCardSetId, encryptedNodeLongReturnCodeSharesChunk, setupSecretKey));
		assertEquals("The encryption group of the election event context payload and the primes mapping table must be equal.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("encryption group of election event context payload and setup secret key must be of same order")
	void combineEncLongCodeSharesWithDifferentOrderEncryptionGroupThrows() {
		final PrimesMappingTable primesMappingTableMock = mock(PrimesMappingTable.class);
		final GqGroup encryptionGroup = electionEventContextPayloadMock.getEncryptionGroup();
		when(primesMappingTableMock.getEncryptionGroup()).thenReturn(encryptionGroup);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> combineEncLongCodeSharesService.combineEncLongCodeShares(electionEventContextPayloadMock, primesMappingTableMock,
						verificationCardSetId, encryptedNodeLongReturnCodeSharesChunk, setupSecretKey));

		assertEquals("The encryption group of the election event context payload and the setup secret key must be of same order.",
				Throwables.getRootCause(exception).getMessage());
	}
}
