/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateNonBlankUCS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDateTime;

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
public class ElectionEventContextEntity {

	@Id
	private String electionEventId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ELECTION_EVENT_ID", referencedColumnName = "ELECTION_EVENT_ID")
	private ElectionEventEntity electionEventEntity;

	private LocalDateTime startTime;

	private LocalDateTime finishTime;

	private String electionEventAlias;

	private String electionEventDescription;

	@Convert(converter = VotesTextsConverter.class)
	private ImmutableList<VoteTexts> votesTexts;

	@Convert(converter = ElectionsTextsConverter.class)
	private ImmutableList<ElectionTexts> electionsTexts;

	@Version
	private Integer changeControlId;

	public ElectionEventContextEntity() {
	}

	public ElectionEventContextEntity(final ElectionEventEntity electionEventEntity, final LocalDateTime startTime, final LocalDateTime finishTime,
			final String electionEventAlias, final String electionEventDescription, final ImmutableList<VoteTexts> votesTexts,
			final ImmutableList<ElectionTexts> electionsTexts) {
		this.electionEventEntity = checkNotNull(electionEventEntity);
		this.startTime = checkNotNull(startTime);
		this.finishTime = checkNotNull(finishTime);
		checkArgument(startTime.isBefore(finishTime) || startTime.equals(finishTime), "The start time must not be after the finish time.");

		this.electionEventAlias = validateXsToken(electionEventAlias);
		this.electionEventDescription = validateNonBlankUCS(electionEventDescription);

		checkNotNull(votesTexts);
		checkNotNull(electionsTexts);
		checkArgument(!votesTexts.isEmpty() || !electionsTexts.isEmpty(), "There must be at least one vote texts or election texts.");
		this.votesTexts = votesTexts;
		this.electionsTexts = electionsTexts;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public ElectionEventEntity getElectionEventEntity() {
		return electionEventEntity;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public LocalDateTime getFinishTime() {
		return finishTime;
	}

	public String getElectionEventAlias() {
		return electionEventAlias;
	}

	public String getElectionEventDescription() {
		return electionEventDescription;
	}

	public ImmutableList<VoteTexts> getVotesTexts() {
		return votesTexts;
	}

	public ImmutableList<ElectionTexts> getElectionsTexts() {
		return electionsTexts;
	}

}
