/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component, DestroyRef, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ActivatedRouteSnapshot, RouterLink} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {WorkflowStateService} from '@sdm/shared-ui-services';
import {SdmRoute, WorkflowStatus} from '@sdm/shared-util-types';
import {environment} from "@sdm/shared-ui-config";

@Component({
	selector: 'sdm-stepper-item',
	standalone: true,
	imports: [CommonModule, TranslateModule, RouterLink],
	templateUrl: './stepper-item.component.html',
})
export class StepperItemComponent implements OnInit, OnChanges {
	@Input({required: true}) activatedRouteSnapshot?: ActivatedRouteSnapshot;
	@Input({required: true}) route!: SdmRoute;
	@Input({required: true}) parent!: SdmRoute;

	readonly WorkflowStatus = WorkflowStatus;

	status?: WorkflowStatus;
	isCurrent = false;
	indicatorClass = '';

	constructor(
		private readonly workflowStates: WorkflowStateService,
		private readonly destroyRef: DestroyRef,
	) {
	}

	ngOnInit(): void {
		const step = this.route.data?.workflowStep;
		if (step && environment.workflowEnabled) {
			this.workflowStates
				.get(step)
				.pipe(takeUntilDestroyed(this.destroyRef))
				.subscribe((workflowState) => {
					this.status = workflowState.status;
					this.updateIndicator();
				});
		}
	}

	ngOnChanges(changes: SimpleChanges): void {
		if (changes['activatedRouteSnapshot']) {
			const step = this.route.data?.workflowStep;
			this.isCurrent =
				!!step && this.activatedRouteSnapshot?.data['workflowStep'] === step;
			this.updateIndicator();
		}
	}

	private updateIndicator() {
		// Note: InProgress status is handled directly in the template.
		switch (this.status) {
			case WorkflowStatus.Idle:
				this.indicatorClass = 'bi-circle text-primary';
				break;
			case WorkflowStatus.Ready:
				this.indicatorClass = `bi-${this.isCurrent ? 'circle-fill' : 'circle'} text-primary`;
				break;
			case WorkflowStatus.Complete:
				this.indicatorClass = `bi-${this.isCurrent ? 'check-circle-fill' : 'check-circle'}`;
				this.indicatorClass += this.isCurrent ? ' text-success' : ' text-primary';
				break;
			case WorkflowStatus.Error:
				this.indicatorClass = `bi-${this.isCurrent ? 'x-circle-fill' : 'x-circle'}`;
				this.indicatorClass += this.isCurrent ? ' text-danger' : ' text-primary';
				break;
		}
	}
}
