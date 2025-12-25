/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToString;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;

@SuppressWarnings("java:S3008")
@DisplayName("A GenKeysCCROutput built with")
class GenKeysCCROutputTest {

	private static final Random random = RandomFactory.createRandom();
	private static final int PSI_SUP = MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
	private static final int PSI_MAX = random.genRandomInteger(PSI_SUP - 2) + 1;
	private static final int NODE_ID = 1;

	private static GqGroup encryptionGroup;
	private static ZeroKnowledgeProof zeroKnowledgeProof;
	private static ZqElement generationSecretKey;
	private static ElGamalMultiRecipientKeyPair keyPair;
	private static GroupVector<SchnorrProof, ZqGroup> schnorrProofs;
	private static AuxiliaryInformation i_aux;

	@BeforeAll
	@SuppressWarnings("java:S117")
	static void setUpAll() {
		zeroKnowledgeProof = ZeroKnowledgeProofFactory.createZeroKnowledgeProof();
		encryptionGroup = GroupTestData.getLargeGqGroup();
		keyPair = ElGamalMultiRecipientKeyPair.genKeyPair(encryptionGroup, PSI_MAX, random);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		i_aux = AuxiliaryInformation.of(electionEventId, "GenKeyCCR", integerToString(NODE_ID));

		schnorrProofs = IntStream.range(0, keyPair.size())
				.mapToObj(i -> {
					final ZqElement EL_sk_j_i = keyPair.getPrivateKey().get(i);
					final GqElement EL_pk_j_i = keyPair.getPublicKey().get(i);
					return zeroKnowledgeProof.genSchnorrProof(EL_sk_j_i, EL_pk_j_i, i_aux);
				}).collect(toGroupVector());

		generationSecretKey = new ZqGroupGenerator(ZqGroup.sameOrderAs(encryptionGroup)).genRandomZqElementMember();
	}

	@Test
	@DisplayName("valid parameters gives expected output")
	void expectOutput() {
		final GenKeysCCROutput genKeysCCROutput = new GenKeysCCROutput(keyPair, generationSecretKey, schnorrProofs);

		final GqGroup keyPairGroup = genKeysCCROutput.ccrjChoiceReturnCodesEncryptionKeyPair().getGroup();
		final ZqGroup generationKeyGroup = genKeysCCROutput.ccrjReturnCodesGenerationSecretKey().getGroup();
		assertTrue(keyPairGroup.hasSameOrderAs(generationKeyGroup));
	}

	@Test
	@DisplayName("any null parameter throws NullPointerException")
	void nullParamThrows() {
		assertThrows(NullPointerException.class, () -> new GenKeysCCROutput(null, generationSecretKey, schnorrProofs));
		assertThrows(NullPointerException.class, () -> new GenKeysCCROutput(keyPair, null, schnorrProofs));
		assertThrows(NullPointerException.class, () -> new GenKeysCCROutput(keyPair, generationSecretKey, null));
	}

	@Test
	@DisplayName("different size keypair and Schnorr proofs throws IllegalArgumentException")
	void differentSizeKeyPairAndSchnorrProofsThrows() {
		final ElGamalMultiRecipientKeyPair differentSize = ElGamalMultiRecipientKeyPair.genKeyPair(encryptionGroup, PSI_MAX + 1, random);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenKeysCCROutput(differentSize, generationSecretKey, schnorrProofs));
		final String message = "The size of the ccrj Choice Return Codes encryption key pair must be equal to the number of Schnorr proofs.";
		assertEquals(message, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@SuppressWarnings("java:S117")
	@DisplayName("wrong size keypair throws IllegalArgumentException")
	void wrongSizeKeyPairThrows() {
		final ElGamalMultiRecipientKeyPair wrongSizeKeyPair = ElGamalMultiRecipientKeyPair.genKeyPair(encryptionGroup, PSI_SUP + 1, random);
		final GroupVector<SchnorrProof, ZqGroup> wrongSizeSchnorrProofs = IntStream.range(0, PSI_SUP + 1)
				.mapToObj(i -> {
					final ZqElement EL_sk_j_i = wrongSizeKeyPair.getPrivateKey().get(i);
					final GqElement EL_pk_j_i = wrongSizeKeyPair.getPublicKey().get(i);
					return zeroKnowledgeProof.genSchnorrProof(EL_sk_j_i, EL_pk_j_i, i_aux);
				}).collect(toGroupVector());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenKeysCCROutput(wrongSizeKeyPair, generationSecretKey, wrongSizeSchnorrProofs));
		final String message = String.format(
				"The ccrj Choice Return Codes encryption key pair must be of size smaller or equal to the maximum supported number of selections. [psi_max: %s, psi_sup: %s]",
				PSI_SUP + 1,
				PSI_SUP);
		assertEquals(message, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("wrong group generation secret key throws IllegalArgumentException")
	void wrongGroupGenerationSecretKeyThrows() {
		final GqGroup otherGroup = GroupTestData.getDifferentGqGroup(encryptionGroup);
		final ZqElement otherGenerationKey = new ZqGroupGenerator(ZqGroup.sameOrderAs(otherGroup)).genRandomZqElementMember();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenKeysCCROutput(keyPair, otherGenerationKey, schnorrProofs));
		assertEquals("The ccrj Return Codes generation secret key must have the same order than the ccr Choice Return Codes encryption key pair.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("wrong group Schnorr proofs throws IllegalArgumentException")
	void wrongGroupSchnorrProofs() {
		final GqGroup otherGroup = GroupTestData.getDifferentGqGroup(encryptionGroup);
		final ElGamalMultiRecipientKeyPair otherKeyPair = ElGamalMultiRecipientKeyPair.genKeyPair(otherGroup, PSI_MAX, random);
		final ZqElement otherGenerationKey = new ZqGroupGenerator(ZqGroup.sameOrderAs(otherGroup)).genRandomZqElementMember();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenKeysCCROutput(otherKeyPair, otherGenerationKey, schnorrProofs));
		assertEquals("The Schnorr proofs must have the same group order as the ccr Choice Return Codes encryption key pair.",
				Throwables.getRootCause(exception).getMessage());
	}

}
