/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.PlaintextEqualityProof;

@DisplayName("VerifyBallotCCRInput with")
class VerifyBallotCCRInputTest extends TestGroupSetup {

	private static final int PSI = 5;

	private ElGamalMultiRecipientCiphertext encryptedVote;
	private ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote;
	private ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes;
	private ExponentiationProof exponentiationProof;
	private PlaintextEqualityProof plaintextEqualityProof;

	@BeforeEach
	@SuppressWarnings("java:S117")
	void setUp() {
		encryptedVote = elGamalGenerator.genRandomCiphertext(1);
		final ZqElement k_id = zqGroupGenerator.genRandomZqElementMember();
		exponentiatedEncryptedVote = encryptedVote.getCiphertextExponentiation(k_id);
		encryptedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(PSI);
		exponentiationProof = new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember());
		plaintextEqualityProof = new PlaintextEqualityProof(zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementVector(2));
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParams() {
		final VerifyBallotCCRInput.Builder builder = new VerifyBallotCCRInput.Builder()
				.setEncryptedVote(encryptedVote)
				.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
				.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes)
				.setExponentiationProof(exponentiationProof)
				.setPlaintextEqualityProof(plaintextEqualityProof);

		assertDoesNotThrow(builder::build);
	}

	@Test
	@DisplayName("different group order exponentiation proof throws IllegalArgumentException")
	void differentOrderExponentiationProof() {
		final ExponentiationProof otherExponentiationProof = new ExponentiationProof(otherZqGroupGenerator.genRandomZqElementMember(),
				otherZqGroupGenerator.genRandomZqElementMember());
		final VerifyBallotCCRInput.Builder shortBuilder = new VerifyBallotCCRInput.Builder()
				.setEncryptedVote(encryptedVote)
				.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
				.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes)
				.setExponentiationProof(otherExponentiationProof)
				.setPlaintextEqualityProof(plaintextEqualityProof);

		final IllegalArgumentException shortException = assertThrows(IllegalArgumentException.class, shortBuilder::build);
		assertEquals("The exponentiation proof must have the same group order than the other inputs.",
				Throwables.getRootCause(shortException).getMessage());
	}

	@Test
	@DisplayName("different group order plaintext equality proof throws IllegalArgumentException")
	void differentOrderPlaintextEqualityProof() {
		final PlaintextEqualityProof otherPlaintextEqualityProof = new PlaintextEqualityProof(otherZqGroupGenerator.genRandomZqElementMember(),
				otherZqGroupGenerator.genRandomZqElementVector(2));
		final VerifyBallotCCRInput.Builder shortBuilder = new VerifyBallotCCRInput.Builder()
				.setEncryptedVote(encryptedVote)
				.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
				.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes)
				.setExponentiationProof(exponentiationProof)
				.setPlaintextEqualityProof(otherPlaintextEqualityProof);

		final IllegalArgumentException shortException = assertThrows(IllegalArgumentException.class, shortBuilder::build);
		assertEquals("The plaintext equality proof must have the same group order than the other inputs.",
				Throwables.getRootCause(shortException).getMessage());
	}

}
