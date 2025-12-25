/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.messaging;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

public class InProgressMessageId implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private String correlationId;

	private int nodeId;

	public InProgressMessageId() {
		//Intentionally left blank
	}

	public InProgressMessageId(final String correlationId, final int nodeId) {
		checkNotNull(correlationId);
		checkArgument(ControlComponentNode.ids().contains(nodeId));

		this.correlationId = correlationId;
		this.nodeId = nodeId;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final InProgressMessageId inProgressMessageId = (InProgressMessageId) o;
		return Objects.equals(correlationId, inProgressMessageId.correlationId) &&
				Objects.equals(nodeId, inProgressMessageId.nodeId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(correlationId, nodeId);
	}

	@Override
	public String toString() {
		return "InProgressMessageId{" +
				"correlationId='" + correlationId + '\'' +
				", nodeId=" + nodeId +
				'}';
	}
}
