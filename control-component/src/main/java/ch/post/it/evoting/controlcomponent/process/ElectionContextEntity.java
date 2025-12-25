/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateNonBlankUCS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.converters.ElectionsTextsConverter;
import ch.post.it.evoting.domain.converters.VotesTextsConverter;
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.ElectionTexts;
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.VoteTexts;

@Entity
@Table(name = "ELECTION_EVENT_CONTEXT")
public class ElectionContextEntity {

	@Id
	private String electionEventId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ELECTION_EVENT_ID", referencedColumnName = "ELECTION_EVENT_ID")
	private ElectionEventEntity electionEventEntity;

	private String electionEventAlias;

	private String electionEventDescription;

	private LocalDateTime startTime;

	private LocalDateTime finishTime;

	private int maxNumberOfVotingOptions;

	private int maxNumberOfSelections;

	private int maxNumberOfWriteInsPlusOne;

	@Convert(converter = VotesTextsConverter.class)
	private ImmutableList<VoteTexts> votesTexts;

	@Convert(converter = ElectionsTextsConverter.class)
	private ImmutableList<ElectionTexts> electionsTexts;

	@Version
	@Column(name = "CHANGE_CONTROL_ID")
	private Integer changeControlId;

	public ElectionContextEntity() {
	}

	private ElectionContextEntity(final ElectionEventEntity electionEventEntity, final String electionEventAlias,
			final String electionEventDescription, final LocalDateTime startTime, final LocalDateTime finishTime, final int maxNumberOfVotingOptions,
			final int maxNumberOfSelections, final int maxNumberOfWriteInsPlusOne, final ImmutableList<VoteTexts> votesTexts,
			final ImmutableList<ElectionTexts> electionsTexts) {

		this.electionEventEntity = checkNotNull(electionEventEntity);
		this.electionEventAlias = validateXsToken(electionEventAlias);
		this.electionEventDescription = validateNonBlankUCS(electionEventDescription);

		this.startTime = checkNotNull(startTime);
		this.finishTime = checkNotNull(finishTime);
		checkArgument(startTime.isBefore(finishTime) || startTime.equals(finishTime), "Start time must be before finish time.");

		checkArgument(maxNumberOfVotingOptions > 0, "The max number of voting options must be strictly positive.");
		checkArgument(maxNumberOfVotingOptions <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
				"The maximum number of voting options must be smaller or equal to the maximum supported number of voting options.");

		checkArgument(maxNumberOfSelections > 0, "The max number of selections must be strictly positive.");
		checkArgument(maxNumberOfSelections <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
				"The maximum number of selections must be smaller or equal to the maximum supported number of selections.");

		checkArgument(maxNumberOfWriteInsPlusOne > 0, "The max number of write-ins + 1 must be strictly positive.");
		checkArgument(maxNumberOfWriteInsPlusOne <= MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1,
				"The maximum number of write-ins + 1 must be smaller or equal to the maximum supported number of write-ins + 1.");

		this.maxNumberOfVotingOptions = maxNumberOfVotingOptions;
		this.maxNumberOfSelections = maxNumberOfSelections;
		this.maxNumberOfWriteInsPlusOne = maxNumberOfWriteInsPlusOne;

		this.votesTexts = checkNotNull(votesTexts);
		this.electionsTexts = checkNotNull(electionsTexts);
		checkArgument(!this.votesTexts.isEmpty() || !this.electionsTexts.isEmpty(), "There must be at least one vote texts or election texts.");
	}

	public String getElectionEventAlias() {
		return electionEventAlias;
	}

