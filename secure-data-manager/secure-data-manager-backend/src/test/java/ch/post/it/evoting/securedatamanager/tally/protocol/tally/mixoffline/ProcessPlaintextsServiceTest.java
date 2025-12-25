/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory.createElGamal;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.DecryptionProof;
import ch.post.it.evoting.domain.generators.ControlComponentShufflePayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.TallyComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.VerifiablePlaintextDecryption;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.FactorizeAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.writeins.DecodeWriteInsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.writeins.IntegerToWriteInAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.writeins.IsWriteInOptionAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.writeins.QuadraticResidueToWriteInAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.ProcessPlaintextsAlgorithm;

@DisplayName("processPlaintexts called with")
class ProcessPlaintextsServiceTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static ProcessPlaintextsService processPlaintextsService;
	private static String electionEventId;
	private static String ballotBoxId;
	private static TallyComponentShufflePayload tallyComponentShufflePayload;
	private static PrimesMappingTable primesMappingTable;

	@BeforeAll
	static void setUpAll() {
		final QuadraticResidueToWriteInAlgorithm quadraticResidueToWriteInAlgorithm = new QuadraticResidueToWriteInAlgorithm(
				new IntegerToWriteInAlgorithm());
		final DecodeWriteInsAlgorithm decodeWriteInsAlgorithm = new DecodeWriteInsAlgorithm(new IsWriteInOptionAlgorithm(),
				quadraticResidueToWriteInAlgorithm);
		final PrimesMappingTableAlgorithms primesMappingTableAlgorithms = new PrimesMappingTableAlgorithms();
		final ProcessPlaintextsAlgorithm processPlaintextsAlgorithm = new ProcessPlaintextsAlgorithm(createElGamal(), new FactorizeAlgorithm(),
				decodeWriteInsAlgorithm, primesMappingTableAlgorithms);
		processPlaintextsService = new ProcessPlaintextsService(processPlaintextsAlgorithm);

		electionEventId = uuidGenerator.generate();
		ballotBoxId = uuidGenerator.generate();
		final PrimesMappingTableGenerator primesMappingTableGenerator = new PrimesMappingTableGenerator();
		primesMappingTable = primesMappingTableGenerator.generate();

		final GqGroup encryptionGroup = primesMappingTable.getEncryptionGroup();
		final ControlComponentShufflePayload controlComponentShufflePayload = new ControlComponentShufflePayloadGenerator(encryptionGroup).generate()
				.get(0);
		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(encryptionGroup);
		final GroupVector<DecryptionProof, ZqGroup> decryptionProofs = controlComponentShufflePayload.getVerifiableDecryptions()
				.getDecryptionProofs();
		final VerifiablePlaintextDecryption verifiablePlaintextDecryption = new VerifiablePlaintextDecryption(
				elGamalGenerator.genRandomMessageVector(decryptionProofs.size(), decryptionProofs.getElementSize()),
				decryptionProofs
		);
		tallyComponentShufflePayload = new TallyComponentShufflePayload(encryptionGroup, electionEventId, ballotBoxId,
				controlComponentShufflePayload.getVerifiableShuffle(), verifiablePlaintextDecryption);
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, ballotBoxId, tallyComponentShufflePayload, primesMappingTable),
				Arguments.of(electionEventId, null, tallyComponentShufflePayload, primesMappingTable),
				Arguments.of(electionEventId, ballotBoxId, null, primesMappingTable),
				Arguments.of(electionEventId, ballotBoxId, tallyComponentShufflePayload, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void processPlaintextsWithNullParametersThrows(final String electionEventId, final String ballotBoxId,
			final TallyComponentShufflePayload tallyComponentShufflePayload, final PrimesMappingTable primesMappingTable) {
		assertThrows(NullPointerException.class,
				() -> processPlaintextsService.processPlaintexts(electionEventId, ballotBoxId, tallyComponentShufflePayload, primesMappingTable));
	}

	@Test
	@DisplayName("invalid election event id throws FailedValidationException")
	void processPlaintextsWithInvalidElectionEventIdThrows() {
		assertThrows(FailedValidationException.class,
				() -> processPlaintextsService.processPlaintexts("InvalidElectionEventId", ballotBoxId, tallyComponentShufflePayload,
						primesMappingTable));
	}

	@Test
	@DisplayName("invalid ballot box id throws FailedValidationException")
	void processPlaintextsWithInvalidBallotBoxIdThrows() {
		assertThrows(FailedValidationException.class,
				() -> processPlaintextsService.processPlaintexts(electionEventId, "InvalidBallotBoxId", tallyComponentShufflePayload,
						primesMappingTable));
	}

	@Test
	@DisplayName("different election payload throws IllegalArgumentException")
	void processPlaintextsWithDifferentElectionPayloadThrows() {
		final TallyComponentShufflePayload differentElectionPayload = new TallyComponentShufflePayload(
				tallyComponentShufflePayload.getEncryptionGroup(), uuidGenerator.generate(), ballotBoxId,
				tallyComponentShufflePayload.getVerifiableShuffle(), tallyComponentShufflePayload.getVerifiablePlaintextDecryption());

		assertThrows(IllegalArgumentException.class,
				() -> processPlaintextsService.processPlaintexts(electionEventId, ballotBoxId, differentElectionPayload, primesMappingTable));
	}

	@Test
	@DisplayName("different ballot box payload throws IllegalArgumentException")
	void processPlaintextsWithDifferentBallotBoxPayloadThrows() {
		final TallyComponentShufflePayload differentBallotBoxPayload = new TallyComponentShufflePayload(
				tallyComponentShufflePayload.getEncryptionGroup(), electionEventId, uuidGenerator.generate(),
				tallyComponentShufflePayload.getVerifiableShuffle(), tallyComponentShufflePayload.getVerifiablePlaintextDecryption());

		assertThrows(IllegalArgumentException.class,
				() -> processPlaintextsService.processPlaintexts(electionEventId, ballotBoxId, differentBallotBoxPayload, primesMappingTable));
	}

	@Test
	@DisplayName("different encryption group payload throws IllegalArgumentException")
	void processPlaintextsWithDifferentEncryptionGroupPayloadThrows() {
		final GqGroup differentGroup = GroupTestData.getDifferentGqGroup(primesMappingTable.getEncryptionGroup());
		final PrimesMappingTable differentGroupPrimesMappingTable = new PrimesMappingTableGenerator(differentGroup).generate(1);

		assertThrows(IllegalArgumentException.class,
				() -> processPlaintextsService.processPlaintexts(electionEventId, ballotBoxId, tallyComponentShufflePayload,
						differentGroupPrimesMappingTable));
	}

}
