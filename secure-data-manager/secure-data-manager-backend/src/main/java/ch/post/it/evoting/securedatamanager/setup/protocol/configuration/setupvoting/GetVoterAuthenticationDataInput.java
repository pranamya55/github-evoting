/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.validations.StartVotingKeyValidation;

/**
 * Regroups the input values needed by the GetVoterAuthenticationData algorithm.
 *
 * <ul>
 *     <li>SVK, the vector of start voting keys. Non-null and contains N<sub>E</sub> valid Base32 strings of size l<sub>SVK</sub>.</li>
 *     <li>EA, the vector of extended authentication factors. Non-null and contains N<sub>E</sub> digit sequences of size l<sub>EA</sub>.</li>
 * </ul>
 */
public record GetVoterAuthenticationDataInput(ImmutableList<String> startVotingKeys, ImmutableList<String> extendedAuthenticationFactors) {

	public GetVoterAuthenticationDataInput {
		checkNotNull(startVotingKeys).stream().parallel().forEach(StartVotingKeyValidation::validate);
		checkArgument(!startVotingKeys.isEmpty(), "The start voting keys must not be empty.");

		checkNotNull(extendedAuthenticationFactors);
		checkArgument(!extendedAuthenticationFactors.isEmpty(), "The extended authentication factors must not be empty.");

		checkArgument(startVotingKeys.size() == extendedAuthenticationFactors.size(),
				"There must be as many extended authentication factors as start voting keys.");
	}

}
