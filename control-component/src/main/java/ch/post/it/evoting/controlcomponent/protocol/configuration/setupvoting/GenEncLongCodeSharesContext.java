/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.hasNoDuplicates;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;

/**
 * Regroups the context values needed by the GenEncLongCodeShares algorithm.
 *
 * <ul>
 *     <li>(p, q, g), the encryption group. Not null.</li>
 *     <li>j, the CCR’s index. In range [1, 4].</li>
 *     <li>ee, the election event id. Not null and a valid UUID.</li>
 *     <li>vcs, the verification card set id. Not null.</li>
 *     <li>vc, a vector of verification card ids. Not null and contains valid UUIDs.</li>
 *     <li>n, the number of voting options. In range [1, n<sub>sup</sub>].</li>
 * </ul>
 */
public class GenEncLongCodeSharesContext {

	private final GqGroup encryptionGroup;
	private final int nodeId;
	private final String electionEventId;
	private final String verificationCardSetId;
	private final ImmutableList<String> verificationCardIds;
	private final int numberOfVotingOptions;

	private GenEncLongCodeSharesContext(final GqGroup encryptionGroup, final int nodeId, final String electionEventId,
			final String verificationCardSetId, final ImmutableList<String> verificationCardIds, final int numberOfVotingOptions) {
		this.encryptionGroup = encryptionGroup;
		this.nodeId = nodeId;
		this.electionEventId = electionEventId;
		this.verificationCardSetId = verificationCardSetId;
		this.verificationCardIds = verificationCardIds;
		this.numberOfVotingOptions = numberOfVotingOptions;
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

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public ImmutableList<String> getVerificationCardIds() {
		return verificationCardIds;
	}

	public int getNumberOfVotingOptions() {
		return numberOfVotingOptions;
	}

	/**
	 * Builder performing input validations before constructing a {@link GenEncLongCodeSharesContext}.
	 */
	public static class Builder {

		private GqGroup encryptionGroup;
		private int nodeId;
		private String electionEventId;
		private String verificationCardSetId;
		private ImmutableList<String> verificationCardIds;
		private int numberOfVotingOptions;

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

		public Builder setVerificationCardSetId(final String verificationCardSetId) {
			this.verificationCardSetId = verificationCardSetId;
			return this;
		}

		public Builder setVerificationCardIds(final ImmutableList<String> verificationCardIds) {
			this.verificationCardIds = verificationCardIds;
			return this;
		}

		public Builder setNumberOfVotingOptions(final int numberOfVotingOptions) {
			this.numberOfVotingOptions = numberOfVotingOptions;
			return this;
		}

		/**
		 * Creates the GenEncLongCodeSharesContext. All fields must have been set and be non-null.
		 *
		 * @return a new GenEncLongCodeSharesContext.
		 * @throws NullPointerException      if any of the fields is null.
		 * @throws FailedValidationException if any of the election event Id and verification card IDs do not comply with the required UUID format
		 * @throws IllegalArgumentException  if
		 *                                   <ul>
		 *                                       <li>The node id is not part of the know node ids.</li>
		 *                                       <li>The number of voting options is not strictly positive.</li>
		 *                                   </ul>
		 */
		public GenEncLongCodeSharesContext build() {
			checkNotNull(encryptionGroup);
			validateUUID(electionEventId);
			validateUUID(verificationCardSetId);
			checkNotNull(verificationCardIds).forEach(Validations::validateUUID);

			checkArgument(hasNoDuplicates(verificationCardIds), "The list of verification card ids contains duplicated values.");

			checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);
			checkArgument(numberOfVotingOptions > 0, "The number of voting options must be strictly positive.");
			checkArgument(numberOfVotingOptions <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
					"The number of voting options must be smaller or equal to the maximum supported number of voting options. [n: %s, n_sup: %s]",
					numberOfVotingOptions, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);

			return new GenEncLongCodeSharesContext(encryptionGroup, nodeId, electionEventId, verificationCardSetId, verificationCardIds,
					numberOfVotingOptions);
		}
	}
}

