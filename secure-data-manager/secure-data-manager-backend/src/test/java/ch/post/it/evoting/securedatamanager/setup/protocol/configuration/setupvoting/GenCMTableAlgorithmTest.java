/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.symmetric.Symmetric;
import ch.post.it.evoting.cryptoprimitives.symmetric.SymmetricFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivation;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivationFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;

@DisplayName("GenCMTableAlgorithm calling genCMTable with")
class GenCMTableAlgorithmTest extends TestGroupSetup {

	private static final Random random = RandomFactory.createRandom();

	private static GenCMTableAlgorithm genCMTableAlgorithm;

	private GenCMTableInput genCMTableInput;
	private GenCMTableContext genCMTableContext;

	@BeforeAll
	static void setUpAll() {
		final Hash hash = HashFactory.createHash();
		final Base64 base64 = BaseEncodingFactory.createBase64();
		final ElGamal elGamal = ElGamalFactory.createElGamal();
		final Symmetric symmetric = SymmetricFactory.createSymmetric();
		final KeyDerivation keyDerivation = KeyDerivationFactory.createKeyDerivation();

		genCMTableAlgorithm = new GenCMTableAlgorithm(hash, base64, random, elGamal, symmetric, keyDerivation);
	}

	@BeforeEach
	void setUp() {
		final int n = randomService.genRandomInteger(1, 5);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		final ImmutableList<String> correctnessInformation = IntStream.range(0, n)
				.mapToObj(i -> uuidGenerator.generate())
				.collect(toImmutableList());
		final int N_e = randomService.genRandomInteger(1, 10);
		final ImmutableList<String> verificationCardIds = IntStream.range(0, N_e)
				.mapToObj(i -> uuidGenerator.generate())
				.collect(toImmutableList());

		genCMTableContext = new GenCMTableContext.Builder()
				.setEncryptionGroup(gqGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardIds(verificationCardIds)
				.setCorrectnessInformation(correctnessInformation)
				.setMaximumNumberOfVotingOptions(MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS)
				.build();

		final ElGamalMultiRecipientPrivateKey setupSecretKey = new ElGamalMultiRecipientPrivateKey(
				IntStream.range(0, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS)
						.mapToObj(i -> zqGroupGenerator.genRandomZqElementMember())
						.collect(GroupVector.toGroupVector()));
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedPreChoiceReturnCodes = elGamalGenerator.genRandomCiphertextVector(N_e,
				n);
		final GroupVector<GqElement, GqGroup> preVoteCastReturnCodes = gqGroupGenerator.genRandomGqElementVector(N_e);

		genCMTableInput = new GenCMTableInput(setupSecretKey, encryptedPreChoiceReturnCodes, preVoteCastReturnCodes);
	}

	@Test
	@DisplayName("any null parameter throws NullPointerException")
	void anyNullParamThrows() {
		assertThrows(NullPointerException.class, () -> genCMTableAlgorithm.genCMTable(null, genCMTableInput));
		assertThrows(NullPointerException.class, () -> genCMTableAlgorithm.genCMTable(genCMTableContext, null));
	}

	@Test
	@DisplayName("inconsistent group throws IllegalArgumentException")
	void inconsistentGroupThrows() {
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> otherEncryptedPreChoiceReturnCodes = otherGroupElGamalGenerator.genRandomCiphertextVector(
				genCMTableInput.encryptedPreChoiceReturnCodes().size(), genCMTableInput.encryptedPreChoiceReturnCodes().getElementSize());
		final GroupVector<GqElement, GqGroup> otherPreVoteCastReturnCodes = otherGqGroupGenerator.genRandomGqElementVector(
				genCMTableInput.preVoteCastReturnCodes().size());
		final GenCMTableInput otherGenCMTableInput = new GenCMTableInput(genCMTableInput.setupSecretKey(), otherEncryptedPreChoiceReturnCodes,
				otherPreVoteCastReturnCodes);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genCMTableAlgorithm.genCMTable(genCMTableContext, otherGenCMTableInput));
		assertEquals("The context and input must have the same group.", exception.getMessage());
	}

	@Test
	@DisplayName("too many pre-Choice Return Codes throws IllegalArgumentException")
	void tooManyChoiceReturnCodesThrows() {
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> otherEncryptedPreChoiceReturnCodes = elGamalGenerator.genRandomCiphertextVector(
				genCMTableInput.encryptedPreChoiceReturnCodes().size(), genCMTableInput.encryptedPreChoiceReturnCodes().getElementSize() + 1);
		final GenCMTableInput otherGenCMTableInput = new GenCMTableInput(genCMTableInput.setupSecretKey(), otherEncryptedPreChoiceReturnCodes,
				genCMTableInput.preVoteCastReturnCodes());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genCMTableAlgorithm.genCMTable(genCMTableContext, otherGenCMTableInput));

		final String expected = String.format(
				"The size of the encrypted pre-Choice Return Code must be equal to the number of voting options. [n: %s]",
				genCMTableContext.getCorrectnessInformation().size());
		assertEquals(expected, exception.getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParamsDoesNotThrow() {
		assertDoesNotThrow(() -> genCMTableAlgorithm.genCMTable(genCMTableContext, genCMTableInput));
	}

	@Test
	@DisplayName("valid parameters correctly orders the CMTable by keys")
	void genCMTableOrdersCMTable() {
		final ImmutableMap<String, String> cmTable = genCMTableAlgorithm.genCMTable(genCMTableContext, genCMTableInput).returnCodesMappingTable();
		final Set<Map.Entry<String, String>> cmTableEntrySet = cmTable.asMap().entrySet();
		final ImmutableList<String> cmTableKeys = cmTableEntrySet.stream().map(Map.Entry::getKey).collect(toImmutableList());
		final ImmutableList<String> sortedCmTableKeys = cmTableEntrySet.stream().map(Map.Entry::getKey).sorted().collect(toImmutableList());

		assertEquals(sortedCmTableKeys, cmTableKeys);
	}
}
