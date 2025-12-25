/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateNonBlankUCS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;

public class AuthorizationObjectSummary {

	private final String domainOfInfluenceId;
	private final String domainOfInfluenceType;
	private final String domainOfInfluenceName;
	private final String countingCircleId;
	private final String countingCircleName;

	private AuthorizationObjectSummary(final String domainOfInfluenceId, final String domainOfInfluenceType, final String domainOfInfluenceName,
			final String countingCircleId, final String countingCircleName) {
		this.domainOfInfluenceId = validateXsToken(domainOfInfluenceId);
		this.domainOfInfluenceType = validateNonBlankUCS(domainOfInfluenceType);
		this.domainOfInfluenceName = validateNonBlankUCS(domainOfInfluenceName);
		this.countingCircleId = validateXsToken(countingCircleId);
		this.countingCircleName = validateNonBlankUCS(countingCircleName);
	}

	public String getDomainOfInfluenceId() {
		return domainOfInfluenceId;
	}

	public String getDomainOfInfluenceType() {
		return domainOfInfluenceType;
	}

	public String getDomainOfInfluenceName() {
		return domainOfInfluenceName;
	}

	public String getCountingCircleId() {
		return countingCircleId;
	}

	public String getCountingCircleName() {
		return countingCircleName;
	}

	public static class Builder {

		private String domainOfInfluenceId;
		private String domainOfInfluenceType;
		private String domainOfInfluenceName;
		private String countingCircleId;
		private String countingCircleName;

		public Builder domainOfInfluenceId(final String domainOfInfluenceId) {
			this.domainOfInfluenceId = domainOfInfluenceId;
			return this;
		}

		public Builder domainOfInfluenceType(final String domainOfInfluenceType) {
			this.domainOfInfluenceType = domainOfInfluenceType;
			return this;
		}

		public Builder domainOfInfluenceName(final String domainOfInfluenceName) {
			this.domainOfInfluenceName = domainOfInfluenceName;
			return this;
		}

		public Builder countingCircleId(final String countingCircleId) {
			this.countingCircleId = countingCircleId;
			return this;
		}

		public Builder countingCircleName(final String countingCircleName) {
			this.countingCircleName = countingCircleName;
			return this;
		}

		public AuthorizationObjectSummary build() {
			return new AuthorizationObjectSummary(domainOfInfluenceId, domainOfInfluenceType, domainOfInfluenceName, countingCircleId,
					countingCircleName);
		}
	}
}
