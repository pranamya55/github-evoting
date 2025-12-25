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
import ch.post.it.evoting.evotinglibraries.domain.validations.CorrectnessInformationValidation;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;

/**
 * Regroups the context values needed by the GenCMTable algorithm.
 *
 * <ul>
 *     <li>(p, q, g), the encryption group. Non-null.</li>
 *     <li>ee, the election event ID. Non-null and a valid UUID.</li>
 *     <li>vc, the vector of verification card ids. Non-null and a list of valid UUIDs.</li>
 *     <li>&tau;, the list of correctness information. Non-null and contains valid correctness information.</li>
 *     <li>n<sub>max</sub>, the maximum number of voting options. In range [1, n<sub>sup</sub>].</li>
 * </ul>
 */
public class GenCMTableContext {

	private final GqGroup encryptionGroup;
	private final String electionEventId;
	private final ImmutableList<String> verificationCardIds;
	private final ImmutableList<String> correctnessInformation;
	private final int maximumNumberOfVotingOptions;

	private GenCMTableContext(final GqGroup encryptionGroup, final String electionEventId, final ImmutableList<String> verificationCardIds,
			final ImmutableList<String> correctnessInformation, final int maximumNumberOfVotingOptions) {
		this.encryptionGroup = encryptionGroup;
		this.electionEventId = electionEventId;
		this.verificationCardIds = verificationCardIds;
		this.correctnessInformation = correctnessInformation;
		this.maximumNumberOfVotingOptions = maximumNumberOfVotingOptions;
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public ImmutableList<String> getVerificationCardIds() {
		return verificationCardIds;
	}

	public ImmutableList<String> getCorrectnessInformation() {
		return correctnessInformation;
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
		final GenCMTableContext that = (GenCMTableContext) o;
		return maximumNumberOfVotingOptions == that.maximumNumberOfVotingOptions && encryptionGroup.equals(that.encryptionGroup)
				&& electionEventId.equals(that.electionEventId) && verificationCardIds.equals(that.verificationCardIds)
				&& correctnessInformation.equals(
				that.correctnessInformation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptionGroup, electionEventId, verificationCardIds, correctnessInformation, maximumNumberOfVotingOptions);
	}

	public static class Builder {

		private GqGroup encryptionGroup;
		private String electionEventId;
		private ImmutableList<String> verificationCardIds;
		private ImmutableList<String> correctnessInformation;
		private int maximumNumberOfVotingOptions;

		public Builder setEncryptionGroup(final GqGroup encryptionGroup) {
			this.encryptionGroup = encryptionGroup;
			return this;
		}

		public Builder setElectionEventId(final String electionEventId) {
			this.electionEventId = electionEventId;
			return this;
		}

		public Builder setVerificationCardIds(final ImmutableList<String> verificationCardIds) {
			this.verificationCardIds = verificationCardIds;
			return this;
		}

		public Builder setCorrectnessInformation(final ImmutableList<String> correctnessInformation) {
			this.correctnessInformation = correctnessInformation;
			return this;
		}

		public Builder setMaximumNumberOfVotingOptions(final int maximumNumberOfVotingOptions) {
			this.maximumNumberOfVotingOptions = maximumNumberOfVotingOptions;
			return this;
		}

		/**
		 * Creates the GenCMTableContext. All fields must have been set and be non-null.
		 *
		 * @throws NullPointerException      if any of the fields is null or {@code correctnessInformation} contains any null.
		 * @throws IllegalArgumentException  if
		 *                                   <ul>
		 *                                       <li>The {@code encryptionGroup} and {@code setupSecretKey} do not have the same group order.</li>
		 *                                       <li>The {@code correctnessInformation} is not in range [1, {@value ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants#MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS}].</li>
		 *                                       <li>The {@code maximumNumberOfVotingOptions} is not in range [1, {@value ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants#MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS}].</li>
		 *                                   </ul>
		 * @throws FailedValidationException if the {@code electionEventId} or {@code verificationCardIds} do not comply with the UUID format.
		 */
		public GenCMTableContext build() {
			checkNotNull(encryptionGroup);
			validateUUID(electionEventId);
			checkNotNull(verificationCardIds).forEach(Validations::validateUUID);
			checkArgument(hasNoDuplicates(verificationCardIds), "All verificationCardIds must be unique.");
			checkArgument(!verificationCardIds.isEmpty(), "The vector of verification card Ids must have at least one element.");

			checkNotNull(correctnessInformation).forEach(CorrectnessInformationValidation::validate);
			checkArgument(!correctnessInformation.isEmpty(), "The correctness information must not be empty.");
			checkArgument(correctnessInformation.size() <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
					"The correctness information must be smaller or equal to the maximum supported number of voting options. [n: %s, n_sup: %s]",
					correctnessInformation.size(), MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);

			checkArgument(maximumNumberOfVotingOptions > 0, "The maximum number of voting options must be strictly positive.");
			checkArgument(maximumNumberOfVotingOptions <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
					"The maximum number of voting options must be smaller or equal to the maximum supported number of voting options. [n_max: %s, n_sup: %s]",
					maximumNumberOfVotingOptions, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);

			return new GenCMTableContext(encryptionGroup, electionEventId, verificationCardIds, correctnessInformation, maximumNumberOfVotingOptions);
		}
	}
}
