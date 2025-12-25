/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.evotinglibraries.domain.validations.StartVotingKeyValidation;

/**
 * Regroups the input needed by the GenCredDat algorithm.
 *
 * <ul>
 *     <li>k, the vector of verification card secret keys. Not null.</li>
 *     <li>SVK, the vector of Start Voting Keys. Not null.</li>
 * </ul>
 */
public record GenCredDatInput(GroupVector<ZqElement, ZqGroup> verificationCardSecretKeys, ImmutableList<String> startVotingKeys) {

	public GenCredDatInput {
		checkNotNull(verificationCardSecretKeys);
		checkNotNull(startVotingKeys).forEach(StartVotingKeyValidation::validate);

		checkArgument(verificationCardSecretKeys.size() == startVotingKeys.size(), "All vectors must have the same size.");
		checkArgument(!verificationCardSecretKeys.isEmpty(), "The vector of verification card secret key must not be empty.");
	}
}
