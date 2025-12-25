/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;

/**
 * The output of the GetElectionEventEncryptionParameters algorithm.
 *
 * @param encryptionGroup the {@link GqGroup} with group modulus <i>p</i>, group cardinality <i>q</i>, and group generator <i>g</i>. Must be
 *                        non-null.
 * @param smallPrimes     the vector of small prime group elements. Must be non-null. The elements must belong to the encryption group.
 */
public record GetElectionEventEncryptionParametersOutput(GqGroup encryptionGroup, GroupVector<PrimeGqElement, GqGroup> smallPrimes) {

	public GetElectionEventEncryptionParametersOutput {
		checkNotNull(encryptionGroup);
		checkNotNull(smallPrimes);

		final int n_sup = MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
		checkArgument(smallPrimes.size() == n_sup,
				"There must be as many small primes as maximum supported number of voting options. [smallPrimesSize: %s, n_sup: %s]",
				smallPrimes.size(), n_sup);
		checkArgument(encryptionGroup.equals(smallPrimes.getGroup()), "The encryption group must be the same as the small primes' group.");
	}
}
