/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generate;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;
import java.util.stream.Collectors;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationData;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Encapsulates the flattened (combining all chunks) setup component verification data and control component code shares payloads.
 * <p>
 * All control components generate encrypted long return code shares during the configuration phase. The encrypted long return code shares contain
 * both the exponentiated encrypted partial choice return codes and the exponentiated encrypted confirmation keys.
 */
public class EncryptedNodeLongReturnCodeSharesChunk {

	private final String electionEventId;
	private final String verificationCardSetId;
	private final ImmutableList<String> verificationCardIds;
	private final int chunkId;
	private final ImmutableList<EncryptedSingleNodeLongReturnCodeSharesChunk> controlComponentCodeSharesChunks;
	private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes;
	private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys;

	private EncryptedNodeLongReturnCodeSharesChunk(final String electionEventId, final String verificationCardSetId,
			final ImmutableList<String> verificationCardIds, final int chunkId,
			final ImmutableList<EncryptedSingleNodeLongReturnCodeSharesChunk> controlComponentCodeSharesChunks,
			final ImmutableList<SetupComponentVerificationData> setupComponentVerificationData) {
		this.electionEventId = electionEventId;
		this.verificationCardSetId = verificationCardSetId;
		this.verificationCardIds = verificationCardIds;
		this.chunkId = chunkId;
		this.controlComponentCodeSharesChunks = controlComponentCodeSharesChunks;
		this.encryptedHashedPartialChoiceReturnCodes = setupComponentVerificationData.stream()
				.map(SetupComponentVerificationData::encryptedHashedSquaredPartialChoiceReturnCodes)
				.collect(GroupVector.toGroupVector());
		this.encryptedHashedConfirmationKeys = setupComponentVerificationData.stream()
				.map(SetupComponentVerificationData::encryptedHashedSquaredConfirmationKey)
				.collect(GroupVector.toGroupVector());
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public ImmutableList<String> getVerificationCardIds() {
		return verificationCardIds;
	}

	public int getChunkId() {
		return chunkId;
	}

	public ImmutableList<EncryptedSingleNodeLongReturnCodeSharesChunk> getControlComponentCodeSharesChunks() {
		return controlComponentCodeSharesChunks;
	}

	public GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> getEncryptedHashedPartialChoiceReturnCodes() {
		return encryptedHashedPartialChoiceReturnCodes;
	}

	public GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> getEncryptedHashedConfirmationKeys() {
		return encryptedHashedConfirmationKeys;
	}

	public static class Builder {

		private String electionEventId;
		private String verificationCardSetId;
		private int chunkId;
		private ImmutableList<EncryptedSingleNodeLongReturnCodeSharesChunk> controlComponentCodeSharesChunks;
		private ImmutableList<SetupComponentVerificationData> setupComponentVerificationData;

		public Builder setElectionEventId(final String electionEventId) {
			this.electionEventId = electionEventId;
			return this;
		}

		public Builder setVerificationCardSetId(final String verificationCardSetId) {
			this.verificationCardSetId = verificationCardSetId;
			return this;
		}

		public Builder setChunkId(final int chunkId) {
			this.chunkId = chunkId;
			return this;
		}

		public Builder setControlComponentCodeSharesChunks(
				final ImmutableList<EncryptedSingleNodeLongReturnCodeSharesChunk> controlComponentCodeSharesChunks) {
			this.controlComponentCodeSharesChunks = controlComponentCodeSharesChunks;
			return this;
		}

		public Builder setSetupComponentVerificationData(final ImmutableList<SetupComponentVerificationData> setupComponentVerificationData) {
			this.setupComponentVerificationData = setupComponentVerificationData;
			return this;
		}

		/**
		 * Creates an EncryptedNodeLongReturnCodeSharesChunk. All fields must have been set and be non-null.
		 *
		 * @return a new EncryptedNodeLongReturnCodeSharesChunk.
		 * @throws NullPointerException      if any of the fields is null.
		 * @throws IllegalArgumentException  if
		 *                                   <ul>
		 *                                       <li>The chunk id is negative.</li>
		 *                                       <li>The nodes return a wrong number or invalid values of node ids.</li>
		 *                                       <li>The nodes do not return the same verification card set ids (size and values).</li>
		 *                                       <li>The verification card set ids do not correspond to node's ones (size and values).</li>
		 *                                   </ul>
		 * @throws FailedValidationException if
		 *                                   <ul>
		 *                                       <li>{@code electionEventId} has an invalid UUID format.</li>
		 *                                       <li>{@code verificationCardIds} contains an id with an invalid UUID format.</li>
		 *                                   </ul>
		 */
		public EncryptedNodeLongReturnCodeSharesChunk build() {
			validateUUID(electionEventId);
			validateUUID(verificationCardSetId);
			checkArgument(chunkId >= 0, String.format("The chunk id must be positive. [chunkId: %s]", chunkId));
			checkNotNull(controlComponentCodeSharesChunks);
			checkNotNull(setupComponentVerificationData);
			final ImmutableList<String> verificationCardIds = setupComponentVerificationData.stream()
					.map(SetupComponentVerificationData::verificationCardId)
					.collect(ImmutableList.toImmutableList());

			final ImmutableList<Integer> controlComponentCodeSharesPayloadsNodeIds = controlComponentCodeSharesChunks.stream()
					.map(EncryptedSingleNodeLongReturnCodeSharesChunk::getNodeId)
					.collect(toImmutableList());
			checkArgument(ControlComponentNode.ids().size() == controlComponentCodeSharesPayloadsNodeIds.size()
							&& controlComponentCodeSharesPayloadsNodeIds.containsAll(ControlComponentNode.ids()),
					"Wrong number or invalid values of control component code shares payload's node ids. [required node ids: %s, found: %s]",
					ControlComponentNode.ids(), controlComponentCodeSharesPayloadsNodeIds);

			checkArgument(controlComponentCodeSharesChunks.stream().map(EncryptedSingleNodeLongReturnCodeSharesChunk::getVerificationCardIds)
							.collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).values().stream()
							.allMatch(i -> i == ControlComponentNode.ids().size()),
					"All nodes must return the same verification card ids. [electionEventId: %s, verificationCardSetId: %s]", electionEventId,
					verificationCardSetId);

			final ImmutableList<String> nodeVerificationCardIds = controlComponentCodeSharesChunks.get(0).getVerificationCardIds();
			checkArgument(verificationCardIds.size() == nodeVerificationCardIds.size() && verificationCardIds.containsAll(nodeVerificationCardIds),
					"The setup component verification data verification card ids must match the control component code shares verification card ids. [electionEventId: %s, verificationCardSetId: %s]",
					electionEventId, verificationCardSetId);

			return new EncryptedNodeLongReturnCodeSharesChunk(electionEventId, verificationCardSetId, verificationCardIds, chunkId,
					controlComponentCodeSharesChunks, setupComponentVerificationData);
		}
	}
}
