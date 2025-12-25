/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.hasNoDuplicates;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;

/**
 * Regroups the context values needed by the CombineEncLongCodeShares algorithm.
 *
 * <ul>
 *     <li>(p, q, g), the encryption group. Non-null.</li>
 *     <li>ee, the election event ID. Non-null and a valid UUID.</li>
 *     <li>vcs, the verification card set ID. Non-null and a valid UUID.</li>
 *     <li>vc, the vector of verification card IDs. Non-null and a list of valid UUIDs.</li>
 *     <li>n, the number of voting options for the verification card set. In range [1, n<sub>max</sub>].</li>
 *     <li>n<sub>max</sub>, the maximum number of voting options. In range [1, n<sub>sup</sub>].</li>
 * </ul>
 */
public class CombineEncLongCodeSharesContext {

	private final GqGroup encryptionGroup;
	private final String electionEventId;
	private final String verificationCardSetId;
	private final ImmutableList<String> verificationCardIds;
	private final int numberOfVotingOptions;
	private final int maximumNumberOfVotingOptions;

	private CombineEncLongCodeSharesContext(final GqGroup encryptionGroup, final String electionEventId, final String verificationCardSetId,
			final ImmutableList<String> verificationCardIds, final int numberOfVotingOptions, final int maximumNumberOfVotingOptions) {
		this.encryptionGroup = encryptionGroup;
		this.electionEventId = electionEventId;
		this.verificationCardSetId = verificationCardSetId;
		this.verificationCardIds = verificationCardIds;
		this.numberOfVotingOptions = numberOfVotingOptions;
		this.maximumNumberOfVotingOptions = maximumNumberOfVotingOptions;
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
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

	public int getMaximumNumberOfVotingOptions() {
		return maximumNumberOfVotingOptions;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final CombineEncLongCodeSharesContext that = (CombineEncLongCodeSharesContext) o;
		return numberOfVotingOptions == that.numberOfVotingOptions && maximumNumberOfVotingOptions == that.maximumNumberOfVotingOptions
				&& encryptionGroup.equals(that.encryptionGroup) && electionEventId.equals(that.electionEventId) && verificationCardSetId.equals(
				that.verificationCardSetId) && verificationCardIds.equals(that.verificationCardIds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptionGroup, electionEventId, verificationCardSetId, verificationCardIds, numberOfVotingOptions,
				maximumNumberOfVotingOptions);
	}

	/**
	 * Builder performing input validations before constructing a {@link CombineEncLongCodeSharesContext}.
	 */
	public static class Builder {

		private GqGroup encryptionGroup;
		private String electionEventId;
		private String verificationCardSetId;
		private ImmutableList<String> verificationCardIds;
		private int numberOfVotingOptions;
		private int maximumNumberOfVotingOptions;

		public Builder setEncryptionGroup(final GqGroup encryptionGroup) {
			this.encryptionGroup = encryptionGroup;
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

		public Builder setMaximumNumberOfVotingOptions(final int maximumNumberOfVotingOptions) {
			this.maximumNumberOfVotingOptions = maximumNumberOfVotingOptions;
			return this;
		}

		/**
		 * Creates the CombineEncLongCodeSharesContext. All fields must have been set and be non-null.
		 *
		 * @return a new CombineEncLongCodeSharesContext.
		 * @throws NullPointerException      if any of the fields is null.
		 * @throws FailedValidationException if {@code electionEventId}, {@code verificationCardSetId} or any id in {@code verificationCardIds} are
		 *                                   invalid UUID.
		 * @throws IllegalArgumentException  if
		 *                                   <ul>
		 *                                       <li>the verification card id list has duplicates.</li>
		 *                                       <li>the verification card id list is empty.</li>
		 *                                       <li>the maximum number of voting options is smaller or equal to zero.</li>
		 *                                       <li>the maximum number of voting options is strictly greater than {@value VotingOptionsConstants#MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS}.</li>
		 *                                       <li>the number of voting options is smaller or equal to zero.</li>
		 *                                       <li>the number of voting options is strictly greater than the maximum number of voting options.</li>
		 *                                   </ul>
		 */
		public CombineEncLongCodeSharesContext build() {
			checkNotNull(encryptionGroup);
			validateUUID(electionEventId);
			validateUUID(verificationCardSetId);
			checkNotNull(verificationCardIds).stream().parallel().forEach(Validations::validateUUID);
			checkArgument(hasNoDuplicates(verificationCardIds), "The verification card id list must not have duplicates.");
			checkArgument(!verificationCardIds.isEmpty(), "The vector of verification card ids must have at least one element.");

			checkArgument(maximumNumberOfVotingOptions > 0, "The maximum number of voting options must be strictly positive.");
			checkArgument(maximumNumberOfVotingOptions <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
					"The maximum number of voting options must be smaller or equal to the maximum supported number of voting options. [n_max: %s, n_sup: %s]",
					maximumNumberOfVotingOptions, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);

			checkArgument(numberOfVotingOptions > 0, "The number of voting options must be strictly positive.");
			checkArgument(numberOfVotingOptions <= maximumNumberOfVotingOptions,
					"The number of voting options must be smaller or equal to the maximum number of voting options. [n: %s, n_max: %s]",
					numberOfVotingOptions, maximumNumberOfVotingOptions);

			return new CombineEncLongCodeSharesContext(encryptionGroup, electionEventId, verificationCardSetId, verificationCardIds,
					numberOfVotingOptions, maximumNumberOfVotingOptions);
		}
	}
}
