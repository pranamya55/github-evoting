/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("DecryptPPCInput with")
class DecryptPCCInputTest extends TestGroupSetup {

	private static final int PSI_SUP = MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
	private static final int PSI = 5;
	private static final int NUM_OTHER_NODES = 3;

	private GroupVector<GqElement, GqGroup> exponentiatedGammaElements;
	private GroupVector<GroupVector<GqElement, GqGroup>, GqGroup> otherCcrExponentiatedGammaElements;
	private GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> otherCcrExponentiationProofs;
	private ElGamalMultiRecipientCiphertext encryptedVote;
	private ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote;
	private ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes;

	@BeforeEach
	void setup() {
		exponentiatedGammaElements = gqGroupGenerator.genRandomGqElementVector(PSI);
		otherCcrExponentiatedGammaElements = gqGroupGenerator.genRandomGqElementMatrix(PSI, NUM_OTHER_NODES).columnStream()
				.collect(GroupVector.toGroupVector());
		otherCcrExponentiationProofs = GroupVector.of(
				genRandomExponentiationProofVector(PSI),
				genRandomExponentiationProofVector(PSI),
				genRandomExponentiationProofVector(PSI)
		);
		encryptedVote = elGamalGenerator.genRandomCiphertext(1);
		exponentiatedEncryptedVote = elGamalGenerator.genRandomCiphertext(1);
		encryptedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(PSI);
	}

	private Stream<Arguments> nullArgumentsProvider() {
		return Stream.of(
				Arguments.of(null, otherCcrExponentiatedGammaElements, otherCcrExponentiationProofs,
						encryptedVote, exponentiatedEncryptedVote, encryptedPartialChoiceReturnCodes),
				Arguments.of(exponentiatedGammaElements, null, otherCcrExponentiationProofs,
						encryptedVote, exponentiatedEncryptedVote, encryptedPartialChoiceReturnCodes),
				Arguments.of(exponentiatedGammaElements, otherCcrExponentiatedGammaElements, null,
						encryptedVote, exponentiatedEncryptedVote, encryptedPartialChoiceReturnCodes),
				Arguments.of(exponentiatedGammaElements, otherCcrExponentiatedGammaElements, otherCcrExponentiationProofs,
						null, exponentiatedEncryptedVote, encryptedPartialChoiceReturnCodes),
				Arguments.of(exponentiatedGammaElements, otherCcrExponentiatedGammaElements, otherCcrExponentiationProofs,
						encryptedVote, null, encryptedPartialChoiceReturnCodes),
				Arguments.of(exponentiatedGammaElements, otherCcrExponentiatedGammaElements, otherCcrExponentiationProofs,
						encryptedVote, exponentiatedEncryptedVote, null)
		);
	}

	@ParameterizedTest
	@MethodSource("nullArgumentsProvider")
	@DisplayName("null arguments throws a NullPointerException")
	void buildWithNullObjectsThrows(final GroupVector<GqElement, GqGroup> exponentiatedGammaElements,
			final GroupVector<GroupVector<GqElement, GqGroup>, GqGroup> otherExponentiatedGammaElements,
			final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> otherExponentiationProofs,
			final ElGamalMultiRecipientCiphertext encryptedVote, final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote,
			final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes) {

		final DecryptPCCInput.Builder decryptPCCInputBuilder = new DecryptPCCInput.Builder()
				.setExponentiatedGammaElements(exponentiatedGammaElements)
				.setOtherCcrExponentiatedGammaElements(otherExponentiatedGammaElements)
				.setOtherCcrExponentiationProofs(otherExponentiationProofs)
				.setEncryptedVote(encryptedVote)
				.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
				.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes);

