/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory.createElGamal;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory.createBase64;
import static ch.post.it.evoting.cryptoprimitives.symmetric.SymmetricFactory.createSymmetric;
import static ch.post.it.evoting.cryptoprimitives.utils.KeyDerivationFactory.createKeyDerivation;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

@DisplayName("genCMTable called with")
class GenCMTableServiceTest {

	private static final Random random = RandomFactory.createRandom();
	private static final ElectionEventService electionEventService = mock(ElectionEventService.class);

	private static int chunkId;
	private static String electionEventId;
	private static String verificationCardSetId;
	private static GenCMTableService genCMTableService;
	private static ImmutableList<String> verificationCardIds;
	private static ImmutableList<String> correctnessInformation;
	private static ElGamalMultiRecipientPrivateKey setupSecretKey;
	private static VerificationCardSetContext verificationCardSetContext;
	private static ElectionEventContextPayload electionEventContextPayload;
	private static CombineEncLongCodeSharesOutput combineEncLongCodeSharesOutput;

	@BeforeAll
	static void setUpAll() {
		final ElGamal elGamal = createElGamal();
		final GenCMTableAlgorithm genCMTableAlgorithm = new GenCMTableAlgorithm(createHash(), createBase64(), random, elGamal, createSymmetric(),
				createKeyDerivation());
		genCMTableService = new GenCMTableService(genCMTableAlgorithm);

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		final ElectionEventContext electionEventContext = electionEventContextPayload.getElectionEventContext();
		electionEventId = electionEventContext.electionEventId();

		verificationCardSetContext = electionEventContext.verificationCardSetContexts().get(0);
		verificationCardSetId = verificationCardSetContext.getVerificationCardSetId();
		final PrimesMappingTable primesMappingTable = verificationCardSetContext.getPrimesMappingTable();
		correctnessInformation = new PrimesMappingTableAlgorithms().getCorrectnessInformation(primesMappingTable, ImmutableList.emptyList());

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		verificationCardIds = Stream.generate(uuidGenerator::generate)
				.limit(verificationCardSetContext.getNumberOfEligibleVoters())
				.collect(ImmutableList.toImmutableList());

		chunkId = random.genRandomInteger(10);

		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		final int numberOfVotingOptions = verificationCardSetContext.getPrimesMappingTable().getNumberOfVotingOptions();
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedPreChoiceReturnCodesVector = GroupVector.of(
				elGamal.neutralElement(numberOfVotingOptions, encryptionGroup),
				elGamal.neutralElement(numberOfVotingOptions, encryptionGroup),
				elGamal.neutralElement(numberOfVotingOptions, encryptionGroup),
				elGamal.neutralElement(numberOfVotingOptions, encryptionGroup)
		);
		final GroupVector<GqElement, GqGroup> preVoteCastReturnCodesVector = GroupVector.of(encryptionGroup.getGenerator(),
				encryptionGroup.getGenerator(),
				encryptionGroup.getGenerator(), encryptionGroup.getGenerator());
		final Base64Alphabet base64Alphabet = Base64Alphabet.getInstance();
		final ImmutableList<String> longVoteCastReturnCodesAllowList = Stream.generate(
						() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
				.limit(4)
				.collect(ImmutableList.toImmutableList());
		combineEncLongCodeSharesOutput = new CombineEncLongCodeSharesOutput.Builder()
				.setEncryptedPreChoiceReturnCodesVector(encryptedPreChoiceReturnCodesVector)
				.setPreVoteCastReturnCodesVector(preVoteCastReturnCodesVector)
				.setLongVoteCastReturnCodesAllowList(longVoteCastReturnCodesAllowList)
				.build();

		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(encryptionGroup);
		setupSecretKey = elGamalGenerator.genRandomKeyPair(electionEventContext.maximumNumberOfVotingOptions()).getPrivateKey();
	}

	@BeforeEach
	void setUp() {
		when(electionEventService.exists(electionEventId)).thenReturn(true);
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, correctnessInformation, verificationCardSetId, verificationCardIds, setupSecretKey,
						combineEncLongCodeSharesOutput),
				Arguments.of(electionEventContextPayload, null, verificationCardSetId, verificationCardIds, setupSecretKey,
						combineEncLongCodeSharesOutput),
				Arguments.of(electionEventContextPayload, correctnessInformation, null, verificationCardIds, setupSecretKey,
						combineEncLongCodeSharesOutput),
				Arguments.of(electionEventContextPayload, correctnessInformation, verificationCardSetId, null, setupSecretKey,
						combineEncLongCodeSharesOutput),
				Arguments.of(electionEventContextPayload, correctnessInformation, verificationCardSetId, verificationCardIds, null,
						combineEncLongCodeSharesOutput),
				Arguments.of(electionEventContextPayload, correctnessInformation, verificationCardSetId, verificationCardIds, setupSecretKey, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void genCMTableWithNullParametersThrows(final ElectionEventContextPayload electionEventContextPayload,
			final ImmutableList<String> correctnessInformation, final String verificationCardSetId, final ImmutableList<String> verificationCardIds,
			final ElGamalMultiRecipientPrivateKey setupSecretKey, final CombineEncLongCodeSharesOutput combineEncLongCodeSharesOutput) {
		assertThrows(NullPointerException.class,
				() -> genCMTableService.genCMTable(electionEventContextPayload, correctnessInformation, verificationCardSetId, verificationCardIds,
						chunkId, setupSecretKey, combineEncLongCodeSharesOutput));
	}

	@Test
	@DisplayName("non-matching encryption group between election event context payload and setup secret key throws IllegalArgumentException")
	void genCMTableWithSetupSecretKeyFromAnotherGroupThrows() {
		final ElGamalMultiRecipientPrivateKey anotherSetupSecretKey = mock(ElGamalMultiRecipientPrivateKey.class);
		when(anotherSetupSecretKey.getGroup()).thenReturn(
				GroupTestData.getDifferentZqGroup(new ZqGroup(electionEventContextPayload.getEncryptionGroup().getQ())));

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genCMTableService.genCMTable(electionEventContextPayload, correctnessInformation, verificationCardSetId, verificationCardIds,
						chunkId, anotherSetupSecretKey, combineEncLongCodeSharesOutput));

		final String expected = "The encryption group of the election event context payload and the setup secret key must have the same order.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("non-matching encryption group between election event context payload and CombineEncLongCodeShares output throws IllegalArgumentException")
	void genCMTableWithDifferentEncryptionGroupThrows() {
		final GqGroup differentEncryptionGroup = GroupTestData.getDifferentGqGroup(electionEventContextPayload.getEncryptionGroup());

		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> anotherEncryptedPreChoiceReturnCodesVector = mock(GroupVector.class);
		when(anotherEncryptedPreChoiceReturnCodesVector.getGroup()).thenReturn(differentEncryptionGroup);
		final CombineEncLongCodeSharesOutput anotherCombineEncLongCodeSharesOutput = mock(CombineEncLongCodeSharesOutput.class);
		when(anotherCombineEncLongCodeSharesOutput.getEncryptedPreChoiceReturnCodesVector()).thenReturn(anotherEncryptedPreChoiceReturnCodesVector);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genCMTableService.genCMTable(electionEventContextPayload, correctnessInformation, verificationCardSetId, verificationCardIds,
						chunkId, setupSecretKey, anotherCombineEncLongCodeSharesOutput));

		final String expected = "The encryption group of the election event context payload and the CombineEncLongCodeShares algorithm output must be the same.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}
}