	public String getElectionEventDescription() {
		return electionEventDescription;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public LocalDateTime getFinishTime() {
		return finishTime;
	}

	public int getMaxNumberOfVotingOptions() {
		return maxNumberOfVotingOptions;
	}

	public int getMaxNumberOfSelections() {
		return maxNumberOfSelections;
	}

	public int getMaxNumberOfWriteInsPlusOne() {
		return maxNumberOfWriteInsPlusOne;
	}

	public ElectionEventEntity getElectionEventEntity() {
		return electionEventEntity;
	}

	public ImmutableList<VoteTexts> getVotesTexts() {
		return votesTexts;
	}

	public ImmutableList<ElectionTexts> getElectionsTexts() {
		return electionsTexts;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ElectionContextEntity that = (ElectionContextEntity) o;
		return maxNumberOfVotingOptions == that.maxNumberOfVotingOptions && maxNumberOfSelections == that.maxNumberOfSelections
				&& maxNumberOfWriteInsPlusOne == that.maxNumberOfWriteInsPlusOne && Objects.equals(electionEventId, that.electionEventId)
				&& Objects.equals(electionEventEntity, that.electionEventEntity) && Objects.equals(electionEventAlias,
				that.electionEventAlias) && Objects.equals(electionEventDescription, that.electionEventDescription) && Objects.equals(
				startTime, that.startTime) && Objects.equals(finishTime, that.finishTime) && Objects.equals(votesTexts,
				that.votesTexts) && Objects.equals(electionsTexts, that.electionsTexts) && Objects.equals(changeControlId,
				that.changeControlId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(electionEventId, electionEventEntity, electionEventAlias, electionEventDescription, startTime, finishTime,
				maxNumberOfVotingOptions, maxNumberOfSelections, maxNumberOfWriteInsPlusOne, votesTexts, electionsTexts, changeControlId);
	}

	public static class Builder {

		private ElectionEventEntity electionEventEntity;
		private String electionEventAlias;
		private String electionEventDescription;
		private LocalDateTime startTime;
		private LocalDateTime finishTime;
		private int maxNumberOfVotingOptions;
		private int maxNumberOfSelections;
		private int maxNumberOfWriteInsPlusOne;
		private ImmutableList<VoteTexts> votesTexts;
		private ImmutableList<ElectionTexts> electionsTexts;

		public Builder() {
			// Do nothing
		}

		public Builder setElectionEventEntity(final ElectionEventEntity electionEventEntity) {
			this.electionEventEntity = checkNotNull(electionEventEntity);
			return this;
		}

		public Builder setElectionEventAlias(final String electionEventAlias) {
			this.electionEventAlias = electionEventAlias;
			return this;
		}

		public Builder setElectionEventDescription(final String electionEventDescription) {
			this.electionEventDescription = electionEventDescription;
			return this;
		}

		public Builder setStartTime(final LocalDateTime startTime) {
			checkNotNull(startTime);
			this.startTime = startTime;
			return this;
		}

		public Builder setFinishTime(final LocalDateTime finishTime) {
			checkNotNull(finishTime);
			this.finishTime = finishTime;
			return this;
		}

		public Builder setMaxNumberOfVotingOptions(final int maxNumberOfVotingOptions) {
			this.maxNumberOfVotingOptions = maxNumberOfVotingOptions;
			return this;
		}

		public Builder setMaxNumberOfSelections(final int maxNumberOfSelections) {
			this.maxNumberOfSelections = maxNumberOfSelections;
			return this;
		}

		public Builder setMaxNumberOfWriteInsPlusOne(final int maxNumberOfWriteInsPlusOne) {
			this.maxNumberOfWriteInsPlusOne = maxNumberOfWriteInsPlusOne;
			return this;
		}

		public Builder setVotesTexts(final ImmutableList<VoteTexts> votesTexts) {
			this.votesTexts = votesTexts;
			return this;
		}

		public Builder setElectionsTexts(final ImmutableList<ElectionTexts> electionsTexts) {
			this.electionsTexts = electionsTexts;
			return this;
		}

		public ElectionContextEntity build() {
			return new ElectionContextEntity(electionEventEntity, electionEventAlias, electionEventDescription, startTime, finishTime,
					maxNumberOfVotingOptions, maxNumberOfSelections, maxNumberOfWriteInsPlusOne, votesTexts, electionsTexts);
		}
	}
}
