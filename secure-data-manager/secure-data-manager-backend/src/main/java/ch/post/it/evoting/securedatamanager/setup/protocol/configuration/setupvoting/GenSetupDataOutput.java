/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;

/**
 * Regroups the output values needed by the GenSetupData algorithm.
 *
 * <ul>
 *     <li>(p, q, g), the encryption group. Not null.</li>
 *     <li>p, the small primes. Not null.</li>
 *     <li>n<sub>max</sub>, the maximum number of voting options. In range [1, n<sub>sup</sub>].</li>
 *     <li>&psi;<sub>max</sub>, the maximum number of selections. In range [1, &psi;<sub>sup</sub>].</li>
 *     <li>&delta;<sub>max</sub>, the maximum number of write-ins + 1. In range [1, &delta;<sub>sup</sub>]</li>
 *     <li>pTable, the primes mapping tables for each verification card set.</li>
 *     <li>pk<sub>setup</sub>, the setup public key. Not null.</li>
 *     <li>sk<sub>setup</sub>, the setup private key. Not null.</li>
 * </ul>
 */
public class GenSetupDataOutput {

	private final GqGroup encryptionGroup;
	private final GroupVector<PrimeGqElement, GqGroup> smallPrimes;
	private final int maximumNumberOfVotingOptions;
	private final int maximumNumberOfSelections;
	private final int maximumNumberOfWriteInsPlusOne;
	private final ImmutableMap<String, PrimesMappingTable> primesMappingTables;
	private final ElGamalMultiRecipientKeyPair setupKeyPair;

	private GenSetupDataOutput(final GqGroup encryptionGroup, final GroupVector<PrimeGqElement, GqGroup> smallPrimes,
			final int maximumNumberOfVotingOptions, final int maximumNumberOfSelections, final int maximumNumberOfWriteInsPlusOne,
			final ImmutableMap<String, PrimesMappingTable> primesMappingTables, final ElGamalMultiRecipientKeyPair setupKeyPair) {
		this.encryptionGroup = checkNotNull(encryptionGroup);
		this.smallPrimes = checkNotNull(smallPrimes);
		this.maximumNumberOfVotingOptions = maximumNumberOfVotingOptions;
		this.maximumNumberOfSelections = maximumNumberOfSelections;
		this.maximumNumberOfWriteInsPlusOne = maximumNumberOfWriteInsPlusOne;
		this.primesMappingTables = checkNotNull(primesMappingTables);
		this.setupKeyPair = checkNotNull(setupKeyPair);

		checkArgument(maximumNumberOfVotingOptions > 0 && maximumNumberOfVotingOptions <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
				"The maximum number of voting options must be strictly greater than zero and smaller or equal to the maximum supported number of voting options. [n_max: %s, n_sup: %s]",
				maximumNumberOfVotingOptions, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);
		checkArgument(maximumNumberOfSelections > 0 && maximumNumberOfSelections <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
				"The maximum number of selections must be strictly greater than zero and smaller or equal to the maximum supported number of selections. [psi_max: %s, psi_sup: %s]",
				maximumNumberOfSelections, MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);
		checkArgument(maximumNumberOfWriteInsPlusOne > 0 && maximumNumberOfWriteInsPlusOne <= MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1,
				"The maximum number of write-ins + 1 must be strictly greater than zero and smaller or equal to the maximum supported number of write-ins + 1. [delta_max: %s, delta_sup: %s]",
				maximumNumberOfWriteInsPlusOne, MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1);

		checkArgument(setupKeyPair.size() == maximumNumberOfVotingOptions,
				"The size of the setup key pair must be equal to the maximum number of voting options.");
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	public GroupVector<PrimeGqElement, GqGroup> getSmallPrimes() {
		return smallPrimes;
	}

	public int getMaximumNumberOfVotingOptions() {
		return maximumNumberOfVotingOptions;
	}

	public int getMaximumNumberOfSelections() {
		return maximumNumberOfSelections;
	}

	public int getMaximumNumberOfWriteInsPlusOne() {
		return maximumNumberOfWriteInsPlusOne;
	}

	public ImmutableMap<String, PrimesMappingTable> getPrimesMappingTables() {
		return primesMappingTables;
	}

	public ElGamalMultiRecipientKeyPair getSetupKeyPair() {
		return setupKeyPair;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final GenSetupDataOutput that = (GenSetupDataOutput) o;
		return maximumNumberOfVotingOptions == that.maximumNumberOfVotingOptions && maximumNumberOfSelections == that.maximumNumberOfSelections
				&& maximumNumberOfWriteInsPlusOne == that.maximumNumberOfWriteInsPlusOne && encryptionGroup.equals(that.encryptionGroup)
				&& smallPrimes.equals(that.smallPrimes) && primesMappingTables.equals(that.primesMappingTables) && setupKeyPair.equals(
				that.setupKeyPair);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptionGroup, smallPrimes, maximumNumberOfVotingOptions, maximumNumberOfSelections, maximumNumberOfWriteInsPlusOne,
				primesMappingTables, setupKeyPair);
	}

	public static class Builder {
		private GqGroup encryptionGroup;
		private GroupVector<PrimeGqElement, GqGroup> smallPrimes;
		private int maximumNumberOfVotingOptions;
		private int maximumNumberOfSelections;
		private int maximumNumberOfWriteInsPlusOne;
		private ImmutableMap<String, PrimesMappingTable> primesMappingTables;
		private ElGamalMultiRecipientKeyPair setupKeyPair;

		public Builder() {
			// Do nothing
		}

		public Builder setEncryptionGroup(final GqGroup encryptionGroup) {
			this.encryptionGroup = encryptionGroup;
			return this;
		}

		public Builder setSmallPrimes(final GroupVector<PrimeGqElement, GqGroup> smallPrimes) {
			this.smallPrimes = smallPrimes;
			return this;
		}

		public Builder setMaximumNumberOfVotingOptions(final int maximumNumberOfVotingOptions) {
			this.maximumNumberOfVotingOptions = maximumNumberOfVotingOptions;
			return this;
		}

		public Builder setMaximumNumberOfSelections(final int maximumNumberOfSelections) {
			this.maximumNumberOfSelections = maximumNumberOfSelections;
			return this;
		}

		public Builder setMaximumNumberOfWriteInsPlusOne(final int maximumNumberOfWriteInsPlusOne) {
			this.maximumNumberOfWriteInsPlusOne = maximumNumberOfWriteInsPlusOne;
			return this;
		}

		public Builder setPrimesMappingTables(final ImmutableMap<String, PrimesMappingTable> primesMappingTables) {
			this.primesMappingTables = primesMappingTables;
			return this;
		}

		public Builder setSetupKeyPair(final ElGamalMultiRecipientKeyPair setupKeyPair) {
			this.setupKeyPair = setupKeyPair;
			return this;
		}

		public GenSetupDataOutput build() {
			return new GenSetupDataOutput(encryptionGroup, smallPrimes, maximumNumberOfVotingOptions, maximumNumberOfSelections,
					maximumNumberOfWriteInsPlusOne, primesMappingTables, setupKeyPair);
		}
	}
}
