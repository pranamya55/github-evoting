/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.tally.mixdecrypt;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
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
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

@Entity
@IdClass(BallotBoxPrimaryKey.class)
@Table(name = "CONTROL_COMPONENT_VOTES_HASH_PAYLOAD")
public class ControlComponentVotesHashPayloadEntity {

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
	private ImmutableByteArray controlComponentVotesHashPayload;

	@Version
	private int changeControlId;

	protected ControlComponentVotesHashPayloadEntity() {
		// Intentionally left blank.
	}

	public ControlComponentVotesHashPayloadEntity(final String electionEventId, final String ballotBoxId, final int nodeId,
			final ImmutableByteArray controlComponentVotesHashPayload) {
		this.electionEventId = validateUUID(electionEventId);
		this.ballotBoxId = validateUUID(ballotBoxId);
		this.nodeId = nodeId;
		this.controlComponentVotesHashPayload = checkNotNull(controlComponentVotesHashPayload);

		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);
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

	public ImmutableByteArray getControlComponentVotesHashPayload() {
		return controlComponentVotesHashPayload;
	}

}
