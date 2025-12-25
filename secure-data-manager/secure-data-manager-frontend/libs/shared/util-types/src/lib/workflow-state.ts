/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {WorkflowStatus} from './workflow-status';
import {WorkflowStep} from './workflow-step';
import {WorkflowExceptionCode} from './workflow-exception-code';

export interface WorkflowState {
	step: WorkflowStep;
	contextId?: string;
	status: WorkflowStatus;
	startTimestamp: string | null;
	endTimestamp: string | null;
	exceptionCode?: WorkflowExceptionCode;
	optional: boolean;
}
