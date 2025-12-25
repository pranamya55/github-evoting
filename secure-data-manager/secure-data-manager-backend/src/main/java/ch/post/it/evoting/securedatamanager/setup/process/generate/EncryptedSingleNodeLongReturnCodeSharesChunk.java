/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generate;

import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;

/**
 * Encapsulates the flattened (combining all chunks) control component code shares payloads of a single node.
 * <p>
 * All control components generate encrypted long return code shares during the configuration phase. The encrypted long return code shares contain
 * both the exponentiated encrypted partial choice return codes and the exponentiated encrypted confirmation keys.
 */
public class EncryptedSingleNodeLongReturnCodeSharesChunk {

	private final int nodeId;
	private final int chunkId;
	private final ImmutableList<String> verificationCardIds;
	private final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> voterChoiceReturnCodeGenerationPublicKeys;
	private final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> voterVoteCastReturnCodeGenerationPublicKeys;
	private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodes;
	private final GroupVector<ExponentiationProof, ZqGroup> proofsOfCorrectPartialChoiceReturnCodesExponentiation;
	private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKey;
	private final GroupVector<ExponentiationProof, ZqGroup> proofsOfCorrectConfirmationKeyExponentiation;

	public EncryptedSingleNodeLongReturnCodeSharesChunk(final ControlComponentCodeSharesPayload controlComponentCodeSharesPayload) {
		checkNotNull(controlComponentCodeSharesPayload);

		this.nodeId = controlComponentCodeSharesPayload.getNodeId();
		this.chunkId = controlComponentCodeSharesPayload.getChunkId();
		this.verificationCardIds = controlComponentCodeSharesPayload.getControlComponentCodeShares().stream()
				.map(ControlComponentCodeShare::verificationCardId)
				.collect(ImmutableList.toImmutableList());
		voterChoiceReturnCodeGenerationPublicKeys = controlComponentCodeSharesPayload.getControlComponentCodeShares().stream()
				.map(ControlComponentCodeShare::voterChoiceReturnCodeGenerationPublicKey)
				.collect(GroupVector.toGroupVector());
		voterVoteCastReturnCodeGenerationPublicKeys = controlComponentCodeSharesPayload.getControlComponentCodeShares().stream()
				.map(ControlComponentCodeShare::voterVoteCastReturnCodeGenerationPublicKey)
				.collect(GroupVector.toGroupVector());
		exponentiatedEncryptedHashedPartialChoiceReturnCodes = controlComponentCodeSharesPayload.getControlComponentCodeShares().stream()
				.map(ControlComponentCodeShare::exponentiatedEncryptedPartialChoiceReturnCodes)
				.collect(GroupVector.toGroupVector());
		proofsOfCorrectPartialChoiceReturnCodesExponentiation = controlComponentCodeSharesPayload.getControlComponentCodeShares().stream()
				.map(ControlComponentCodeShare::encryptedPartialChoiceReturnCodeExponentiationProof)
				.collect(GroupVector.toGroupVector());
		exponentiatedEncryptedHashedConfirmationKey = controlComponentCodeSharesPayload.getControlComponentCodeShares().stream()
				.map(ControlComponentCodeShare::exponentiatedEncryptedConfirmationKey)
				.collect(GroupVector.toGroupVector());
		proofsOfCorrectConfirmationKeyExponentiation = controlComponentCodeSharesPayload.getControlComponentCodeShares().stream()
				.map(ControlComponentCodeShare::encryptedConfirmationKeyExponentiationProof)
				.collect(GroupVector.toGroupVector());

		// The group and size validations are done in the constructor of ControlComponentCodeSharesPayload.
	}

	public int getNodeId() {
		return nodeId;
	}

	public int getChunkId() {
		return chunkId;
	}

	public ImmutableList<String> getVerificationCardIds() {
		return verificationCardIds;
	}

	public GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> getVoterChoiceReturnCodeGenerationPublicKeys() {
		return voterChoiceReturnCodeGenerationPublicKeys;
	}

	public GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> getVoterVoteCastReturnCodeGenerationPublicKeys() {
		return voterVoteCastReturnCodeGenerationPublicKeys;
	}

	public GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> getExponentiatedEncryptedHashedPartialChoiceReturnCodes() {
		return exponentiatedEncryptedHashedPartialChoiceReturnCodes;
	}

	public GroupVector<ExponentiationProof, ZqGroup> getProofsOfCorrectPartialChoiceReturnCodesExponentiation() {
		return proofsOfCorrectPartialChoiceReturnCodesExponentiation;
	}

	public GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> getExponentiatedEncryptedHashedConfirmationKey() {
		return exponentiatedEncryptedHashedConfirmationKey;
	}

	public GroupVector<ExponentiationProof, ZqGroup> getProofsOfCorrectConfirmationKeyExponentiation() {
		return proofsOfCorrectConfirmationKeyExponentiation;
	}
}
