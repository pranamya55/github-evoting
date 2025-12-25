/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.Optional;

import ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms.GetHashExtractedElectionEventService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;

/**
 * Regroups the context values needed by the DecryptPCC and PartialDecryptPCC algorithms.
 *
 * <ul>
 *     <li>j, the CCR's index. In range [1, 4].</li>
 *     <li>j&#770;, the other CCR's indeces. Not null and not empty.</li>
 *     <li>vc<sub>id</sub>, the verification card id. Not null and a valid UUID.</li>
 *     <li>&psi;, the number of allowed selections for this specific ballot box. In range [1, &psi;<sub>sup</sub>].</li>
 *     <li>&delta;, the number of allowed write-ins + 1 for this specific ballot box. In range [1, &delta;<sub>sup</sub>].</li>
 *     <li>For performance reasons, we extract the {@link ExtractedElectionEvent} eee in {@link GetHashExtractedElectionEventService#getHashExtractedElectionEvent}.
 *     		<ul>
 *     		    <li>(p, q, g), the encryption group. Not null.</li>
 *     		    <li>ee, the election event id. Not null and a valid UUID.</li>
 *     		</ul>
 *     </li>
 *     <li>(pk<sub>CCR_j&#770;_1</sub>, pk<sub>CCR_j&#770;_2</sub>, pk<sub>CCR_j&#770;_3</sub>), the other CCR's Choice Return Codes encryption keys. Not null.</li>
 * </ul>
 */
public class DecryptPCCContext {

	private final int nodeId;
	private final ImmutableList<Integer> otherNodeIds;
	private final String verificationCardId;
	private final int numberOfSelections;
	private final int numberOfWriteInsPlusOne;
	private final GqGroup encryptionGroup;
	private final String electionEventId;
	private final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> otherCcrChoiceReturnCodesEncryptionKeys;

	private DecryptPCCContext(final int nodeId, final ImmutableList<Integer> otherNodeIds, final String verificationCardId,
			final int numberOfSelections, final int numberOfWriteInsPlusOne, final GqGroup encryptionGroup, final String electionEventId,
			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> otherCcrChoiceReturnCodesEncryptionKeys) {
		this.nodeId = nodeId;
		this.otherNodeIds = otherNodeIds;
		this.verificationCardId = verificationCardId;
		this.numberOfSelections = numberOfSelections;
		this.numberOfWriteInsPlusOne = numberOfWriteInsPlusOne;
		this.encryptionGroup = encryptionGroup;
		this.electionEventId = electionEventId;
		this.otherCcrChoiceReturnCodesEncryptionKeys = otherCcrChoiceReturnCodesEncryptionKeys;
	}

	public int getNodeId() {
		return nodeId;
	}

	public ImmutableList<Integer> getOtherNodeIds() {
		return otherNodeIds;
	}

	public String getVerificationCardId() {
		return verificationCardId;
	}

	public int getNumberOfSelections() {
		return numberOfSelections;
	}

	public int getNumberOfWriteInsPlusOne() {
		return numberOfWriteInsPlusOne;
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public Optional<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>> getOtherCcrChoiceReturnCodesEncryptionKeys() {
		return Optional.ofNullable(otherCcrChoiceReturnCodesEncryptionKeys);
	}

	/**
	 * Builder performing input validations and cross-validations before constructing a {@link DecryptPCCContext}.
	 */
	public static class Builder {

		private int nodeId;
		private String verificationCardId;
		private int numberOfSelections;
		private int numberOfWriteInsPlusOne;
		private GqGroup encryptionGroup;
		private String electionEventId;
		private GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> otherCcrChoiceReturnCodesEncryptionKeys;

		public Builder setNodeId(final int nodeId) {
			this.nodeId = nodeId;
			return this;
		}

		public Builder setVerificationCardId(final String verificationCardId) {
			this.verificationCardId = verificationCardId;
			return this;
		}

		public Builder setNumberOfSelections(final int numberOfSelections) {
			this.numberOfSelections = numberOfSelections;
			return this;
		}

		public Builder setNumberOfWriteInsPlusOne(final int numberOfWriteInsPlusOne) {
			this.numberOfWriteInsPlusOne = numberOfWriteInsPlusOne;
			return this;
		}

		public Builder setEncryptionGroup(final GqGroup encryptionGroup) {
			this.encryptionGroup = encryptionGroup;
			return this;
		}

		public Builder setElectionEventId(final String electionEventId) {
			this.electionEventId = electionEventId;
			return this;
		}

		public Builder setOtherCcrChoiceReturnCodesEncryptionKeys(
				final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> otherCcrChoiceReturnCodesEncryptionKeys) {
			this.otherCcrChoiceReturnCodesEncryptionKeys = otherCcrChoiceReturnCodesEncryptionKeys;
			return this;
		}

		public DecryptPCCContext build() {
			checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);
			validateUUID(verificationCardId);
			checkNotNull(encryptionGroup);
			validateUUID(electionEventId);

			final ImmutableList<Integer> otherNodeIds = ControlComponentNode.ids().stream().filter(j -> !j.equals(nodeId)).collect(toImmutableList());
			checkArgument(otherNodeIds.size() == ControlComponentNode.ids().size() - 1,
					"The size of the other node ids must be equal to the number of known node ids - 1.");

			checkArgument(numberOfSelections > 0, "The number of selections must be strictly positive. [psi: %s]", numberOfSelections);
			checkArgument(numberOfSelections <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
					"The number of selections must be smaller or equal to the maximum supported number of selections. [psi: %s, psi_sup: %s]",
					numberOfSelections, MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);

			checkArgument(numberOfWriteInsPlusOne > 0, "The number of write-ins + 1 must be strictly positive. [delta: %s]", numberOfWriteInsPlusOne);
			checkArgument(numberOfWriteInsPlusOne <= MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1,
					"The number of write-ins + 1 must be smaller or equal to the maximum supported number of write-ins + 1. [delta: %s, delta_sup: %s]",
					numberOfWriteInsPlusOne, MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1);

			if (Objects.nonNull(otherCcrChoiceReturnCodesEncryptionKeys)) {
				checkArgument(otherCcrChoiceReturnCodesEncryptionKeys.size() == 3,
						"There must be exactly 3 vectors of other CCR's Choice Return Codes encryption keys.");
				checkArgument(otherCcrChoiceReturnCodesEncryptionKeys.allEqual(
								ElGamalMultiRecipientPublicKey::size),
						"All other CCR's Choice Return Codes encryption keys must have the same size.");

				checkArgument(otherCcrChoiceReturnCodesEncryptionKeys.getGroup().equals(encryptionGroup),
						"The encryption group of the other CCR's Choice Return Codes encryption keys must be equal to the encryption group.");

			}

			return new DecryptPCCContext(nodeId, otherNodeIds, verificationCardId, numberOfSelections, numberOfWriteInsPlusOne, encryptionGroup,
					electionEventId, otherCcrChoiceReturnCodesEncryptionKeys);
		}
	}
}
