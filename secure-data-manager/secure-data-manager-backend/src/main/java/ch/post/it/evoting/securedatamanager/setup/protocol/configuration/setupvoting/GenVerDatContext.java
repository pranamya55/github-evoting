/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;

/**
 * Regroups the context values needed by the GenVerDat algorithm.
 *
 * <ul>
 *     <li>encryptionGroup, the encryption group. Non-null.</li>
 *     <li>ee, the election event id. Non-null and a valid UUID.</li>
 *     <li>N<sub>E</sub>, the number of eligible voters for the verification card set. Strictly positive.</li>
 *     <li>pTable, the primes mapping table. Not null.</li>
 *     <li>n<sub>max</sub>, the maximum number of voting options. In range [1, n<sub>sup</sub>].</li>
 * </ul>
 */
public record GenVerDatContext(GqGroup encryptionGroup, String electionEventId, int numberOfEligibleVoters, PrimesMappingTable primesMappingTable,
							   int maximumNumberOfVotingOptions) {

	public GenVerDatContext {
		checkNotNull(encryptionGroup);
		validateUUID(electionEventId);
		checkNotNull(primesMappingTable);

		checkArgument(numberOfEligibleVoters > 0, "The number of eligible voters must be strictly greater than 0.");
		checkArgument(maximumNumberOfVotingOptions > 0, "The number of eligible voters must be strictly positive.");
		checkArgument(maximumNumberOfVotingOptions <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
				"The maximum number of voting options must be smaller or equal to the maximum supported number of voting options. [n_max: %s, n_sup: %s]",
				maximumNumberOfVotingOptions, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);
		checkArgument(primesMappingTable.getNumberOfVotingOptions() <= maximumNumberOfVotingOptions,
				"The size of the pTable must not exceed the maximum number of voting options. [n: %s, n_max: %s]",
				primesMappingTable.getNumberOfVotingOptions(), maximumNumberOfVotingOptions);

		checkArgument(primesMappingTable.getEncryptionGroup().equals(encryptionGroup),
				"The primes mapping table's group must be equal to the encryption group.");
	}
}
