/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.workflow;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

@RestController
@RequestMapping("/sdm-shared/workflow")
public class WorkflowController {
	private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowController.class);
	private final WorkflowService workflowService;

	public WorkflowController(
			final WorkflowService workflowService) {
		this.workflowService = workflowService;
	}

	@GetMapping(path = "/subscribe", produces = TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe() {
		LOGGER.info("Subscribing to the workflow...");
		return workflowService.getEmitter();
	}

	@GetMapping(path = "/state")
	public ImmutableList<WorkflowState> getWorkflowStateList() {
		return workflowService.getWorkflowStateList();
	}
}
