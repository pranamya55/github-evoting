/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToString;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyCCSchnorrProofsAlgorithm;

/**
 * Tests of GenVerCardSetKeysAlgorithm.
 */
@DisplayName("A GenVerCardSetKeysAlgorithm calling genVerCardSetKeys with")
class GenVerCardSetKeysAlgorithmTest {

	private static final int NODES_NUMBER = ControlComponentNode.ids().size();
	private static final int PSI_SUP = MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
	private static final int PSI_INF = 2;
	private static final Random random = RandomFactory.createRandom();
	private static final int PSI_MAX = random.genRandomInteger(PSI_SUP - 1) + PSI_INF;
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();

	private static ElGamalGenerator elGamalGenerator;
	private static ZeroKnowledgeProof zeroKnowledgeProof;
	private static GqGroup gqGroup;
	private static ImmutableList<ElGamalMultiRecipientKeyPair> ccrKeyPairs;
	private static GenVerCardSetKeysAlgorithm genVerCardSetKeysAlgorithm;
	private static GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccrPublicKeys;
	private static GroupVector<GroupVector<SchnorrProof, ZqGroup>, ZqGroup> ccrSchnorrProofs;
	private static GenVerCardSetKeysContext context;
	private static GenVerCardSetKeysInput input;

	@BeforeAll
	@SuppressWarnings("java:S117")
	static void setup() {
		gqGroup = GroupTestData.getLargeGqGroup();
		elGamalGenerator = new ElGamalGenerator(gqGroup);
		zeroKnowledgeProof = ZeroKnowledgeProofFactory.createZeroKnowledgeProof();

		ccrKeyPairs = genKeyPairs(PSI_MAX);

		ccrPublicKeys = ccrKeyPairs.stream()
				.map(ElGamalMultiRecipientKeyPair::getPublicKey)
				.collect(toGroupVector());

		ccrSchnorrProofs = IntStream.range(0, ccrKeyPairs.size())
				.mapToObj(j -> {
					final ElGamalMultiRecipientKeyPair CCRKeyPair = ccrKeyPairs.get(j);
					final AuxiliaryInformation i_aux = AuxiliaryInformation.of(ELECTION_EVENT_ID, "GenKeysCCR", integerToString(j + 1));
					return IntStream.range(0, CCRKeyPair.size())
							.parallel()
							.mapToObj(i -> {
								final ZqElement EL_sk_j_i = CCRKeyPair.getPrivateKey().get(i);
								final GqElement EL_pk_j_i = CCRKeyPair.getPublicKey().get(i);
								return zeroKnowledgeProof.genSchnorrProof(EL_sk_j_i, EL_pk_j_i, i_aux);
							}).collect(toGroupVector());
				})
				.collect(toGroupVector());

		context = new GenVerCardSetKeysContext(gqGroup, ELECTION_EVENT_ID, PSI_MAX);
		input = new GenVerCardSetKeysInput(ccrPublicKeys, ccrSchnorrProofs);

		genVerCardSetKeysAlgorithm = new GenVerCardSetKeysAlgorithm(ElGamalFactory.createElGamal(),
				new VerifyCCSchnorrProofsAlgorithm(ZeroKnowledgeProofFactory.createZeroKnowledgeProof()));
	}

	@Test
	@DisplayName("valid parameter does not throw")
	void validParamDoesNotThrow() {
		assertDoesNotThrow(
				() -> genVerCardSetKeysAlgorithm.genVerCardSetKeys(context, input));
	}

	@Test
	@DisplayName("invalid election event id throws FailedValidationException")
	void invalidElectionEventId() {
		assertThrows(FailedValidationException.class, () -> new GenVerCardSetKeysContext(gqGroup, "123", PSI_MAX));
	}

