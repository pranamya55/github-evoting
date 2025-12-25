/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {HttpClient} from '@angular/common/http';
import {Injectable, NgZone} from '@angular/core';
import {WorkflowState, WorkflowStep} from '@sdm/shared-util-types';
import {distinctUntilChanged, filter, from, map, merge, Observable, ReplaySubject, scan, share, shareReplay, switchMap, tap} from 'rxjs';
import {environment} from "@sdm/shared-ui-config";

function getKey<T extends { step: WorkflowStep, contextId?: WorkflowState['contextId'] }>(state: T): string {
	return state.contextId ? `${state.step}-${state.contextId}` : state.step;
}

@Injectable({
	providedIn: 'root',
})
export class WorkflowStateService {
	private readonly workflowStates!: Observable<Map<string, WorkflowState>>;
	private readonly workflowStateListSource = new ReplaySubject<WorkflowState[]>();

	constructor(
		private readonly ngZone: NgZone,
		private readonly http: HttpClient,
	) {
		if (!environment.workflowEnabled) return;

		// Start by getting the status of all steps
		const initialStates$ = this.getSnapshot()
			.pipe(switchMap((stateList) => from(stateList)));

		//
		const workflowStateListUpdates$ = this.workflowStateListSource
			.asObservable()
			.pipe(switchMap((stateList) => from(stateList)));

		// Connect to the status updates (SSE)
		const notificationStateUpdates$ = this.getWorkflowStates().pipe(share());

		this.workflowStates = merge(initialStates$, workflowStateListUpdates$, notificationStateUpdates$)
			.pipe(
				// Create a map of the last emitted state for each step/contextId pair
				scan((stateByStep, state) => {
					stateByStep.set(getKey(state), state);
					return stateByStep;
				}, new Map<string, WorkflowState>),

				// replay the latest map so that each new subscriber receives an initial state
				shareReplay(1),
			);
	}

	/**
	 * Returns a stream of states for a given step (or step/contextId pair)
	 * @param step
	 * @param contextId
	 */
	get(step: WorkflowStep, contextId?: WorkflowState['contextId']): Observable<WorkflowState> {
		return this.workflowStates.pipe(
			map(stateByStep => stateByStep.get(getKey({step, contextId}))),
			filter((state): state is WorkflowState => !!state),
			distinctUntilChanged((prev, next) => prev.status === next.status),
		);
	}

	getAllMerged(steps: WorkflowStep[], contextId?: WorkflowState['contextId']): Observable<WorkflowState> {
		return merge(...steps.map((step) => this.get(step, contextId)));
	}

	/**
	 * Returns a list of the up-to-date states of all steps
	 */
	getSnapshot(): Observable<WorkflowState[]> {
		return this.http.get<WorkflowState[]>(`${environment.backendPath}/sdm-shared/workflow/state`)
			.pipe(
				tap((stateList) => {
					this.workflowStateListSource.next(stateList);
				})
			);
	}

	private getWorkflowStates(): Observable<WorkflowState> {
		return new Observable((subscriber) => {
			const eventSource = new EventSource(`${environment.backendPath}/sdm-shared/workflow/subscribe`);

			eventSource.onmessage = (message: MessageEvent<string>) => {
				this.ngZone.run(() => {
					subscriber.next(JSON.parse(message.data));
				});
			};

			eventSource.onerror = (error) => {
				this.ngZone.run(() => {
					subscriber.error(error);
				});
			};
		});
	}
}
