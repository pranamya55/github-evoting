/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.evotinglibraries.domain.validations.GracePeriodValidation;

@Entity
@Table(name = "BALLOT_BOX")
public class BallotBoxEntity {

	@Id
	@Column(name = "BALLOT_BOX_ID")
	private String ballotBoxId;

	@ManyToOne
	@JoinColumn(name = "ELECTION_EVENT_FK_ID", referencedColumnName = "ELECTION_EVENT_ID")
	private ElectionEventEntity electionEventEntity;

	@Column(name = "DESCRIPTION")
	private String description;

	@Column(name = "START_TIME")
	private LocalDateTime startTime;

	@Column(name = "FINISH_TIME")
	private LocalDateTime finishTime;

	@Column(name = "GRACE_PERIOD")
	private int gracePeriod;

	@Column(name = "TEST")
	private boolean test;

	@Version
	private int changeControlId;

	public BallotBoxEntity() {
	}

	public BallotBoxEntity(final String ballotBoxId, final ElectionEventEntity electionEventEntity, final String description, final LocalDateTime startTime, final LocalDateTime finishTime, final int gracePeriod,
			final boolean test) {
		this.ballotBoxId = ballotBoxId;
		this.electionEventEntity = electionEventEntity;
		this.description = description;
		this.startTime = startTime;
		this.finishTime = finishTime;
		this.gracePeriod = GracePeriodValidation.validate(gracePeriod);
		this.test = test;
	}

	public String getBallotBoxId() {
		return ballotBoxId;
	}

	public ElectionEventEntity getElectionEventEntity() {
		return electionEventEntity;
	}

	public String getDescription() {
		return description;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public LocalDateTime getFinishTime() {
		return finishTime;
	}

	public int getGracePeriod() {
		return gracePeriod;
	}


	public boolean isTest() {
		return test;
	}
}
