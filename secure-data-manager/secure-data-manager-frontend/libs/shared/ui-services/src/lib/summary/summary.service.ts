/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Injectable} from '@angular/core';
import {environment} from '@sdm/shared-ui-config';
import {HttpClient} from '@angular/common/http';
import {filter, mergeMap, Observable} from 'rxjs';
import {WorkflowStatus, WorkflowStep} from '@sdm/shared-util-types';
import {WorkflowStateService} from "../workflow-states/workflow-state.service";
import {Summary} from "e-voting-libraries-ui-kit";

@Injectable({
	providedIn: 'root',
})
export class SummaryService {

	constructor(private readonly httpClient: HttpClient,
				private readonly workflowStates: WorkflowStateService) {
	}

	getConfigurationSummary(): Observable<Summary> {
		return this.httpClient.get<Summary>(
			`${environment.backendPath}/sdm-shared/summary`,
		);
	}

	getHeaderSummary(): Observable<Summary> {
		return this.workflowStates
			// Depending on the sdm mode, only one will be not EMPTY.
			.getAllMerged([WorkflowStep.PreConfigure, WorkflowStep.ImportFromSetup1, WorkflowStep.ImportFromOnline5])
			.pipe(
				filter((state) => state.status === WorkflowStatus.Complete),
				mergeMap(() => this.getConfigurationSummary())
			);
	}
}