	@Test
	@DisplayName("negative maximum number of selections throws IllegalArgumentException")
	void negativeMaximumNumberOfSelectionsThrows() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenVerCardSetKeysContext(gqGroup, ELECTION_EVENT_ID, -2));
		final String message = String.format("The maximum number of selections must be strictly positive. [psi_max: %s]", -2);
		assertEquals(message, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("null maximum number of selections throws IllegalArgumentException")
	void nullMaximumNumberOfSelectionsThrows() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenVerCardSetKeysContext(gqGroup, ELECTION_EVENT_ID, 0));
		final String message = String.format("The maximum number of selections must be strictly positive. [psi_max: %s]", 0);
		assertEquals(message, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("too big maximum number of selections throws IllegalArgumentException")
	void tooBigMaximumNumberOfSelectionsThrows() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenVerCardSetKeysContext(gqGroup, ELECTION_EVENT_ID, PSI_SUP + 1));
		final String message = String.format(
				"The maximum number of selections must be smaller or equal to the maximum supported number of selections. [psi_max: %s, psi_sup: %s]",
				PSI_SUP + 1, PSI_SUP);
		assertEquals(message, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("null argument throws a NullPointerException")
	void testGenVerCardSetKeysWithNullArgumentThrows() {
		assertThrows(NullPointerException.class,
				() -> genVerCardSetKeysAlgorithm.genVerCardSetKeys(context, null));
		assertThrows(NullPointerException.class,
				() -> genVerCardSetKeysAlgorithm.genVerCardSetKeys(null, input));
	}

	@Test
	@DisplayName("a too short Choice Return Codes encryption public key vector throws")
	void testGenVerCardSetKeysWithTooShortChoiceReturnCodesEncryptionPublicKeysVectorThrows() {
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> tooFewPublicKeys = ccrPublicKeys.subVector(0, NODES_NUMBER - 1);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenVerCardSetKeysInput(tooFewPublicKeys, ccrSchnorrProofs));
		final String errorMessage = "There must be as many Schnorr proofs as CCR election public keys.";

		assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("a too long Choice Return Codes encryption public key vector throws")
	void testGenVerCardSetKeysWithTooLongChoiceReturnCodesEncryptionPublicKeysVectorThrows() {
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> tooManyPublicKeys = ccrPublicKeys.append(ccrPublicKeys.getFirst());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenVerCardSetKeysInput(tooManyPublicKeys, ccrSchnorrProofs));
		final String errorMessage = "There must be as many Schnorr proofs as CCR election public keys.";

		assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("too few Schnorr proofs throws")
	void tooFewSchnorrProofs() {
		final GroupVector<GroupVector<SchnorrProof, ZqGroup>, ZqGroup> tooFewProofs = ccrSchnorrProofs.subVector(0, NODES_NUMBER - 1);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenVerCardSetKeysInput(ccrPublicKeys, tooFewProofs));
		final String errorMessage = "There must be as many Schnorr proofs as CCR election public keys.";

		assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("a too few Choice Return Codes encryption public key elements throws")
	void testGenVerCardSetKeysWithTooFewKeyElementsThrows() {
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> tooFewElementsPublicKeys = genKeyPairs(PSI_INF - 1).stream()
				.map(ElGamalMultiRecipientKeyPair::getPublicKey)
				.collect(toGroupVector());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenVerCardSetKeysInput(tooFewElementsPublicKeys, ccrSchnorrProofs));

		assertEquals("The size of the CCR Choice Return Codes encryption keys must be equal to the size of the Schnorr proofs.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("a too many Choice Return Codes encryption public key elements throws")
	void testGenVerCardSetKeysWithTooManyKeyElementsThrows() {
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> tooManyElementsPublicKeys = genKeyPairs(PSI_SUP + 1).stream()
				.map(ElGamalMultiRecipientKeyPair::getPublicKey)
				.collect(toGroupVector());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenVerCardSetKeysInput(tooManyElementsPublicKeys, ccrSchnorrProofs));

		assertEquals("The size of the CCR Choice Return Codes encryption keys must be equal to the size of the Schnorr proofs.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@SuppressWarnings("java:S117")
	@DisplayName("Schnorr proofs with too few elements throws")
	void schnorrProofsTooFewElements() {
		final GroupVector<GroupVector<SchnorrProof, ZqGroup>, ZqGroup> tooFewElementsProofs = IntStream.range(0, ccrKeyPairs.size())
				.mapToObj(j -> {
					final ElGamalMultiRecipientKeyPair CCRKeyPair = ccrKeyPairs.get(j);
					final AuxiliaryInformation i_aux = AuxiliaryInformation.of(ELECTION_EVENT_ID, "GenKeysCCR", integerToString(j));
					return IntStream.range(0, PSI_INF - 1)
							.parallel()
							.mapToObj(i -> {
								final ZqElement EL_sk_j_i = CCRKeyPair.getPrivateKey().get(i);
								final GqElement EL_pk_j_i = CCRKeyPair.getPublicKey().get(i);
								return zeroKnowledgeProof.genSchnorrProof(EL_sk_j_i, EL_pk_j_i, i_aux);
							}).collect(toGroupVector());
				})
				.collect(toGroupVector());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenVerCardSetKeysInput(ccrPublicKeys, tooFewElementsProofs));

		assertEquals("The size of the CCR Choice Return Codes encryption keys must be equal to the size of the Schnorr proofs.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("public keys and proofs of different groups throws")
	void differentGroupsKeysAndProofs() {
		final GenVerCardSetKeysInput otherPublicKeys = new GenVerCardSetKeysInput(spy(ccrPublicKeys), ccrSchnorrProofs);
		when(otherPublicKeys.ccrChoiceReturnCodesEncryptionPublicKeys().getGroup()).thenReturn(GroupTestData.getGqGroup());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genVerCardSetKeysAlgorithm.genVerCardSetKeys(context, otherPublicKeys));
		final String expectedErrorMessage = "The context and input must have the same encryption group.";

		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception).getMessage());
	}

	private static ImmutableList<ElGamalMultiRecipientKeyPair> genKeyPairs(final int keyElementsSize) {
		return IntStream.range(0, NODES_NUMBER)
				.mapToObj(i -> elGamalGenerator.genRandomKeyPair(keyElementsSize))
				.collect(toImmutableList());
	}

}
