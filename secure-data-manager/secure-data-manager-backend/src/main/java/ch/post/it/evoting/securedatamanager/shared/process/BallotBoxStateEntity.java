/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "BALLOT_BOX_STATE")
public class BallotBoxStateEntity {

	@Id
	@Column(name = "BALLOT_BOX_STATE_ID")
	private String ballotBoxStateId;

	@Column(name = "STATUS")
	private String status;

	public BallotBoxStateEntity() {
		// Needed by the repository.
	}

	public BallotBoxStateEntity(final String ballotBoxStateId, final String status) {
		this.ballotBoxStateId = ballotBoxStateId;
		this.status = status;
	}

	public String getBallotBoxStateId() {
		return ballotBoxStateId;
	}

	public void setBallotBoxStateId(final String ballotBoxStateId) {
		this.ballotBoxStateId = ballotBoxStateId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}
}
