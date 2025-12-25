/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.exceptions;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

public record ExceptionPayload(String correlationId, int nodeId, Throwable throwable) {

	public ExceptionPayload {
		checkNotNull(correlationId);
		checkArgument(ControlComponentNode.ids().contains(nodeId));
		checkNotNull(throwable);
	}
}
