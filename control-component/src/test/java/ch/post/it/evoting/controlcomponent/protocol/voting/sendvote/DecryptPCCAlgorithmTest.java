/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.controlcomponent.process.ExtractedElectionEventHashService;
import ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms.GetHashExtractedElectionEventService;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedElectionEventPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashExtractedElectionEventAlgorithm;

/**
 * Tests of DecryptPCCAlgorithm.
 */
@DisplayName("DecryptPCCService")
class DecryptPCCAlgorithmTest extends TestGroupSetup {

	private final int psi = 10;

	private ZeroKnowledgeProof zeroKnowledgeProofMock;
	private DecryptPCCAlgorithm decryptPCCAlgorithm;
	private DecryptPCCContext decryptPCCContext;
	private DecryptPCCInput decryptPCCInput;

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardId = uuidGenerator.generate();

		final GroupVector<GqElement, GqGroup> exponentiatedGammaElements = gqGroupGenerator.genRandomGqElementVector(psi);
		final GroupVector<GroupVector<GqElement, GqGroup>, GqGroup> otherExponentiatedGammaElements = GroupVector.of(
				gqGroupGenerator.genRandomGqElementVector(psi),
				gqGroupGenerator.genRandomGqElementVector(psi),
				gqGroupGenerator.genRandomGqElementVector(psi)
		);
		final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> otherExponentiationProofs = GroupVector.of(
				genRandomExponentiationProofVector(),
				genRandomExponentiationProofVector(),
				genRandomExponentiationProofVector()
		);
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> otherChoiceReturnCodesEncryptionKeys = GroupVector.of(
				elGamalGenerator.genRandomPublicKey(psi),
				elGamalGenerator.genRandomPublicKey(psi),
				elGamalGenerator.genRandomPublicKey(psi)
		);
		final ElGamalMultiRecipientCiphertext encryptedVote = elGamalGenerator.genRandomCiphertext(1);
		final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote = elGamalGenerator.genRandomCiphertext(1);
		final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(psi);

		final int nodeId = 1;
		final int delta = 1;
		decryptPCCContext = new DecryptPCCContext.Builder()
				.setNodeId(nodeId)
				.setVerificationCardId(verificationCardId)
				.setNumberOfSelections(psi)
				.setNumberOfWriteInsPlusOne(delta)
				.setEncryptionGroup(gqGroup)
				.setElectionEventId(electionEventId)
				.setOtherCcrChoiceReturnCodesEncryptionKeys(otherChoiceReturnCodesEncryptionKeys)
				.build();

