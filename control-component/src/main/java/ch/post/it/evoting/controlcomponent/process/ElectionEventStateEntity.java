/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "ELECTION_EVENT_STATE")
public class ElectionEventStateEntity {

	@Id
	private String electionEventId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ELECTION_EVENT_ID", referencedColumnName = "ELECTION_EVENT_ID")
	private ElectionEventEntity electionEventEntity;

	private ElectionEventState state = ElectionEventState.INITIAL;

	@Version
	private Integer changeControlId;

	public ElectionEventStateEntity() {
	}

	public ElectionEventStateEntity(final ElectionEventEntity electionEventEntity) {
		this.electionEventEntity = checkNotNull(electionEventEntity);
	}

	public ElectionEventState getState() {
		return state;
	}

	public void setState(final ElectionEventState state) {
		this.state = checkNotNull(state);
	}

}
