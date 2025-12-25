/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory.createElGamal;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory.createBase64;
import static ch.post.it.evoting.securedatamanager.shared.Constants.TOO_SMALL_CHUNK_SIZE_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.securedatamanager.setup.process.precompute.PrecomputeContext;

@DisplayName("genVerDat called with")
class GenVerDatServiceTest {

	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final int CHUNK_SIZE = 3;

	private static GenVerDatService genVerDatService;
	private static PrecomputeContext precomputeContext;
	private static int numberOfEligibleVoters;
	private static ElectionEventContextPayload electionEventContextPayload;
	private static GqGroup encryptionGroup;
	private static PrimesMappingTable primesMappingTable;
	private static ElGamalMultiRecipientPublicKey setupPublicKey;

	@BeforeAll
	static void setUpAll() {
		final GenVerDatAlgorithm genVerDatAlgorithm = new GenVerDatAlgorithm(createElGamal(), createHash(), random, createBase64(),
				new PrimesMappingTableAlgorithms());
		genVerDatService = new GenVerDatService(genVerDatAlgorithm);

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		final String electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();
		final VerificationCardSetContext verificationCardSetContext = electionEventContextPayload.getElectionEventContext()
				.verificationCardSetContexts()
				.get(0);
		primesMappingTable = verificationCardSetContext.getPrimesMappingTable();
		precomputeContext = new PrecomputeContext(electionEventId, verificationCardSetContext.getBallotBoxId(),
				verificationCardSetContext.getVerificationCardSetId());
		numberOfEligibleVoters = verificationCardSetContext.getNumberOfEligibleVoters();

		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(encryptionGroup);
		setupPublicKey = elGamalGenerator.genRandomKeyPair(electionEventContextPayload.getElectionEventContext().maximumNumberOfVotingOptions())
				.getPublicKey();
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, numberOfEligibleVoters, CHUNK_SIZE, electionEventContextPayload, setupPublicKey, primesMappingTable),
				Arguments.of(precomputeContext, numberOfEligibleVoters, CHUNK_SIZE, null, setupPublicKey, primesMappingTable),
				Arguments.of(precomputeContext, numberOfEligibleVoters, CHUNK_SIZE, electionEventContextPayload, null, primesMappingTable),
				Arguments.of(precomputeContext, numberOfEligibleVoters, CHUNK_SIZE, electionEventContextPayload, setupPublicKey, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void genVerDatWithNullParametersThrows(final PrecomputeContext precomputeContext, final int numberOfEligibleVoters,
			final int chunkSize,
			final ElectionEventContextPayload electionEventContextPayload, final ElGamalMultiRecipientPublicKey setupPublicKey,
			final PrimesMappingTable primesMappingTable) {
		assertThrows(NullPointerException.class, () -> genVerDatService.genVerDat(precomputeContext, numberOfEligibleVoters, chunkSize,
				electionEventContextPayload, setupPublicKey, primesMappingTable));
	}

	@ParameterizedTest
	@ValueSource(
			ints = { -1 }
	)
	@DisplayName("invalid number of eligible voters throws IllegalArgumentException")
	void genVerDatWithInvalidNumberOfEligibleVotersThrows(final int invalidNumberOfEligibleVoters) {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genVerDatService.genVerDat(precomputeContext, invalidNumberOfEligibleVoters, CHUNK_SIZE, electionEventContextPayload,
						setupPublicKey,
						primesMappingTable));

		final String expected = "The number of eligible voters must be positive.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@ParameterizedTest
	@ValueSource(
			ints = { -1, 0 }
	)
	@DisplayName("invalid chunk size throws IllegalArgumentException")
	void genVerDatWithInvalidChunkSizeThrows(final int invalidChunkSize) {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genVerDatService.genVerDat(precomputeContext, numberOfEligibleVoters, invalidChunkSize,
						electionEventContextPayload, setupPublicKey, primesMappingTable));

		assertEquals(TOO_SMALL_CHUNK_SIZE_MESSAGE, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("setup public key encryption group different than election event context payload encryption group throws IllegalArgumentException")
	void genVerDatWithSetupPublicKeyEncryptionGroupNonMatchingThrows() {
		final ElGamalMultiRecipientPublicKey anotherSetupPublicKey = mock(ElGamalMultiRecipientPublicKey.class);
		when(anotherSetupPublicKey.getGroup()).thenReturn(GroupTestData.getDifferentGqGroup(encryptionGroup));

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genVerDatService.genVerDat(precomputeContext, numberOfEligibleVoters, CHUNK_SIZE, electionEventContextPayload,
						anotherSetupPublicKey, primesMappingTable));

		assertEquals("The group of the setup public key must be equal to the encryption group.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("primes mapping table encryption group different than election event context payload encryption group throws IllegalArgumentException")
	void genVerDatWithPrimesMappingTableEncryptionGroupNonMatchingThrows() {
		final PrimesMappingTable anotherPrimesMappingTable = mock(PrimesMappingTable.class);
		when(anotherPrimesMappingTable.getEncryptionGroup()).thenReturn(GroupTestData.getDifferentGqGroup(encryptionGroup));

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genVerDatService.genVerDat(precomputeContext, numberOfEligibleVoters, CHUNK_SIZE, electionEventContextPayload,
						setupPublicKey, anotherPrimesMappingTable));

		assertEquals("The group of the primes mapping table must be equal to the encryption group.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	void genVerDatWithNonMatchingElectionEventIdThrows() {
		final ElectionEventContextPayload anotherElectionEventContextPayload = mock(ElectionEventContextPayload.class);
		final ElectionEventContext anotherElectionEventContext = mock(ElectionEventContext.class);
		when(anotherElectionEventContext.electionEventId()).thenReturn(uuidGenerator.generate());
		when(anotherElectionEventContextPayload.getElectionEventContext()).thenReturn(anotherElectionEventContext);
		when(anotherElectionEventContextPayload.getEncryptionGroup()).thenReturn(encryptionGroup);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genVerDatService.genVerDat(precomputeContext, numberOfEligibleVoters, CHUNK_SIZE, anotherElectionEventContextPayload,
						setupPublicKey, primesMappingTable));

		assertEquals("The election event identifier must be equal to the election event identifier in the election event context payload.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void genVerDatWithValidParametersDoesNotThrow() {
		assertDoesNotThrow(() -> genVerDatService.genVerDat(precomputeContext, numberOfEligibleVoters, CHUNK_SIZE,
				electionEventContextPayload, setupPublicKey, primesMappingTable));
	}

}
