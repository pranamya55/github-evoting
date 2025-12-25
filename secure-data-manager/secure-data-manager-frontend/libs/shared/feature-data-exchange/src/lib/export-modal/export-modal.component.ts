/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, DestroyRef, inject, Input, OnInit} from '@angular/core';

import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {TranslateModule} from '@ngx-translate/core';
import {ExportInformationComponent} from '../export-information/export-information.component';
import {WorkflowExceptionCode, WorkflowStatus, WorkflowStepUtil,} from '@sdm/shared-util-types';
import {WorkflowStateService} from '@sdm/shared-ui-services';
import {DataExchangeService} from '../data-exchange.service';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';

@Component({
	selector: 'sdm-export-modal',
	standalone: true,
	imports: [ExportInformationComponent, TranslateModule],
	templateUrl: './export-modal.component.html',
})
export class ExportModalComponent implements OnInit {
	@Input() exchangeIndex!: string;
	exportStatus!: WorkflowStatus;
	errorFeedbackCode!: WorkflowExceptionCode;
	destroyRef = inject(DestroyRef)
	readonly activeModal = inject(NgbActiveModal);
	private readonly workflowStates = inject(WorkflowStateService);
	private readonly dataExchangeService = inject(DataExchangeService);
	protected readonly WorkflowStatus = WorkflowStatus;

	ngOnInit(): void {
		this.workflowStates
			.get(WorkflowStepUtil.getExportStep(parseInt(this.exchangeIndex)))
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe((state) => {
				this.exportStatus = state.status;
				this.errorFeedbackCode =
					state.exceptionCode ?? WorkflowExceptionCode.None;
			});
		this.dataExchangeService.export(this.exchangeIndex).subscribe();
	}

	get hasErrorFeedback(): boolean {
		return (
			this.errorFeedbackCode !== null &&
			this.errorFeedbackCode !== WorkflowExceptionCode.None &&
			this.errorFeedbackCode !== WorkflowExceptionCode.Default
		);
	}


}
