/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.PlaintextEqualityProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;

/**
 * Tests of VerifyBallotCCRAlgorithm.
 */
@DisplayName("VerifyBallotCCRService")
class VerifyBallotCCRAlgorithmTest extends TestGroupSetup {

	private static final int PSI_MAX = 6;
	private static final int PSI = 5;
	private static final int DELTA_MAX = 1;
	private static final int DELTA = DELTA_MAX;
	private static final ZeroKnowledgeProof zeroKnowledgeProof = mock(ZeroKnowledgeProof.class);
	private static final Base64 base64 = BaseEncodingFactory.createBase64();
	private static final Hash hash = HashFactory.createHash();
	private static final PrimesMappingTableAlgorithms primesMappingTableAlgorithms = spy(new PrimesMappingTableAlgorithms());
	private static final GetHashContextAlgorithm getHashContextAlgorithm = new GetHashContextAlgorithm(base64, hash, primesMappingTableAlgorithms);
	private static VerifyBallotCCRAlgorithm verifyBallotCCRAlgorithm;
	private VerifyBallotCCRContext context;
	private VerifyBallotCCRInput input;
	private VerifyBallotCCRInput.Builder verifyBallotCCRInputBuilder;
	private VerifyBallotCCRContext.Builder verifyBallotCCRContextBuilder;

	@BeforeAll
	static void setUpAll() {
		verifyBallotCCRAlgorithm = new VerifyBallotCCRAlgorithm(zeroKnowledgeProof, primesMappingTableAlgorithms, getHashContextAlgorithm);
	}

	@BeforeEach
	@SuppressWarnings("java:S117")
	void setUp() {
		verifyBallotCCRContextBuilder = new VerifyBallotCCRContext.Builder();

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();
		final String verificationCardId = uuidGenerator.generate();

		final PrimesMappingTable primesMappingTable = new PrimesMappingTableGenerator(gqGroup).generate(1);
		final GqElement verificationCardPublicKey = gqGroupGenerator.genMember();
		final ElGamalMultiRecipientPublicKey electionPublicKey = new ElGamalMultiRecipientPublicKey(
				gqGroupGenerator.genRandomGqElementVector(DELTA_MAX));
		final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey = new ElGamalMultiRecipientPublicKey(
				gqGroupGenerator.genRandomGqElementVector(PSI_MAX));

		context = verifyBallotCCRContextBuilder
				.setEncryptionGroup(gqGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardId(verificationCardId)
				.setPrimesMappingTable(primesMappingTable)
				.setVerificationCardPublicKey(verificationCardPublicKey)
				.setElectionPublicKey(electionPublicKey)
				.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey)
				.build();

		verifyBallotCCRInputBuilder = new VerifyBallotCCRInput.Builder();
		final ElGamalMultiRecipientCiphertext encryptedVote = elGamalGenerator.genRandomCiphertext(1);
		final ZqElement k_id = zqGroupGenerator.genRandomZqElementMember();
		final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote = encryptedVote.getCiphertextExponentiation(k_id);
		final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(PSI);
		final ExponentiationProof exponentiationProof = new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementMember());
		final PlaintextEqualityProof plaintextEqualityProof = new PlaintextEqualityProof(zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementVector(2));

		input = verifyBallotCCRInputBuilder
				.setEncryptedVote(encryptedVote)
				.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
				.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes)
				.setExponentiationProof(exponentiationProof)
				.setPlaintextEqualityProof(plaintextEqualityProof)
				.build();

		doReturn(PSI).when(primesMappingTableAlgorithms).getPsi(any());
		doReturn(DELTA).when(primesMappingTableAlgorithms).getDelta(any());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParams() {
		when(zeroKnowledgeProof.verifyExponentiation(any(), any(), any(), any())).thenReturn(true);
		when(zeroKnowledgeProof.verifyPlaintextEquality(any(), any(), any(), any(), any(), any())).thenReturn(true);

		assertTrue(verifyBallotCCRAlgorithm.verifyBallotCCR(context, input));
	}

	@Test
	@DisplayName("invalid proofs return false")
	void invalidExponentiationProof() {
		when(zeroKnowledgeProof.verifyExponentiation(any(), any(), any(), any())).thenReturn(false);
		when(zeroKnowledgeProof.verifyPlaintextEquality(any(), any(), any(), any(), any(), any())).thenReturn(true);
		assertFalse(verifyBallotCCRAlgorithm.verifyBallotCCR(context, input));

		when(zeroKnowledgeProof.verifyExponentiation(any(), any(), any(), any())).thenReturn(true);
		when(zeroKnowledgeProof.verifyPlaintextEquality(any(), any(), any(), any(), any(), any())).thenReturn(false);
		assertFalse(verifyBallotCCRAlgorithm.verifyBallotCCR(context, input));

		when(zeroKnowledgeProof.verifyExponentiation(any(), any(), any(), any())).thenReturn(false);
		when(zeroKnowledgeProof.verifyPlaintextEquality(any(), any(), any(), any(), any(), any())).thenReturn(false);
		assertFalse(verifyBallotCCRAlgorithm.verifyBallotCCR(context, input));
	}

	@Test
	@DisplayName("any null parameter throws NullPointerException")
	void nullParams() {
		assertThrows(NullPointerException.class, () -> verifyBallotCCRAlgorithm.verifyBallotCCR(context, null));
		assertThrows(NullPointerException.class, () -> verifyBallotCCRAlgorithm.verifyBallotCCR(null, input));
	}

	@Test
	@DisplayName("context and input with different groups throws IllegalArgumentException")
	void differentGroupContextInput() {
		final PrimesMappingTable otherPrimesMappingTable = new PrimesMappingTableGenerator(otherGqGroup).generate(1);
		final GqElement verificationCardPublicKey = otherGqGroupGenerator.genMember();
		final ElGamalMultiRecipientPublicKey electionPublicKey = new ElGamalMultiRecipientPublicKey(
				otherGqGroupGenerator.genRandomGqElementVector(DELTA_MAX));
		final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey = new ElGamalMultiRecipientPublicKey(
				otherGqGroupGenerator.genRandomGqElementVector(PSI_MAX));
		final VerifyBallotCCRContext otherContext = verifyBallotCCRContextBuilder
				.setEncryptionGroup(otherGqGroup)
				.setPrimesMappingTable(otherPrimesMappingTable)
				.setVerificationCardPublicKey(verificationCardPublicKey)
				.setElectionPublicKey(electionPublicKey)
				.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey)
				.build();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> verifyBallotCCRAlgorithm.verifyBallotCCR(otherContext, input));
		assertEquals("The context and input must have the same group.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("wrong size encrypted partial Choice Return Codes")
	void wrongSizeEncryptedPartialChoiceReturnCodes() {
		doReturn(PSI - 1).when(primesMappingTableAlgorithms).getPsi(any());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> verifyBallotCCRAlgorithm.verifyBallotCCR(context, input));
		assertEquals(String.format(
						"The encrypted partial Choice Return Codes size must be equal to number of selectable voting options. [E2_size: %s, psi: %s]",
						PSI, PSI - 1),
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("numberOfAllowedWriteInsPlusOne bigger than delta_max throws IllegalArgumentException")
	void wrongSizeDelta() {
		doReturn(DELTA + 1).when(primesMappingTableAlgorithms).getDelta(any());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> verifyBallotCCRAlgorithm.verifyBallotCCR(context, input));
		assertEquals(String.format("The encrypted vote size must be equal to the number of allowed write-ins + 1. [E1_size: %s, delta: %s]",
						DELTA, DELTA + 1),
				Throwables.getRootCause(exception).getMessage());
	}

}
