/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

public class ElectionSummary {

	private final String electionId;
	private final int electionPosition;
	private final int electionType;
	private final int primarySecondaryType;
	private final ImmutableList<DescriptionSummary> electionDescription;
	private final int numberOfMandates;
	private final boolean writeInsAllowed;
	private final int candidateAccumulation;
	private final ImmutableList<CandidateSummary> candidates;
	private final ImmutableList<ListSummary> lists;
	private final ImmutableList<ListUnionSummary> listUnions;

	private ElectionSummary(final String electionId, final int electionPosition, final int electionType, final int primarySecondaryType,
			final ImmutableList<DescriptionSummary> electionDescription, final int numberOfMandates, final boolean writeInsAllowed,
			final int candidateAccumulation, final ImmutableList<CandidateSummary> candidates, final ImmutableList<ListSummary> lists,
			final ImmutableList<ListUnionSummary> listUnions) {
		this.electionId = validateXsToken(electionId);
		checkArgument(electionPosition >= 0, "The election position must be positive.");
		this.electionPosition = electionPosition;
		checkArgument(electionType >= 0, "The election type must be positive.");
		this.electionType = electionType;
		checkArgument(0 <= primarySecondaryType && primarySecondaryType <= 2, "The primary secondary type must be equal to 0, 1 or 2.");
		this.primarySecondaryType = primarySecondaryType;
		this.electionDescription = checkNotNull(electionDescription);
		checkArgument(numberOfMandates > 0, "The number of mandates must be strictly positive.");
		this.numberOfMandates = numberOfMandates;
		this.writeInsAllowed = writeInsAllowed;
		checkArgument(candidateAccumulation >= 0, "The candidate accumulation must be positive.");
		this.candidateAccumulation = candidateAccumulation;
		this.candidates = checkNotNull(candidates);
		this.lists = checkNotNull(lists);
		this.listUnions = checkNotNull(listUnions);
	}

	public String getElectionId() {
		return electionId;
	}

	public int getElectionPosition() {
		return electionPosition;
	}

	public int getElectionType() {
		return electionType;
	}

	public int getPrimarySecondaryType() {
		return primarySecondaryType;
	}

	public ImmutableList<DescriptionSummary> getElectionDescription() {
		return electionDescription;
	}

	public int getNumberOfMandates() {
		return numberOfMandates;
	}

	public boolean isWriteInsAllowed() {
		return writeInsAllowed;
	}

	public int getCandidateAccumulation() {
		return candidateAccumulation;
	}

	public ImmutableList<CandidateSummary> getCandidates() {
		return candidates;
	}

	public ImmutableList<ListSummary> getLists() {
		return lists;
	}

	public ImmutableList<ListUnionSummary> getListUnions() {
		return listUnions;
	}

	public static class Builder {

		private String electionId;
		private int electionPosition;
		private int electionType;
		private int primarySecondaryType;
		private ImmutableList<DescriptionSummary> electionDescription;
		private int numberOfMandates;
		private boolean writeInsAllowed;
		private int candidateAccumulation;
		private ImmutableList<CandidateSummary> candidates;
		private ImmutableList<ListSummary> lists;
		private ImmutableList<ListUnionSummary> listUnions;

		public Builder electionId(final String electionId) {
			this.electionId = electionId;
			return this;
		}

		public Builder electionPosition(final int position) {
			this.electionPosition = position;
			return this;
		}

		public Builder electionType(final int electionType) {
			this.electionType = electionType;
			return this;
		}

		/**
		 * Sets the primary secondary type.
		 *
		 * @param primarySecondaryType the primary secondary type. 1 if primary, 2 if secondary, 0 if none.
		 */
		public Builder primarySecondaryType(final int primarySecondaryType) {
			this.primarySecondaryType = primarySecondaryType;
			return this;
		}

		public Builder electionDescription(final ImmutableList<DescriptionSummary> electionDescription) {
			this.electionDescription = electionDescription;
			return this;
		}

		public Builder numberOfMandates(final int numberOfMandates) {
			this.numberOfMandates = numberOfMandates;
			return this;
		}

		public Builder writeInsAllowed(final boolean writeInsAllowed) {
			this.writeInsAllowed = writeInsAllowed;
			return this;
		}

		public Builder candidateAccumulation(final int candidateAccumulation) {
			this.candidateAccumulation = candidateAccumulation;
			return this;
		}

		public Builder candidates(final ImmutableList<CandidateSummary> candidates) {
			this.candidates = candidates;
			return this;
		}

		public Builder lists(final ImmutableList<ListSummary> lists) {
			this.lists = lists;
			return this;
		}

		public Builder listUnions(final ImmutableList<ListUnionSummary> listUnions) {
			this.listUnions = listUnions;
			return this;
		}

		public ElectionSummary build() {
			return new ElectionSummary(electionId, electionPosition, electionType, primarySecondaryType, electionDescription, numberOfMandates,
					writeInsAllowed, candidateAccumulation, candidates, lists, listUnions);
		}
	}
}
