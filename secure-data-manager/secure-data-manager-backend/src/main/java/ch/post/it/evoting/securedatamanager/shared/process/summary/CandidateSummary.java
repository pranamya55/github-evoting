/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateNonBlankUCS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDate;

public class CandidateSummary {

	private final String candidateId;
	private final String familyName;
	private final String firstName;
	private final String callName;
	private final LocalDate dateOfBirth;
	private final boolean isIncumbent;
	private final String referenceOnPosition;
	private final String eligibility;

	private CandidateSummary(final String candidateId, final String familyName, final String firstName, final String callName,
			final LocalDate dateOfBirth, final boolean isIncumbent, final String referenceOnPosition, final String eligibility) {
		this.candidateId = validateXsToken(candidateId);
		this.familyName = validateNonBlankUCS(familyName);
		this.firstName = validateNonBlankUCS(firstName);
		this.callName = validateNonBlankUCS(callName);
		this.dateOfBirth = checkNotNull(dateOfBirth);
		this.isIncumbent = isIncumbent;
		this.referenceOnPosition = validateNonBlankUCS(referenceOnPosition);
		this.eligibility = validateNonBlankUCS(eligibility);
	}

	public String getCandidateId() {
		return candidateId;
	}

	public String getFamilyName() {
		return familyName;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getCallName() {
		return callName;
	}

	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public boolean isIncumbent() {
		return isIncumbent;
	}

	public String getReferenceOnPosition() {
		return referenceOnPosition;
	}

	public String getEligibility() {
		return eligibility;
	}

	public static class Builder {

		private String candidateId;
		private String familyName;
		private String firstName;
		private String callName;
		private LocalDate dateOfBirth;
		private boolean isIncumbent;
		private String referenceOnPosition;
		private String eligibility;

		public Builder candidateId(final String candidateId) {
			this.candidateId = candidateId;
			return this;
		}

		public Builder familyName(final String familyName) {
			this.familyName = familyName;
			return this;
		}

		public Builder firstName(final String firstName) {
			this.firstName = firstName;
			return this;
		}

		public Builder callName(final String callName) {
			this.callName = callName;
			return this;
		}

		public Builder dateOfBirth(final LocalDate dateOfBirth) {
			this.dateOfBirth = dateOfBirth;
			return this;
		}

		public Builder isIncumbent(final boolean isIncumbent) {
			this.isIncumbent = isIncumbent;
			return this;
		}

		public Builder referenceOnPosition(final String referenceOnPosition) {
			this.referenceOnPosition = referenceOnPosition;
			return this;
		}

		public Builder eligibility(final String eligibility) {
			this.eligibility = eligibility;
			return this;
		}

		public CandidateSummary build() {
			return new CandidateSummary(candidateId, familyName, firstName, callName, dateOfBirth, isIncumbent, referenceOnPosition, eligibility);
		}
	}
}
