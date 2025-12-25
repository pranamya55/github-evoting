/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class OnlineApplicationStartupListener {

	private final OnlineWorkflowResumeService onlineWorkflowResumeService;

	public OnlineApplicationStartupListener(final OnlineWorkflowResumeService onlineWorkflowResumeService) {
		this.onlineWorkflowResumeService = onlineWorkflowResumeService;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationEvent() {
		onlineWorkflowResumeService.resumeWorkflow();
	}

}
