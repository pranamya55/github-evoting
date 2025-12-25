/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.workflow;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "WORKFLOW_LOG")
public class WorkflowLog {
	@Id
	@Column(name = "WORKFLOW_LOG_ID")
	private String id;

	@Column(name = "WORKFLOW_STEP")
	private String workflowStep;

	@Column(name = "STATUS")
	private String status;

	@Column(name = "CONTEXT_ID")
	private String contextId;

	@Column(name = "EXCEPTION_CODE")
	private String exceptionCode;

	@Column(name = "TIMESTAMP")
	private Instant timestamp;

	@Version
	private int changeControlId;

	public WorkflowLog() {
	}

	public WorkflowLog(final String id, final String workflowStep, final String status, final String contextId, final String exceptionCode,
			final Instant timestamp) {
		this.id = id;
		this.workflowStep = workflowStep;
		this.status = status;
		this.contextId = contextId;
		this.exceptionCode = exceptionCode;
		this.timestamp = timestamp;
	}

	public String getId() {
		return id;
	}

	public String getWorkflowStep() {
		return workflowStep;
	}

	public String getStatus() {
		return status;
	}

	public String getContextId() {
		return contextId;
	}

	public String getExceptionCode() {
		return exceptionCode;
	}

	public Instant getTimestamp() {
		return timestamp;
	}
}
