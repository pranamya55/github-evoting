/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;

/**
 * Regroups the context values needed by the VerifyEncryptedPCCExponentiationProofs and the VerifyEncryptedCKExponentiationProofs algorithm.
 *
 * <ul>
 *     <li>(p,q,g), the encryption group. Non-null.</li>
 *     <li>j, the CCR's index. In the range [1, 4].</li>
 *     <li>ee, the election event ID. Non-null and a valid UUID.</li>
 *     <li>vc, the vector of verification card IDs. Non-null and contains valid UUIDs.</li>
 *     <li>n, the number of voting options. In range [1, n<sub>sup</sub>]. Only needed for VerifyEncryptedPCCExponentiationProofs.</li>
 * </ul>
 */
public record VerifyEncryptedExponentiationProofsContext(GqGroup encryptionGroup, int nodeId, String electionEventId,
														 ImmutableList<String> verificationCardIds, int numberOfVotingOptions) {

	public VerifyEncryptedExponentiationProofsContext(final GqGroup encryptionGroup, final int nodeId, final String electionEventId,
			final ImmutableList<String> verificationCardIds, final int numberOfVotingOptions) {
		this.encryptionGroup = checkNotNull(encryptionGroup);
		this.nodeId = nodeId;
		this.electionEventId = validateUUID(electionEventId);
		this.verificationCardIds = checkNotNull(verificationCardIds);
		this.numberOfVotingOptions = numberOfVotingOptions;

		verificationCardIds.forEach(Validations::validateUUID);

		checkArgument(ControlComponentNode.ids().contains(nodeId), "The CCR's index must be in the range [1, %s]. [j: %s]",
				ControlComponentNode.ids().size(), nodeId);

		checkArgument(numberOfVotingOptions > 0, "The number of voting options must be strictly positive. [n: %s]", numberOfVotingOptions);
		checkArgument(numberOfVotingOptions <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
				"The number of voting options must be smaller or equal to the maximum supported number of voting options. [n: %s, n_sup: %s]",
				numberOfVotingOptions, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);
	}
}
