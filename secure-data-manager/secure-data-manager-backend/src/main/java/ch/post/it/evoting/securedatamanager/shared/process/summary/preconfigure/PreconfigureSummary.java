/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary.preconfigure;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;

/**
 * Summary of the pre-configuration step.
 */
public class PreconfigureSummary {

	private final GqGroup encryptionGroup;
	private final int maximumNumberOfVotingOptions;
	private final int maximumNumberOfSelections;
	private final int maximumNumberOfWriteInsPlusOne;
	private final ImmutableList<VerificationCardSetSummary> verificationCardSets;

	/**
	 * @param encryptionGroup                the encryption group.
	 * @param maximumNumberOfVotingOptions   the maximum number of voting options.
	 * @param maximumNumberOfSelections      the maximum number of selections.
	 * @param maximumNumberOfWriteInsPlusOne the maximum number of write-ins plus one.
	 * @param verificationCardSets           the verification card sets.
	 */
	private PreconfigureSummary(final GqGroup encryptionGroup, final int maximumNumberOfVotingOptions, final int maximumNumberOfSelections,
			final int maximumNumberOfWriteInsPlusOne, final ImmutableList<VerificationCardSetSummary> verificationCardSets) {
		this.encryptionGroup = checkNotNull(encryptionGroup);
		checkArgument(maximumNumberOfVotingOptions > 0, "The maximum number of voting options must be greater than 0.");
		checkArgument(maximumNumberOfVotingOptions <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
				"The maximum number of voting options must be smaller or equal to the maximum supported number of voting options.");
		this.maximumNumberOfVotingOptions = maximumNumberOfVotingOptions;
		checkArgument(maximumNumberOfSelections > 0, "The maximum number of selections must be greater than 0.");
		checkArgument(maximumNumberOfSelections <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
				"The maximum number of selections must be smaller or equal to the maximum supported number of selections.");
		this.maximumNumberOfSelections = maximumNumberOfSelections;
		checkArgument(maximumNumberOfWriteInsPlusOne > 0, "The maximum number of write-ins + 1 must be greater than 0.");
		checkArgument(maximumNumberOfWriteInsPlusOne <= MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1,
				"The maximum number of write-ins + 1 must be smaller or equal to the maximum supported number of write-ins + 1.");
		this.maximumNumberOfWriteInsPlusOne = maximumNumberOfWriteInsPlusOne;
		this.verificationCardSets = checkNotNull(verificationCardSets);
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
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

	public ImmutableList<VerificationCardSetSummary> getVerificationCardSets() {
		return verificationCardSets;
	}

	public static class Builder {
		private GqGroup encryptionGroup;
		private int maximumNumberOfVotingOptions;
		private int maximumNumberOfSelections;
		private int maximumNumberOfWriteInsPlusOne;
		private ImmutableList<VerificationCardSetSummary> verificationCardSets;

		public Builder withEncryptionGroup(final GqGroup encryptionGroup) {
			this.encryptionGroup = encryptionGroup;
			return this;
		}

		public Builder withMaximumNumberOfVotingOptions(final int maximumNumberOfVotingOptions) {
			this.maximumNumberOfVotingOptions = maximumNumberOfVotingOptions;
			return this;
		}

		public Builder withMaximumNumberOfSelections(final int maximumNumberOfSelections) {
			this.maximumNumberOfSelections = maximumNumberOfSelections;
			return this;
		}

		public Builder withMaximumNumberOfWriteInsPlusOne(final int maximumNumberOfWriteInsPlusOne) {
			this.maximumNumberOfWriteInsPlusOne = maximumNumberOfWriteInsPlusOne;
			return this;
		}

		public Builder withVerificationCardSets(final ImmutableList<VerificationCardSetSummary> verificationCardSets) {
			this.verificationCardSets = verificationCardSets;
			return this;
		}

		public PreconfigureSummary build() {
			return new PreconfigureSummary(encryptionGroup, maximumNumberOfVotingOptions, maximumNumberOfSelections, maximumNumberOfWriteInsPlusOne,
					verificationCardSets);
		}
	}
}
