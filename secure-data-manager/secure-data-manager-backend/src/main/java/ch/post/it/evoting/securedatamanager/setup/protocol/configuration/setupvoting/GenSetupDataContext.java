/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.evotinglibraries.domain.election.PartialPrimesMappingTableEntry;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;

/**
 * Regroups the output values needed by the GenSetupData algorithm.
 *
 * <ul>
 *     <li>vcs, the vector of verification card set ids. Non-null and contains valid UUIDs.</li>
 *     <li>Primes Mapping Table entry subset for all vcs:</li>
 *     <ul>
 *        <li>v&#771;, the list of actual voting options. Non-null and contains valid actual voting options.</li>
 *        <li>&sigma;, the list of semantic information. Non-null and contains valid semantic information.</li>
 *        <li>&tau;, the list of correctness information. Non-null and contains valid correctness information.</li>
 *     </ul>
 *     <li>n<sub>sup</sub>, the maximum supported number of voting options. Strictly positive.</li>
 *     <li>&psi;<sub>sup</sub>, the maximum supported number of selections. Strictly positive.</li>
 *     <li>&delta;<sub>sup</sub>, the maximum supported number of write-ins + 1. Strictly positive.</li>
 * </ul>
 */
public class GenSetupDataContext {

	private final ImmutableList<String> verificationsCardSetIds;
	private final ImmutableMap<String, ImmutableList<PartialPrimesMappingTableEntry>> partialPTableEntriesPerVerificationCardSetId;
	private final int maximumSupportedNumberOfVotingOptions;
	private final int maximumSupportedNumberOfSelections;
	private final int maximumSupportedNumberOfWriteInsPlusOne;

	public GenSetupDataContext(
			final ImmutableMap<String, ImmutableList<PartialPrimesMappingTableEntry>> partialPTableEntriesPerVerificationCardSetId) {
		this.partialPTableEntriesPerVerificationCardSetId = checkNotNull(partialPTableEntriesPerVerificationCardSetId);
		this.verificationsCardSetIds = this.partialPTableEntriesPerVerificationCardSetId.keySet().stream()
				.map(Validations::validateUUID)
				.sorted(String::compareTo)
				.collect(toImmutableList());
		// The constructor of PartialPrimesMappingTableEntry verifies that the actual voting options, the semantic information and the
		// correctness information are valid.

		this.maximumSupportedNumberOfVotingOptions = MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
		this.maximumSupportedNumberOfSelections = MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
		this.maximumSupportedNumberOfWriteInsPlusOne = MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1;
	}

	public ImmutableList<String> getVerificationsCardSetIds() {
		return verificationsCardSetIds;
	}

	public ImmutableMap<String, ImmutableList<PartialPrimesMappingTableEntry>> getpartialPTableEntriesPerVerificationCardSetId() {
		return partialPTableEntriesPerVerificationCardSetId;
	}

	public int getMaximumSupportedNumberOfVotingOptions() {
		return maximumSupportedNumberOfVotingOptions;
	}

	public int getMaximumSupportedNumberOfSelections() {
		return maximumSupportedNumberOfSelections;
	}

	public int getMaximumSupportedNumberOfWriteInsPlusOne() {
		return maximumSupportedNumberOfWriteInsPlusOne;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final GenSetupDataContext that = (GenSetupDataContext) o;
		return maximumSupportedNumberOfVotingOptions == that.maximumSupportedNumberOfVotingOptions
				&& maximumSupportedNumberOfSelections == that.maximumSupportedNumberOfSelections
				&& maximumSupportedNumberOfWriteInsPlusOne == that.maximumSupportedNumberOfWriteInsPlusOne && verificationsCardSetIds.equals(
				that.verificationsCardSetIds) && partialPTableEntriesPerVerificationCardSetId.equals(
				that.partialPTableEntriesPerVerificationCardSetId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(verificationsCardSetIds, partialPTableEntriesPerVerificationCardSetId, maximumSupportedNumberOfVotingOptions,
				maximumSupportedNumberOfSelections, maximumSupportedNumberOfWriteInsPlusOne);
	}
}
