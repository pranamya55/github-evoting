/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary.preconfigure;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;
import static com.google.common.base.Preconditions.checkArgument;

import ch.post.it.evoting.evotinglibraries.domain.validations.GracePeriodValidation;

public class VerificationCardSetSummary {

	private final String verificationCardSetAlias;
	private final boolean testBallotBox;
	private final int numberOfEligibleVoters;
	private final int numberOfVotingOptions;
	private final int gracePeriod;

	private VerificationCardSetSummary(final String verificationCardSetAlias, final boolean testBallotBox, final int numberOfEligibleVoters,
			final int numberOfVotingOptions, final int gracePeriod) {
		this.verificationCardSetAlias = validateXsToken(verificationCardSetAlias);
		this.testBallotBox = testBallotBox;
		checkArgument(numberOfEligibleVoters > 0, "The number of eligible voters must be strictly positive.");
		this.numberOfEligibleVoters = numberOfEligibleVoters;
		checkArgument(numberOfVotingOptions > 0, "The number of voting options must be strictly positive.");
		this.numberOfVotingOptions = numberOfVotingOptions;
		this.gracePeriod = GracePeriodValidation.validate(gracePeriod);
	}

	public String getVerificationCardSetAlias() {
		return verificationCardSetAlias;
	}

	public boolean isTestBallotBox() {
		return testBallotBox;
	}

	public int getNumberOfEligibleVoters() {
		return numberOfEligibleVoters;
	}

	public int getNumberOfVotingOptions() {
		return numberOfVotingOptions;
	}

	public int getGracePeriod() {
		return gracePeriod;
	}

	public static class Builder {
		private String verificationCardSetAlias;
		private boolean testBallotBox;
		private int numberOfEligibleVoters;
		private int numberOfVotingOptions;
		private int gracePeriod;

		public Builder setVerificationCardSetAlias(final String verificationCardSetAlias) {
			this.verificationCardSetAlias = verificationCardSetAlias;
			return this;
		}

		public Builder setTestBallotBox(final boolean testBallotBox) {
			this.testBallotBox = testBallotBox;
			return this;
		}

		public Builder setNumberOfEligibleVoters(final int numberOfEligibleVoters) {
			this.numberOfEligibleVoters = numberOfEligibleVoters;
			return this;
		}

		public Builder setNumberOfVotingOptions(final int numberOfVotingOptions) {
			this.numberOfVotingOptions = numberOfVotingOptions;
			return this;
		}

		public Builder setGracePeriod(final int gracePeriod) {
			this.gracePeriod = gracePeriod;
			return this;
		}

		public VerificationCardSetSummary build() {
			return new VerificationCardSetSummary(verificationCardSetAlias, testBallotBox, numberOfEligibleVoters, numberOfVotingOptions,
					gracePeriod);
		}
	}
}
