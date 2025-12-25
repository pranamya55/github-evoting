/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component, Input} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {NgbProgressbar} from '@ng-bootstrap/ng-bootstrap';
import {TranslateModule} from '@ngx-translate/core';
import {WorkflowStateService} from '@sdm/shared-ui-services';
import {WorkflowState, WorkflowStatus} from '@sdm/shared-util-types';
import {filter, Observable, switchMap, take, takeUntil, tap, timer} from 'rxjs';
import {MetricService} from './metric.service';

@Component({
	selector: 'sdm-progress',
	standalone: true,
	imports: [CommonModule, TranslateModule, NgbProgressbar],
	templateUrl: './progress.component.html',
})
export class ProgressComponent {
	@Input() displayMetrics = true;
	workflowState$?: Observable<WorkflowState>;
	processorName$: Observable<string>;
	usedMemoryPercentage = 0;
	usedCpuPercentage = 0;
	runTime = 0;

	constructor(
		readonly metricService: MetricService,
		readonly workflowStates: WorkflowStateService,
		readonly route: ActivatedRoute,
	) {
		this.processorName$ = metricService.getProcessorName();

		const step = this.route.snapshot.data['workflowStep'];
		if (!step) return;

		this.workflowState$ = this.workflowStates.get(step);

		const progressStart$ = this.workflowState$.pipe(
			filter((state) => state.status === WorkflowStatus.InProgress),
			tap((state: WorkflowState) => {
				this.runTime = this.getRunTime(state);
			}),
			take(1),
		);

		const progressEnd$ = this.workflowState$.pipe(
			filter(
				(state) =>
					state.status === WorkflowStatus.Complete ||
					state.status === WorkflowStatus.Error,
			),
			take(1),
		);

		// refresh the timer every second
		const progressTimeRefresher$ = progressStart$.pipe(
			switchMap(() => timer(0, 1000)),
			takeUntil(progressEnd$),
		);

		progressTimeRefresher$.subscribe(() => {
			this.runTime += 1000;
		});

		// refresh the metrics every other second
		const metricsRefresher$ = progressStart$.pipe(
			switchMap(() => timer(0, 1000)),
			takeUntil(progressEnd$),
		);

		metricsRefresher$
			.pipe(switchMap(() => metricService.getUsedMemoryPercentage()))
			.subscribe((usedMemoryPercentage) => {
				this.usedMemoryPercentage = usedMemoryPercentage;
			});

		metricsRefresher$
			.pipe(switchMap(() => metricService.getUsedCpuPercentage()))
			.subscribe((usedCpuPercentage) => {
				this.usedCpuPercentage = usedCpuPercentage;
			});

		// reset everything when the step is complete
		progressEnd$.subscribe((state) => {
			this.usedMemoryPercentage = 0;
			this.usedCpuPercentage = 0;

			if (!state.startTimestamp || !state.endTimestamp) return;

			this.runTime = this.getRunTime(state);
		});
	}

	private getRunTime(state: WorkflowState): number {
		const runTimeBase = new Date(2000, 1, 1, 0, 0, 0).getTime();
		const endDate = state.endTimestamp ? new Date(state.endTimestamp) : new Date();
		const startDate = state.startTimestamp ? new Date(state.startTimestamp) : endDate;

		return runTimeBase + (endDate.getTime() - startDate.getTime());
	}
}