		decryptPCCInput = new DecryptPCCInput.Builder()
				.setExponentiatedGammaElements(exponentiatedGammaElements)
				.setOtherCcrExponentiatedGammaElements(otherExponentiatedGammaElements)
				.setOtherCcrExponentiationProofs(otherExponentiationProofs)
				.setEncryptedVote(encryptedVote)
				.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
				.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes)
				.build();

		final ExtractedElectionEventHashService extractedElectionEventHashService = mock(ExtractedElectionEventHashService.class);
		final GetHashExtractedElectionEventService getHashExtractedElectionEventService = new GetHashExtractedElectionEventService(
				extractedElectionEventHashService);

		final ControlComponentExtractedElectionEventPayloadGenerator generator = new ControlComponentExtractedElectionEventPayloadGenerator(
				gqGroup);
		final ExtractedElectionEvent extractedElectionEvent = generator.generate(electionEventId, ControlComponentNode.THREE.id())
				.getExtractedElectionEvent();
		final Hash hash = HashFactory.createHash();
		final Base64 base64 = BaseEncodingFactory.createBase64();
		final GetHashExtractedElectionEventAlgorithm getHashExtractedElectionEventAlgorithm = new GetHashExtractedElectionEventAlgorithm(base64,
				hash);
		final String extractedElectionEventHash = getHashExtractedElectionEventAlgorithm.getHashExtractedElectionEvent(extractedElectionEvent);
		when(extractedElectionEventHashService.getHashExtractedElectionEvent(electionEventId)).thenReturn(extractedElectionEventHash);

		zeroKnowledgeProofMock = spy(ZeroKnowledgeProof.class);
		decryptPCCAlgorithm = new DecryptPCCAlgorithm(zeroKnowledgeProofMock, getHashExtractedElectionEventService);
	}

	@Test
	@DisplayName("any null arguments throws a NullPointerException")
	void decryptPCCWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> decryptPCCAlgorithm.decryptPCC(null, decryptPCCInput));
		assertThrows(NullPointerException.class, () -> decryptPCCAlgorithm.decryptPCC(decryptPCCContext, null));
	}

	@Test
	@DisplayName("valid arguments does not throw")
	void decryptPCCWithValidInputDoesNotThrow() {
		doReturn(true).when(zeroKnowledgeProofMock).verifyExponentiation(any(), any(), any(), any());

		final GroupVector<GqElement, GqGroup> gqElements = assertDoesNotThrow(
				() -> decryptPCCAlgorithm.decryptPCC(decryptPCCContext, decryptPCCInput));

		assertEquals(psi, gqElements.size());
	}

	@Test
	@DisplayName("context and input having different groups throws an IllegalArgumentException")
	void decryptPCCWithContextAndInputFromDifferentGroupsThrows() {
		final DecryptPCCContext spyDecryptPCCContext = spy(DecryptPCCAlgorithmTest.this.decryptPCCContext);
		doReturn(otherGqGroup).when(spyDecryptPCCContext).getEncryptionGroup();
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> decryptPCCAlgorithm.decryptPCC(spyDecryptPCCContext, decryptPCCInput));
		assertEquals("The context and input must have the same group.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("encrypted partial choice return codes with a size different from numberOfSelectableVotingOptions throws an IllegalArgumentException")
	void decryptPCCWithEncryptedPartialChoiceReturnCodesWrongSizeThrows() {
		// Encrypted partial choice return codes of size numberOfSelectableVotingOptions - 1
		final DecryptPCCInput decryptPCCInputSpy = spy(decryptPCCInput);
		final ElGamalMultiRecipientCiphertext tooShortEncryptedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(psi - 1);
		doReturn(tooShortEncryptedPartialChoiceReturnCodes).when(decryptPCCInputSpy).getEncryptedPartialChoiceReturnCodes();
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> decryptPCCAlgorithm.decryptPCC(decryptPCCContext, decryptPCCInputSpy));
		assertEquals(String.format("The encrypted partial Choice Return Codes size must be equal to psi. [psi: %s]", psi),
				Throwables.getRootCause(exception).getMessage());

		// Encrypted partial choice return codes of size numberOfSelectableVotingOptions + 1
		final ElGamalMultiRecipientCiphertext tooLongEncryptedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(psi + 1);
		doReturn(tooLongEncryptedPartialChoiceReturnCodes).when(decryptPCCInputSpy).getEncryptedPartialChoiceReturnCodes();
		exception = assertThrows(IllegalArgumentException.class,
				() -> decryptPCCAlgorithm.decryptPCC(decryptPCCContext, decryptPCCInputSpy));
		assertEquals(String.format("The encrypted partial Choice Return Codes size must be equal to psi. [psi: %s]", psi),
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("with failing zero knowledge proof validation throws an IllegalStateException")
	void decryptPCCWithFailingZeroKnowledgeProofVerification() {
		doReturn(false).when(zeroKnowledgeProofMock).verifyExponentiation(any(), any(), any(), any());

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> decryptPCCAlgorithm.decryptPCC(decryptPCCContext, decryptPCCInput));
		assertEquals(String.format("The verification of the other control component's exponentiation proof failed [control component: %d]", 2),
				Throwables.getRootCause(exception).getMessage());
	}

	private GroupVector<ExponentiationProof, ZqGroup> genRandomExponentiationProofVector() {
		return Stream.generate(
						() -> new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
				.limit(psi)
				.collect(GroupVector.toGroupVector());
	}
}
