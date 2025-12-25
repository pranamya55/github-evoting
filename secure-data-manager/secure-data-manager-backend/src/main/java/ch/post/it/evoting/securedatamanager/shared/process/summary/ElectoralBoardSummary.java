/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateNonBlankUCS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

public class ElectoralBoardSummary {

	private final String electoralBoardId;
	private final String electoralBoardName;
	private final String electoralBoardDescription;
	private final ImmutableList<String> members;

	private boolean constituted;

	private ElectoralBoardSummary(final String electoralBoardId, final String electoralBoardName, final String electoralBoardDescription,
			final ImmutableList<String> members) {
		this.electoralBoardId = validateXsToken(electoralBoardId);
		this.electoralBoardName = validateNonBlankUCS(electoralBoardName);
		this.electoralBoardDescription = validateNonBlankUCS(electoralBoardDescription);
		checkArgument(!checkNotNull(members).isEmpty(), "The electoral board summary members must not be empty.");
		this.members = members;
	}

	public String getElectoralBoardId() {
		return electoralBoardId;
	}

	public String getElectoralBoardName() {
		return electoralBoardName;
	}

	public String getElectoralBoardDescription() {
		return electoralBoardDescription;
	}

	public ImmutableList<String> getMembers() {
		return members;
	}

	public boolean isConstituted() {
		return constituted;
	}

	public void setConstituted(final boolean constituted) {
		this.constituted = constituted;
	}

	public static class Builder {

		private String electoralBoardId;
		private String electoralBoardName;
		private String electoralBoardDescription;
		private ImmutableList<String> members;

		public Builder electoralBoardId(final String electoralBoardId) {
			this.electoralBoardId = electoralBoardId;
			return this;
		}

		public Builder electoralBoardName(final String electoralBoardName) {
			this.electoralBoardName = electoralBoardName;
			return this;
		}

		public Builder electoralBoardDescription(final String electoralBoardDescription) {
			this.electoralBoardDescription = electoralBoardDescription;
			return this;
		}

		public Builder members(final ImmutableList<String> members) {
			this.members = members;
			return this;
		}

		public ElectoralBoardSummary build() {
			return new ElectoralBoardSummary(electoralBoardId, electoralBoardName, electoralBoardDescription, members);
		}
	}
}
