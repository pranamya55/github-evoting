/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.messaging;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.converters.ImmutableByteArrayConverter;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

@Entity
@IdClass(InProgressMessageId.class)
public class InProgressMessage {

	@Id
	private String correlationId;

	@Id
	private int nodeId;

	private String requestMessageType;

	private String contextId;

	@Convert(converter = ImmutableByteArrayConverter.class)
	private ImmutableByteArray responsePayload;

	private String responseMessageType;

	public InProgressMessage() {
		//intentionally left blank
	}

	public InProgressMessage(final String correlationId, final int nodeId, final String requestMessageType, final String contextId) {
		checkNotNull(correlationId);
		checkArgument(ControlComponentNode.ids().contains(nodeId));
		checkNotNull(requestMessageType);
		checkNotNull(contextId);

		this.correlationId = correlationId;
		this.nodeId = nodeId;
		this.requestMessageType = requestMessageType;
		this.contextId = contextId;
	}

	public InProgressMessage(final String correlationId, final int nodeId, final ImmutableByteArray responsePayload) {
		this.correlationId = checkNotNull(correlationId);
		this.nodeId = nodeId;
		this.responsePayload = checkNotNull(responsePayload);

		checkArgument(ControlComponentNode.ids().contains(nodeId));
	}

	public ImmutableByteArray getResponsePayload() {
		return responsePayload;
	}

	public void setResponsePayload(final ImmutableByteArray responsePayload) {
		this.responsePayload = checkNotNull(responsePayload);
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public int getNodeId() {
		return nodeId;
	}

	public String getResponseMessageType() {
		return responseMessageType;
	}

	public void setResponseMessageType(final String messageType) {
		this.responseMessageType = messageType;
	}

	public String getRequestMessageType() {
		return requestMessageType;
	}

	public String getContextId() {
		return contextId;
	}
}
