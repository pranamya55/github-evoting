/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {WorkflowStatus} from '@sdm/shared-util-types';
import {DataExchangeService} from '../data-exchange.service';
import {NextButtonComponent, PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';
import {ProgressComponent} from '@sdm/shared-feature-progress';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {WorkflowStateService} from '@sdm/shared-ui-services';
import {ExportInformationComponent} from '../export-information/export-information.component';

@Component({
	selector: 'sdm-export',
	standalone: true,
	imports: [
		CommonModule,
		ReactiveFormsModule,
		TranslateModule,
		PageActionsComponent,
		ProgressComponent,
		ExportInformationComponent,
		NextButtonComponent,
		PageTitleComponent,
	],
	templateUrl: './export.component.html',
})
export class ExportComponent {
	exchangeIndex = '';
	private workflowStatus!: WorkflowStatus;

	constructor(
		private readonly dataExchangeService: DataExchangeService,
		private readonly activatedRoute: ActivatedRoute,
		readonly workflowStates: WorkflowStateService,
	) {
		this.exchangeIndex = this.activatedRoute.snapshot.data['exchangeIndex'];

		const step = this.activatedRoute.snapshot.data['workflowStep'];
		if (!step) return;
		workflowStates
			.get(step)
			.pipe(takeUntilDestroyed())
			.subscribe((state) => {
				this.workflowStatus = state.status;
			});
	}

	export() {
		this.dataExchangeService.export(this.exchangeIndex).subscribe();
	}

	get isComplete(): boolean {
		return this.isCurrentStatus(WorkflowStatus.Complete);
	}

	private isCurrentStatus(status: WorkflowStatus): boolean {
		return this.workflowStatus === status;
	}

	// @ts-ignore
	public importToExportLaptop(): string {
		switch (this.exchangeIndex) {
			case '1':
			case '3':
			case '4':
				return 'assets/setup-online.gif';
			case '2':
				return 'assets/online-setup.gif';
			case '5':
			case '50':
				return 'assets/online-tally.gif';
			default:
				console.error('Exchange index is out of range. [exchangeIndex=' + this.exchangeIndex + ']');
				return '';
		}
	}
}
