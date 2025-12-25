/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.mixonline;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Regroups the context values needed by the MixDecOnline algorithm.
 *
 * <ul>
 *     <li>(p, q, g), the encryption group. Not null.</li>
 *     <li>j, the control component index. In range [1, 4].</li>
 *     <li>ee, the election event id. Not null and a valid UUID.</li>
 *     <li>bb, the ballot box id. Not null and a valid UUID.</li>
 *     <li>&delta;, the number of allowed write-ins plus one. In range [1, &delta;<sub>sup</sub>].</li>
 *     <li>(EL<sub>pk,1</sub>, EL<sub>pk,2</sub>, EL<sub>pk,3</sub>, EL<sub>pk,4</sub>), the CCM election public keys. Not null.</li>
 *     <li>EB<sub>pk</sub>, the electoral board public key. Not null.</li>
 * </ul>
 */
public class MixDecOnlineContext {

	private final GqGroup encryptionGroup;
	private final int nodeId;
	private final String electionEventId;
	private final String ballotBoxId;
	private final int numberOfAllowedWriteInsPlusOne;
	private final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys;
	private final ElGamalMultiRecipientPublicKey electoralBoardPublicKey;

	private MixDecOnlineContext(final GqGroup encryptionGroup, final int nodeId, final String electionEventId, final String ballotBoxId,
			final int numberOfAllowedWriteInsPlusOne, final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys,
			final ElGamalMultiRecipientPublicKey electoralBoardPublicKey) {
		this.encryptionGroup = encryptionGroup;
		this.nodeId = nodeId;
		this.electionEventId = electionEventId;
		this.ballotBoxId = ballotBoxId;
		this.numberOfAllowedWriteInsPlusOne = numberOfAllowedWriteInsPlusOne;
		this.ccmElectionPublicKeys = ccmElectionPublicKeys;
		this.electoralBoardPublicKey = electoralBoardPublicKey;
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	public int getNodeId() {
		return nodeId;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public String getBallotBoxId() {
		return ballotBoxId;
	}

	public int getNumberOfAllowedWriteInsPlusOne() {
		return numberOfAllowedWriteInsPlusOne;
	}

	public GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> getCcmElectionPublicKeys() {
		return ccmElectionPublicKeys;
	}

	public ElGamalMultiRecipientPublicKey getElectoralBoardPublicKey() {
		return electoralBoardPublicKey;
	}

	/**
	 * Builder performing context validations before constructing a {@link MixDecOnlineContext}.
	 */
	public static class Builder {

		private GqGroup encryptionGroup;
		private int nodeId;
		private String electionEventId;
		private String ballotBoxId;
		private int numberOfAllowedWriteInsPlusOne;
		private GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys;
		private ElGamalMultiRecipientPublicKey electoralBoardPublicKey;

		public Builder setEncryptionGroup(final GqGroup encryptionGroup) {
			this.encryptionGroup = encryptionGroup;
			return this;
		}

		public Builder setNodeId(final int nodeId) {
			this.nodeId = nodeId;
			return this;
		}

		public Builder setElectionEventId(final String electionEventId) {
			this.electionEventId = electionEventId;
			return this;
		}

		public Builder setBallotBoxId(final String ballotBoxId) {
			this.ballotBoxId = ballotBoxId;
			return this;
		}

		public Builder setNumberOfAllowedWriteInsPlusOne(final int numberOfAllowedWriteInsPlusOne) {
			this.numberOfAllowedWriteInsPlusOne = numberOfAllowedWriteInsPlusOne;
			return this;
		}

		public Builder setCcmElectionPublicKeys(final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys) {
			this.ccmElectionPublicKeys = ccmElectionPublicKeys;
			return this;
		}

		public Builder setElectoralBoardPublicKey(final ElGamalMultiRecipientPublicKey electoralBoardPublicKey) {
			this.electoralBoardPublicKey = electoralBoardPublicKey;
			return this;
		}

		/**
		 * Constructs a MixDecOnlineContext object.
		 *
		 * @throws NullPointerException      if any id is null.
		 * @throws IllegalArgumentException  if
		 *                                   <ul>
		 *                                    <li>the number of allowed write-ins plus one is not in range [1, &delta;<sub>sup</sub>].</li>
		 *                                    <li>the node id is not part of the known node ids.</li>
		 *                                    <li>the inputs have different encryption groups.</li>
		 *                                    <li>the number of CCM election public keys is not the expected one.</li>
		 *                                   </ul>
		 * @throws FailedValidationException if the election event id or the ballot box id are not a valid UUID.
		 */
		public MixDecOnlineContext build() {
			checkNotNull(encryptionGroup);
			checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);
			validateUUID(electionEventId);
			validateUUID(ballotBoxId);

			checkArgument(numberOfAllowedWriteInsPlusOne >= 1, "The number of allowed write-ins + 1 must be greater than or equal to 1.");
			checkArgument(numberOfAllowedWriteInsPlusOne <= MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1,
					"The number of write-ins + 1 must be smaller or equal to the maximum supported number of write-ins + 1. [delta: %s, delta_sup: %s]",
					numberOfAllowedWriteInsPlusOne, MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1);

			checkNotNull(ccmElectionPublicKeys);
			checkNotNull(electoralBoardPublicKey);

			checkArgument(ccmElectionPublicKeys.size() == ControlComponentNode.ids().size(), "There must be exactly %s CCM election public keys.",
					ControlComponentNode.ids().size());
			checkArgument(numberOfAllowedWriteInsPlusOne <= ccmElectionPublicKeys.getElementSize(),
					"The number of write-ins + 1 must be smaller than or equal to the CCM election public keys size. [delta: %s, delta_max: %s]",
					numberOfAllowedWriteInsPlusOne, ccmElectionPublicKeys.getElementSize());
			checkArgument(ccmElectionPublicKeys.getElementSize() <= MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1,
					"The CCM election public keys must be smaller than or equal to the maximum supported number of write-ins + 1. [delta_max: %s, delta_sup: %s]",
					ccmElectionPublicKeys.getElementSize(), MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1);

			// Cross dimension check
			checkArgument(ccmElectionPublicKeys.getElementSize() == electoralBoardPublicKey.size(),
					"The CCM election public keys must have the same size as the electoral board public key.");

			// Cross group checks
			checkArgument(encryptionGroup.equals(electoralBoardPublicKey.getGroup()),
					"The encryption group of the public keys must be equal to the encryption group.");
			checkArgument(ccmElectionPublicKeys.getGroup().equals(electoralBoardPublicKey.getGroup()),
					"The CCM election public keys must have the same group as the electoral board public key.");

			return new MixDecOnlineContext(encryptionGroup, nodeId, electionEventId, ballotBoxId, numberOfAllowedWriteInsPlusOne,
					ccmElectionPublicKeys, electoralBoardPublicKey);
		}
	}
}
