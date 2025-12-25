/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain;

public class SharedQueue {

	public static final String VOTING_SERVER_ADDRESS = "voting-server";
	public static final String CONTROL_COMPONENTS_ADDRESS = "control-components";
	public static final String CONTROL_COMPONENT_QUEUE = CONTROL_COMPONENTS_ADDRESS + "::control-component-";

	public static final String MESSAGE_HEADER_NODE_ID = "nodeId";
	public static final String MESSAGE_HEADER_MESSAGE_TYPE = "message_type";
	public static final String MESSAGE_HEADER_TENANT_ID = "tenantId";

	private SharedQueue() {
		// static usage only.
	}
}
