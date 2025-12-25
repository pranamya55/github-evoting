/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateNonBlankUCS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

public class AuthorizationSummary {

	private final String authorizationId;
	private final String authorizationName;
	private final boolean isTest;
	private final LocalDateTime fromDate;
	private final LocalDateTime toDate;
	private final long numberOfVoters;
	private final ImmutableList<AuthorizationObjectSummary> authorizationObjects;

	private AuthorizationSummary(final String authorizationId, final String authorizationName, final boolean isTest,
			final LocalDateTime fromDate, final LocalDateTime toDate, final long numberOfVoters,
			final ImmutableList<AuthorizationObjectSummary> authorizationObjects) {
		this.authorizationId = validateXsToken(authorizationId);
		this.authorizationName = validateNonBlankUCS(authorizationName);
		this.isTest = isTest;
		checkNotNull(fromDate);
		checkNotNull(toDate);
		checkArgument(fromDate.isBefore(toDate) || fromDate.equals(toDate), "The fromDate must not be after the toDate.");
		this.fromDate = fromDate;
		this.toDate = toDate;
		checkArgument(numberOfVoters > 0, "The number of voters must be strictly positive.");
		this.numberOfVoters = numberOfVoters;
		this.authorizationObjects = checkNotNull(authorizationObjects);
	}

	public String getAuthorizationId() {
		return authorizationId;
	}

	public String getAuthorizationName() {
		return authorizationName;
	}

	@JsonProperty("isTest")
	public boolean isTest() {
		return isTest;
	}

	public LocalDateTime getFromDate() {
		return fromDate;
	}

	public LocalDateTime getToDate() {
		return toDate;
	}

	public long getNumberOfVoters() {
		return numberOfVoters;
	}

	public ImmutableList<AuthorizationObjectSummary> getAuthorizationObjects() {
		return authorizationObjects;
	}

	public static class Builder {

		private String authorizationId;
		private String authorizationName;
		private boolean isTest;
		private LocalDateTime fromDate;
		private LocalDateTime toDate;
		private long numberOfVoters;
		private ImmutableList<AuthorizationObjectSummary> authorizationObjects;

		public Builder authorizationId(final String authorizationId) {
			this.authorizationId = authorizationId;
			return this;
		}

		public Builder authorizationName(final String authorization) {
			this.authorizationName = authorization;
			return this;
		}

		public Builder isTest(final boolean isTest) {
			this.isTest = isTest;
			return this;
		}

		public Builder fromDate(final LocalDateTime fromDate) {
			this.fromDate = fromDate;
			return this;
		}

		public Builder toDate(final LocalDateTime toDate) {
			this.toDate = toDate;
			return this;
		}

		public Builder numberOfVoters(final long numberOfVoters) {
			this.numberOfVoters = numberOfVoters;
			return this;
		}

		public Builder authorizationObjects(final ImmutableList<AuthorizationObjectSummary> authorizationObjects) {
			this.authorizationObjects = authorizationObjects;
			return this;
		}

		public AuthorizationSummary build() {
			return new AuthorizationSummary(authorizationId, authorizationName, isTest, fromDate, toDate, numberOfVoters,
					authorizationObjects);
		}
	}
}
