/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;

class VerifyEncryptedPCCExponentiationProofsAlgorithmTest extends TestGroupSetup {

	private VerifyEncryptedExponentiationProofsContext context;
	private VerifyEncryptedPCCExponentiationProofsInput input;
	private ZeroKnowledgeProof zeroKnowledgeProof;
	private VerifyEncryptedPCCExponentiationProofsAlgorithm verifyEncryptedPCCExponentiationProofsAlgorithm;

	@BeforeEach
	void setUp() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		final int numberOfEligibleVoters = 5;
		final ImmutableList<String> verificationCardIds = Stream.generate(uuidGenerator::generate)
				.limit(numberOfEligibleVoters)
				.collect(toImmutableList());
		final int numberOfVotingOptions = 3;
		context = new VerifyEncryptedExponentiationProofsContext(gqGroup, ControlComponentNode.last().id(), electionEventId, verificationCardIds,
				numberOfVotingOptions);

		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertextVector(
				numberOfEligibleVoters, numberOfVotingOptions);
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> voterChoiceReturnCodeGenerationPublicKeys = Stream.generate(
						() -> elGamalGenerator.genRandomPublicKey(1))
				.limit(numberOfEligibleVoters)
				.collect(toGroupVector());
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertextVector(
				numberOfEligibleVoters,
				numberOfVotingOptions);
		final GroupVector<ExponentiationProof, ZqGroup> proofsOfCorrectExponentiation = Stream.generate(
						() -> new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
				.limit(numberOfEligibleVoters)
				.collect(toGroupVector());
		input = new VerifyEncryptedPCCExponentiationProofsInput(
				encryptedHashedPartialChoiceReturnCodes,
				voterChoiceReturnCodeGenerationPublicKeys,
				exponentiatedEncryptedHashedPartialChoiceReturnCodes,
				proofsOfCorrectExponentiation
		);

		zeroKnowledgeProof = mock(ZeroKnowledgeProof.class);
		verifyEncryptedPCCExponentiationProofsAlgorithm = new VerifyEncryptedPCCExponentiationProofsAlgorithm(zeroKnowledgeProof);
	}

	@Test
	@DisplayName("valid proofs returns true.")
	void validProofsReturnsTrue() {
		when(zeroKnowledgeProof.verifyExponentiation(any(), any(), any(), any())).thenReturn(true);

		assertTrue(() -> verifyEncryptedPCCExponentiationProofsAlgorithm.verifyEncryptedPCCExponentiationProofs(context, input));
	}

	@Test
	@DisplayName("invalid proofs returns false.")
	void invalidProofsReturnsFalse() {
		when(zeroKnowledgeProof.verifyExponentiation(any(), any(), any(), any())).thenReturn(false);

		assertFalse(() -> verifyEncryptedPCCExponentiationProofsAlgorithm.verifyEncryptedPCCExponentiationProofs(context, input));
	}

	@Test
	@DisplayName("null arguments throws NullPointerException.")
	void nullArgumentsThrows() {
		assertAll(
				() -> assertThrows(NullPointerException.class,
						() -> verifyEncryptedPCCExponentiationProofsAlgorithm.verifyEncryptedPCCExponentiationProofs(null, input)),
				() -> assertThrows(NullPointerException.class,
						() -> verifyEncryptedPCCExponentiationProofsAlgorithm.verifyEncryptedPCCExponentiationProofs(context, null))
		);
	}

	@Test
	@DisplayName("different group throws IllegalArgumentException.")
	void differentGroupThrows() {
		final VerifyEncryptedExponentiationProofsContext differentGroupContext = new VerifyEncryptedExponentiationProofsContext(otherGqGroup,
				context.nodeId(), context.electionEventId(), context.verificationCardIds(), context.numberOfVotingOptions());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> verifyEncryptedPCCExponentiationProofsAlgorithm.verifyEncryptedPCCExponentiationProofs(differentGroupContext, input));

		final String expectedMessage = "The context and input must have the same encryption group.";
		assertEquals(expectedMessage, exception.getMessage());
	}

	@Test
	@DisplayName("size of input parameters different from number of eligible voters throws IllegalArgumentException.")
	void sizeOfInputParametersDifferentFromNumberOfEligibleVotersThrows() {
		final ImmutableList<String> differentSizeVerificationCardIds = context.verificationCardIds()
				.subList(0, context.verificationCardIds().size() - 1);
		final VerifyEncryptedExponentiationProofsContext differentNumberOfEligibleVotersContext = new VerifyEncryptedExponentiationProofsContext(
				gqGroup,
				context.nodeId(), context.electionEventId(), differentSizeVerificationCardIds, context.numberOfVotingOptions());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> verifyEncryptedPCCExponentiationProofsAlgorithm.verifyEncryptedPCCExponentiationProofs(differentNumberOfEligibleVotersContext,
						input));

		final String expectedMessage = "The size of each input must be equal to the number of eligible voters.";
		assertEquals(expectedMessage, exception.getMessage());
	}

	@Test
	@DisplayName("ciphertext size different from number of voting options throws IllegalArgumentException.")
	void ciphertextSizeDifferentFromNumberOfVotingOptionsThrows() {
		final VerifyEncryptedExponentiationProofsContext differentNumberOfVotingOptionsContext = new VerifyEncryptedExponentiationProofsContext(
				gqGroup,
				context.nodeId(), context.electionEventId(), context.verificationCardIds(), context.numberOfVotingOptions() + 1);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> verifyEncryptedPCCExponentiationProofsAlgorithm.verifyEncryptedPCCExponentiationProofs(differentNumberOfVotingOptionsContext,
						input));

		final String expectedMessage = "The size of each exponentiated and encrypted, hashed partial Choice Return Codes must be equal to the number of voting options.";
		assertEquals(expectedMessage, exception.getMessage());
	}
}