		assertThrows(NullPointerException.class, decryptPCCInputBuilder::build);
	}

	@Test
	@DisplayName("valid arguments does not throw")
	void buildWithValidArgumentsDoesNotThrow() {
		final DecryptPCCInput.Builder decryptPCCInputBuilder = new DecryptPCCInput.Builder()
				.setExponentiatedGammaElements(exponentiatedGammaElements)
				.setOtherCcrExponentiatedGammaElements(otherCcrExponentiatedGammaElements)
				.setOtherCcrExponentiationProofs(otherCcrExponentiationProofs)
				.setEncryptedVote(encryptedVote)
				.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
				.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes);

		assertDoesNotThrow(decryptPCCInputBuilder::build);
	}

	@Test
	@DisplayName("exponentiated gama elements of size different than size of other Ccr exponentiated gamma elements IllegalArgumentException")
	void buildWithExponentiatedGammaElementsSizeNotPhiThrows() {
		final GroupVector<GqElement, GqGroup> tooShortExponentiatedGammaElements = gqGroupGenerator.genRandomGqElementVector(PSI_SUP - 1);

		DecryptPCCInput.Builder decryptPCCInputBuilder = new DecryptPCCInput.Builder()
				.setExponentiatedGammaElements(tooShortExponentiatedGammaElements)
				.setOtherCcrExponentiatedGammaElements(otherCcrExponentiatedGammaElements)
				.setOtherCcrExponentiationProofs(otherCcrExponentiationProofs)
				.setEncryptedVote(encryptedVote)
				.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
				.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, decryptPCCInputBuilder::build);
		assertEquals(
				"The exponentiated gamma elements, the other CCR's exponentiated gamma elements, the other CCR's exponentiation proofs and the encrypted partial Choice Return Codes must have the same size.",
				Throwables.getRootCause(exception).getMessage());

		final GroupVector<GqElement, GqGroup> tooLongExponentiatedGammaElements = gqGroupGenerator.genRandomGqElementVector(PSI_SUP + 1);

		decryptPCCInputBuilder = new DecryptPCCInput.Builder()
				.setExponentiatedGammaElements(tooLongExponentiatedGammaElements)
				.setOtherCcrExponentiatedGammaElements(otherCcrExponentiatedGammaElements)
				.setOtherCcrExponentiationProofs(otherCcrExponentiationProofs)
				.setEncryptedVote(encryptedVote)
				.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
				.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes);

		exception = assertThrows(IllegalArgumentException.class, decryptPCCInputBuilder::build);
		assertEquals(
				"The exponentiated gamma elements, the other CCR's exponentiated gamma elements, the other CCR's exponentiation proofs and the encrypted partial Choice Return Codes must have the same size.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("other exponentiated gamma elements size different 3 throws")
	void buildWithOtherExponentiatedGammaElementsSizeDifferentThreeThrows() {
		final GroupVector<GroupVector<GqElement, GqGroup>, GqGroup> tooSmallOtherExponentiatedGammaElements = otherCcrExponentiatedGammaElements = gqGroupGenerator.genRandomGqElementMatrix(
				PSI, NUM_OTHER_NODES - 1).columnStream().collect(GroupVector.toGroupVector());

		DecryptPCCInput.Builder decryptPCCInputBuilder = new DecryptPCCInput.Builder()
				.setExponentiatedGammaElements(exponentiatedGammaElements)
				.setOtherCcrExponentiatedGammaElements(tooSmallOtherExponentiatedGammaElements)
				.setOtherCcrExponentiationProofs(otherCcrExponentiationProofs)
				.setEncryptedVote(encryptedVote)
				.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
				.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, decryptPCCInputBuilder::build);
		assertEquals("There must be exactly 3 vectors of other CCR's exponentiated gamma elements.",
				Throwables.getRootCause(exception).getMessage());

		final GroupVector<GroupVector<GqElement, GqGroup>, GqGroup> tooBigOtherExponentiatedGammaElements = otherCcrExponentiatedGammaElements = gqGroupGenerator.genRandomGqElementMatrix(
				PSI, NUM_OTHER_NODES + 1).columnStream().collect(GroupVector.toGroupVector());

		decryptPCCInputBuilder = new DecryptPCCInput.Builder()
				.setExponentiatedGammaElements(exponentiatedGammaElements)
				.setOtherCcrExponentiatedGammaElements(tooBigOtherExponentiatedGammaElements)
				.setOtherCcrExponentiationProofs(otherCcrExponentiationProofs)
				.setEncryptedVote(encryptedVote)
				.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
				.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes);

		exception = assertThrows(IllegalArgumentException.class, decryptPCCInputBuilder::build);
		assertEquals("There must be exactly 3 vectors of other CCR's exponentiated gamma elements.",
				Throwables.getRootCause(exception).getMessage());
	}

	private GroupVector<ExponentiationProof, ZqGroup> genRandomExponentiationProofVector(final int size) {
		return Stream.generate(
						() -> new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
				.limit(size)
				.collect(GroupVector.toGroupVector());
	}
}
