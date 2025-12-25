/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;

@SuppressWarnings("java:S116")
@DisplayName("a GenEncLongCodeSharesOutput built with")
class GenEncLongCodeSharesOutputTest extends TestGroupSetup {

	private static int ciphertextSize;
	private final int SIZE_10 = 10;
	private final GqGroup generator = GroupTestData.getDifferentGqGroup(gqGroup);
	private final GqGroup generator2 = GroupTestData.getDifferentGqGroup(gqGroup);

	private final GroupVector<GqElement, GqGroup> K_j_id = Stream.generate(generator::getGenerator).limit(SIZE_10)
			.collect(GroupVector.toGroupVector());
	private final GroupVector<GqElement, GqGroup> Kc_j_id = Stream.generate(generator2::getGenerator).limit(SIZE_10)
			.collect(GroupVector.toGroupVector());
	private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_expPCC_j_id = elGamalGenerator.genRandomCiphertextVector(10,
			ciphertextSize);
	private final GroupVector<ExponentiationProof, ZqGroup> pi_expPCC_j_id =
			Stream.generate(
							() -> new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
					.limit(SIZE_10)
					.collect(GroupVector.toGroupVector());

	private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_expCK_j_id = elGamalGenerator.genRandomCiphertextVector(10,
			ciphertextSize);
	private final GroupVector<ExponentiationProof, ZqGroup> pi_expCK_j_id =
			Stream.generate(
							() -> new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
					.limit(SIZE_10)
					.collect(GroupVector.toGroupVector());

	@BeforeAll
	static void setupAll() {
		ciphertextSize = new SecureRandom().nextInt(5) + 1;
	}

	@Test
	@DisplayName("valid parameters gives expected output")
	void validParameters() {
		final GenEncLongCodeSharesOutput output =
				new GenEncLongCodeSharesOutput.Builder()
						.setVoterChoiceReturnCodeGenerationPublicKeys(K_j_id)
						.setVoterVoteCastReturnCodeGenerationPublicKeys(Kc_j_id)
						.setExponentiatedEncryptedHashedPartialChoiceReturnCodes(c_expPCC_j_id)
						.setProofsCorrectExponentiationPartialChoiceReturnCodes(pi_expPCC_j_id)
						.setExponentiatedEncryptedHashedConfirmationKeys(c_expCK_j_id)
						.setProofsCorrectExponentiationConfirmationKeys(pi_expCK_j_id)
						.build();

		final GqGroup confirmationKeysGroup = output.getExponentiatedEncryptedHashedConfirmationKeys().getGroup();
		final GqGroup partialChoiceReturnCodesGroup = output.getExponentiatedEncryptedHashedPartialChoiceReturnCodes().getGroup();

		assertTrue(confirmationKeysGroup.hasSameOrderAs(partialChoiceReturnCodesGroup));
	}

	@Test
	@DisplayName("any null parameter throws NullPointerException")
	void anyNullParameter() {
		GenEncLongCodeSharesOutput.Builder builder = new GenEncLongCodeSharesOutput.Builder()
				.setVoterChoiceReturnCodeGenerationPublicKeys(K_j_id)
				.setVoterVoteCastReturnCodeGenerationPublicKeys(Kc_j_id)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodes(c_expPCC_j_id)
				.setProofsCorrectExponentiationPartialChoiceReturnCodes(pi_expPCC_j_id)
				.setExponentiatedEncryptedHashedConfirmationKeys(c_expCK_j_id)
				.setProofsCorrectExponentiationConfirmationKeys(pi_expCK_j_id);

		builder = builder.setVoterChoiceReturnCodeGenerationPublicKeys(null);
		Exception exception = assertThrows(NullPointerException.class, builder::build);

		assertEquals("The Vector of Voter Choice Return Code Generation public keys is null.", Throwables.getRootCause(exception).getMessage());

		builder = builder.setVoterChoiceReturnCodeGenerationPublicKeys(K_j_id)
				.setVoterVoteCastReturnCodeGenerationPublicKeys(null);
		exception = assertThrows(NullPointerException.class, builder::build);

		assertEquals("The Vector of Voter Vote Cast Return Code Generation public keys is null.",
				Throwables.getRootCause(exception).getMessage());

		builder = builder.setVoterVoteCastReturnCodeGenerationPublicKeys(Kc_j_id)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodes(null);
		exception = assertThrows(NullPointerException.class, builder::build);

		assertEquals("The Vector of exponentiated, encrypted, hashed partial Choice Return Codes is null.",
				Throwables.getRootCause(exception).getMessage());

		builder = builder.setExponentiatedEncryptedHashedPartialChoiceReturnCodes(c_expPCC_j_id)
				.setProofsCorrectExponentiationPartialChoiceReturnCodes(null);
		exception = assertThrows(NullPointerException.class, builder::build);

		assertEquals("The Proofs of correct exponentiation of the partial Choice Return Codes is null.",
				Throwables.getRootCause(exception).getMessage());

		builder = builder.setProofsCorrectExponentiationPartialChoiceReturnCodes(pi_expPCC_j_id)
				.setExponentiatedEncryptedHashedConfirmationKeys(null);
		exception = assertThrows(NullPointerException.class, builder::build);

		assertEquals("The Vector of exponentiated, encrypted, hashed Confirmation Keys is null.",
				Throwables.getRootCause(exception).getMessage());

		builder = builder.setExponentiatedEncryptedHashedConfirmationKeys(c_expCK_j_id)
				.setProofsCorrectExponentiationConfirmationKeys(null);
		exception = assertThrows(NullPointerException.class, builder::build);

		assertEquals("The Proofs of correct exponentiation of the Confirmation Keys is null.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("empty vectors throws IllegalArgumentException")
	void emptyVectors() {
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> emptyVector = GroupVector.of();

		final GroupVector<GqElement, GqGroup> emptyGqElementList = GroupVector.of();
		GenEncLongCodeSharesOutput.Builder builder = new GenEncLongCodeSharesOutput.Builder()
				.setVoterChoiceReturnCodeGenerationPublicKeys(K_j_id)
				.setVoterVoteCastReturnCodeGenerationPublicKeys(Kc_j_id)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodes(c_expPCC_j_id)
				.setProofsCorrectExponentiationPartialChoiceReturnCodes(pi_expPCC_j_id)
				.setExponentiatedEncryptedHashedConfirmationKeys(c_expCK_j_id)
				.setProofsCorrectExponentiationConfirmationKeys(pi_expCK_j_id);

		builder = builder.setVoterChoiceReturnCodeGenerationPublicKeys(emptyGqElementList);
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);

		assertEquals("The Vector of Voter Choice Return Code Generation public keys must have more than zero elements.",
				Throwables.getRootCause(exception).getMessage());

		builder = builder.setVoterChoiceReturnCodeGenerationPublicKeys(K_j_id).setVoterVoteCastReturnCodeGenerationPublicKeys(emptyGqElementList);
		exception = assertThrows(IllegalArgumentException.class, builder::build);

		assertEquals("The Vector of Voter Vote Cast Return Code Generation public keys must have more than zero elements.",
				Throwables.getRootCause(exception).getMessage());

		builder = builder.setVoterVoteCastReturnCodeGenerationPublicKeys(Kc_j_id)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodes(emptyVector);
		exception = assertThrows(IllegalArgumentException.class, builder::build);

		assertEquals("The Vector of exponentiated, encrypted, hashed partial Choice Return Codes must have more than zero elements.",
				Throwables.getRootCause(exception).getMessage());

		final GroupVector<ExponentiationProof, ZqGroup> emptyExponentiationProofList = GroupVector.of();

		builder = builder.setExponentiatedEncryptedHashedPartialChoiceReturnCodes(c_expPCC_j_id)
				.setProofsCorrectExponentiationPartialChoiceReturnCodes(emptyExponentiationProofList);
		exception = assertThrows(IllegalArgumentException.class, builder::build);

		assertEquals("The Proofs of correct exponentiation of the partial Choice Return Codes must have more than zero elements.",
				Throwables.getRootCause(exception).getMessage());

		builder = builder.setProofsCorrectExponentiationPartialChoiceReturnCodes(pi_expPCC_j_id)
				.setExponentiatedEncryptedHashedConfirmationKeys(emptyVector);
		exception = assertThrows(IllegalArgumentException.class, builder::build);

		assertEquals("The Vector of exponentiated, encrypted, hashed Confirmation Keys must have more than zero elements.",
				Throwables.getRootCause(exception).getMessage());

		builder = builder.setExponentiatedEncryptedHashedConfirmationKeys(c_expCK_j_id)
				.setProofsCorrectExponentiationConfirmationKeys(emptyExponentiationProofList);
		exception = assertThrows(IllegalArgumentException.class, builder::build);

		assertEquals("The Proofs of correct exponentiation of the Confirmation Keys must have more than zero elements.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@SuppressWarnings("java:S117")
	@DisplayName("inconsistentVectorsSize throws IllegalArgumentException")
	void inconsistentVectorsSize() {
		final GroupVector<GqElement, GqGroup> Kc_j_id_1 = Stream.generate(generator2::getGenerator)
				.limit(SIZE_10 + 1)
				.collect(GroupVector.toGroupVector());
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_expPCC_j_id_1 = elGamalGenerator.genRandomCiphertextVector(SIZE_10 + 1,
				ciphertextSize);
		final GroupVector<ExponentiationProof, ZqGroup> pi_expPCC_j_id_1 =
				Stream.generate(
								() -> new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
						.limit(SIZE_10 + 1)
						.collect(GroupVector.toGroupVector());

		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_expCK_j_id_1 = elGamalGenerator.genRandomCiphertextVector(SIZE_10 + 1,
				ciphertextSize);
		final GroupVector<ExponentiationProof, ZqGroup> pi_expCK_j_id_1 =
				Stream.generate(
								() -> new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
						.limit(SIZE_10 + 1)
						.collect(GroupVector.toGroupVector());

		final int N_E = K_j_id.size();
		GenEncLongCodeSharesOutput.Builder builder = new GenEncLongCodeSharesOutput.Builder()
				.setVoterChoiceReturnCodeGenerationPublicKeys(K_j_id)
				.setVoterVoteCastReturnCodeGenerationPublicKeys(Kc_j_id)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodes(c_expPCC_j_id)
				.setProofsCorrectExponentiationPartialChoiceReturnCodes(pi_expPCC_j_id)
				.setExponentiatedEncryptedHashedConfirmationKeys(c_expCK_j_id)
				.setProofsCorrectExponentiationConfirmationKeys(pi_expCK_j_id);

		builder = builder.setVoterVoteCastReturnCodeGenerationPublicKeys(Kc_j_id_1);
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);

		assertEquals(String.format("The Vector of Voter Vote Cast Return Code Generation public keys is of incorrect size "
								+ "[size: expected: %s, actual: %s].",
						N_E, Kc_j_id_1.size()),
				Throwables.getRootCause(exception).getMessage());

		builder = builder.setVoterVoteCastReturnCodeGenerationPublicKeys(Kc_j_id)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodes(c_expPCC_j_id_1);
		exception = assertThrows(IllegalArgumentException.class, builder::build);

		assertEquals(String.format("The Vector of exponentiated, encrypted, hashed partial Choice Return Codes is of incorrect size "
						+ "[size: expected: %s, actual: %s].", N_E, c_expPCC_j_id_1.size()),
				Throwables.getRootCause(exception).getMessage());

		builder = builder.setExponentiatedEncryptedHashedPartialChoiceReturnCodes(c_expPCC_j_id)
				.setProofsCorrectExponentiationPartialChoiceReturnCodes(pi_expPCC_j_id_1);
		exception = assertThrows(IllegalArgumentException.class, builder::build);

		assertEquals(String.format("The Proofs of correct exponentiation of the partial Choice Return Codes is of incorrect size "
						+ "[size: expected: %s, actual: %s].", N_E, c_expPCC_j_id_1.size()),
				Throwables.getRootCause(exception).getMessage());

		builder = builder.setProofsCorrectExponentiationPartialChoiceReturnCodes(pi_expPCC_j_id)
				.setExponentiatedEncryptedHashedConfirmationKeys(c_expCK_j_id_1);
		exception = assertThrows(IllegalArgumentException.class, builder::build);

		assertEquals(String.format("The Vector of exponentiated, encrypted, hashed Confirmation Keys is of incorrect size "
						+ "[size: expected: %s, actual: %s].", N_E, c_expPCC_j_id_1.size()),
				Throwables.getRootCause(exception).getMessage());

		builder = builder.setExponentiatedEncryptedHashedConfirmationKeys(c_expCK_j_id)
				.setProofsCorrectExponentiationConfirmationKeys(pi_expCK_j_id_1);
		exception = assertThrows(IllegalArgumentException.class, builder::build);

		assertEquals(String.format("The Proofs of correct exponentiation of the Confirmation Keys is of incorrect size "
						+ "[size: expected: %s, actual: %s].", N_E, c_expPCC_j_id_1.size()),
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@SuppressWarnings("java:S117")
	@DisplayName("different group orders throws IllegalArgumentException")
	void differentGroupOrders() {
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_expPCC_j_id_q1 = otherGroupElGamalGenerator.genRandomCiphertextVector(
				SIZE_10, ciphertextSize);
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_expCK_j_id_q1 = otherGroupElGamalGenerator.genRandomCiphertextVector(
				SIZE_10, ciphertextSize);

		GenEncLongCodeSharesOutput.Builder builder = new GenEncLongCodeSharesOutput.Builder()
				.setVoterChoiceReturnCodeGenerationPublicKeys(K_j_id)
				.setVoterVoteCastReturnCodeGenerationPublicKeys(Kc_j_id)
				.setExponentiatedEncryptedHashedPartialChoiceReturnCodes(c_expPCC_j_id)
				.setProofsCorrectExponentiationPartialChoiceReturnCodes(pi_expPCC_j_id)
				.setExponentiatedEncryptedHashedConfirmationKeys(c_expCK_j_id)
				.setProofsCorrectExponentiationConfirmationKeys(pi_expCK_j_id);

		builder = builder.setExponentiatedEncryptedHashedPartialChoiceReturnCodes(c_expPCC_j_id_q1);
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);

		assertEquals("The Vector of exponentiated, encrypted, hashed partial Choice Return Codes and the Vector of exponentiated, encrypted, "
						+ "hashed Confirmation Keys do not have the same group order.",
				Throwables.getRootCause(exception).getMessage());

		builder = builder.setExponentiatedEncryptedHashedPartialChoiceReturnCodes(c_expPCC_j_id)
				.setExponentiatedEncryptedHashedConfirmationKeys(c_expCK_j_id_q1);
		exception = assertThrows(IllegalArgumentException.class, builder::build);

		assertEquals("The Vector of exponentiated, encrypted, hashed partial Choice Return Codes and the Vector of exponentiated, encrypted, "
						+ "hashed Confirmation Keys do not have the same group order.",
				Throwables.getRootCause(exception).getMessage());
	}
}
