/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {Component, DestroyRef} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {TranslateModule} from '@ngx-translate/core';
import {NextButtonComponent, PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';
import {WorkflowStatus, WorkflowStep} from '@sdm/shared-util-types';
import {PreConfigureService} from './pre-configure.service';
import {ProgressComponent} from '@sdm/shared-feature-progress';
import {SummaryService, WorkflowStateService} from '@sdm/shared-ui-services';
import {catchError, EMPTY, switchMap} from "rxjs";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {Summary, SummaryComponent} from "e-voting-libraries-ui-kit";

@Component({
	selector: 'sdm-preconfigure',
	standalone: true,
	imports: [
		TranslateModule,
		PageActionsComponent,
		ProgressComponent,
		FormsModule,
		SummaryComponent,
		TranslateModule,
		NextButtonComponent,
		PageTitleComponent
	],
	templateUrl: './pre-configure.component.html',
})
export class PreConfigureComponent {
	loadingSummaryError = false;
	workflowStatus: WorkflowStatus = WorkflowStatus.Idle;
	loadingSummary = false;
	isContestConfirmed = false;
	configurationSummary: Summary | null = null;
	protected readonly WorkflowStatus = WorkflowStatus;

	constructor(
		private readonly preConfigureService: PreConfigureService,
		private readonly summaryService: SummaryService,
		private readonly workflowStates: WorkflowStateService,
		private readonly destroyRef: DestroyRef,
	) {
		this.loadConfigurationSummary();
	}

	reload() {
		this.configurationSummary = null;
		this.loadConfigurationSummary();
	}

	preConfigure() {
		this.preConfigureService.preConfigureElectionEvent();
	}

	private loadConfigurationSummary() {
		this.workflowStates
			.get(WorkflowStep.PreConfigure)
			.pipe(
				switchMap((state) => {
						this.workflowStatus = state.status

						if (state.status === WorkflowStatus.Ready) {
							this.loadingSummary = true;
							this.loadingSummaryError = false;
							return this.preConfigureService.previewSummary();
						}
						if (state.status === WorkflowStatus.Complete) {
							this.loadingSummary = true;
							this.loadingSummaryError = false;
							return this.summaryService.getConfigurationSummary();
						}

						return EMPTY;
					}
				),
				catchError(() => {
					this.loadingSummaryError = true
					this.loadingSummary = false;
					return EMPTY;
				}),
				takeUntilDestroyed(this.destroyRef)
			)
			.subscribe((configurationSummary) => {
				this.configurationSummary = configurationSummary;
				this.loadingSummary = false;
			});
	}
}
