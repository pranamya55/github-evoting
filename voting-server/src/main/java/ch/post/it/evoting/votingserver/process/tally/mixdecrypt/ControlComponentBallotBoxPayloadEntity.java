/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.tally.mixdecrypt;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;

@Entity
@IdClass(BallotBoxPrimaryKey.class)
@Table(name = "CONTROL_COMPONENT_BALLOT_BOX_PAYLOAD")
public class ControlComponentBallotBoxPayloadEntity {

	@Id
	@Column(name = "ELECTION_EVENT_ID")
	private String electionEventId;

	@Id
	@Column(name = "BALLOT_BOX_ID")
	private String ballotBoxId;

	@Id
	@Column(name = "NODE_ID")
	private int nodeId;

	@Column(name = "PAYLOAD")
	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray controlComponentBallotBoxPayload;

	@Version
	private Integer changeControlId;

	protected ControlComponentBallotBoxPayloadEntity() {
	}

	ControlComponentBallotBoxPayloadEntity(final String electionEventId, final String ballotBoxId, final int nodeId,
			final ImmutableByteArray controlComponentBallotBoxPayload) {
		this.electionEventId = validateUUID(electionEventId);
		this.ballotBoxId = validateUUID(ballotBoxId);
		this.nodeId = nodeId;
		this.controlComponentBallotBoxPayload = checkNotNull(controlComponentBallotBoxPayload);
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public String getBallotBoxId() {
		return ballotBoxId;
	}

	public int getNodeId() {
		return nodeId;
	}

	public ImmutableByteArray getControlComponentBallotBoxPayload() {
		return controlComponentBallotBoxPayload;
	}
}
