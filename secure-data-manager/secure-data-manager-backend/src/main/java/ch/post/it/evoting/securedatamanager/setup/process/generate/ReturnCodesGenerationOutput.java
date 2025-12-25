/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generate;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Regroups the outputs produced by the {@link VerificationCardSetDataGenerationService}.
 */
@SuppressWarnings("java:S115")
public class ReturnCodesGenerationOutput {

	private final String electionEventId;
	private final String verificationCardSetId;
	private final ImmutableList<String> verificationCardIds;
	private final ImmutableList<String> shortVoteCastReturnCodes;
	private final ImmutableList<String> longVoteCastReturnCodesAllowList;
	private final ImmutableMap<String, String> returnCodesMappingTable;
	private final ImmutableList<ImmutableList<String>> shortChoiceReturnCodes;

	/**
	 * @throws NullPointerException      if any of the fields is null.
	 * @throws FailedValidationException if {@code verificationCardSetId} is invalid.
	 */
	private ReturnCodesGenerationOutput(final String electionEventId, final String verificationCardSetId,
			final ImmutableList<String> verificationCardIds, final ImmutableList<String> shortVoteCastReturnCodes,
			final ImmutableList<String> longVoteCastReturnCodesAllowList, final ImmutableMap<String, String> returnCodesMappingTable,
			final ImmutableList<ImmutableList<String>> shortChoiceReturnCodes) {

		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkNotNull(verificationCardIds);
		checkNotNull(shortVoteCastReturnCodes);
		checkNotNull(longVoteCastReturnCodesAllowList);
		checkNotNull(returnCodesMappingTable);
		checkNotNull(shortChoiceReturnCodes);

		this.electionEventId = electionEventId;
		this.verificationCardSetId = verificationCardSetId;
		this.verificationCardIds = verificationCardIds;
		this.shortVoteCastReturnCodes = shortVoteCastReturnCodes;
		this.longVoteCastReturnCodesAllowList = longVoteCastReturnCodesAllowList;
		this.returnCodesMappingTable = returnCodesMappingTable;
		this.shortChoiceReturnCodes = shortChoiceReturnCodes;
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

	public ImmutableList<String> getShortVoteCastReturnCodes() {
		return shortVoteCastReturnCodes;
	}

	public ImmutableList<String> getLongVoteCastReturnCodesAllowList() {
		return longVoteCastReturnCodesAllowList;
	}

	public ImmutableMap<String, String> getReturnCodesMappingTable() {
		return returnCodesMappingTable;
	}

	public ImmutableList<ImmutableList<String>> getShortChoiceReturnCodes() {
		return shortChoiceReturnCodes;
	}

	public static class Builder {
		private String electionEventId;
		private String verificationCardSetId;
		private ImmutableList<String> verificationCardIds;
		private ImmutableList<String> shortVoteCastReturnCodes;
		private ImmutableList<String> longVoteCastReturnCodesAllowList;
		private ImmutableMap<String, String> returnCodesMappingTable;
		private ImmutableList<ImmutableList<String>> shortChoiceReturnCodes;

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

		public Builder setShortVoteCastReturnCodes(final ImmutableList<String> shortVoteCastReturnCodes) {
			this.shortVoteCastReturnCodes = shortVoteCastReturnCodes;
			return this;
		}

		public Builder setLongVoteCastReturnCodesAllowList(final ImmutableList<String> longVoteCastReturnCodesAllowList) {
			this.longVoteCastReturnCodesAllowList = longVoteCastReturnCodesAllowList;
			return this;
		}

		public Builder setReturnCodesMappingTable(final ImmutableMap<String, String> returnCodesMappingTable) {
			this.returnCodesMappingTable = returnCodesMappingTable;
			return this;
		}

		public Builder setShortChoiceReturnCodes(final ImmutableList<ImmutableList<String>> shortChoiceReturnCodes) {
			this.shortChoiceReturnCodes = shortChoiceReturnCodes;
			return this;
		}

		public ReturnCodesGenerationOutput build() {
			return new ReturnCodesGenerationOutput(electionEventId, verificationCardSetId, verificationCardIds, shortVoteCastReturnCodes,
					longVoteCastReturnCodesAllowList, returnCodesMappingTable, shortChoiceReturnCodes);
		}
	}
}
