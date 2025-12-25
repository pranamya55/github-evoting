/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.workflow;

import java.time.Instant;

public record WorkflowState(WorkflowStep step, Instant startTimestamp, Instant endTimestamp, WorkflowStatus status, String contextId,
							WorkflowExceptionCode exceptionCode, boolean optional) {
}
