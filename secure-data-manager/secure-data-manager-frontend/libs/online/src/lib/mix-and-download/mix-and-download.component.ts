/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component, TemplateRef} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {TranslateModule} from '@ngx-translate/core';
import {BallotBoxListComponent} from '@sdm/shared-feature-ballot-box-list';
import {ExportModalComponent} from '@sdm/shared-feature-data-exchange';
import {WorkflowStateService} from '@sdm/shared-ui-services';
import {BallotBox, WorkflowState, WorkflowStatus, WorkflowStep,} from '@sdm/shared-util-types';
import {map, merge, Observable, switchMap, tap} from 'rxjs';
import {MixAndDownloadService} from './mix-and-download.service';
import {NextButtonComponent, PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';

@Component({
	selector: 'sdm-mix-and-download',
	standalone: true,
	imports: [
		CommonModule,
		FormsModule,
		TranslateModule,
		BallotBoxListComponent,
		PageActionsComponent,
		NextButtonComponent,
		PageTitleComponent,
	],
	templateUrl: './mix-and-download.component.html',
})
export class MixAndDownloadComponent {
	readonly WorkflowStatus = WorkflowStatus;
	mixAndDownloadStatus$: Observable<WorkflowStatus>;
	mixableBallotBoxes = new Set<BallotBox>();
	exportableBallotBoxes = new Set<BallotBox>();
	selectedBallotBoxes: BallotBox[] = [];
	private readonly ballotBoxById = new Map<BallotBox['id'], BallotBox>();

	constructor(
		private readonly mixAndDownloadService: MixAndDownloadService,
		private readonly workflowStates: WorkflowStateService,
		private readonly modalService: NgbModal,
		private readonly activatedRoute: ActivatedRoute,
	) {
		this.mixAndDownloadStatus$ = this.workflowStates
			.get(WorkflowStep.MixAndDownload)
			.pipe(map((state) => state.status));

		this.mixAndDownloadService.getBallotBoxes()
			.pipe(
				tap((ballotBoxes) => {
					this.registerBallotBoxes(ballotBoxes);
				}),
				switchMap(ballotBoxes => this.getAllDownloadedBallotBoxStates(ballotBoxes)),
				takeUntilDestroyed()
			)
			.subscribe((state) => {
				this.moveBallotBoxDependingOnState(state);
			});
	}

	get hasSelection(): boolean {
		return this.selectedBallotBoxes.length > 0;
	}

	selectedBallotBoxesCount(test: boolean): number {
		return this.selectedBallotBoxes.filter(b => b.test === test).length;
	}

	openModal(modalTemplate: TemplateRef<null>) {
		this.modalService
			.open(modalTemplate, {
				ariaLabelledBy: 'confirmation-modal-title',
			})
			.result.then((wasMixAndDownloadConfirmed) => {
			if (!wasMixAndDownloadConfirmed) return;
			this.mixAndDownloadService.mixAndDownload(this.selectedBallotBoxes);
			this.selectedBallotBoxes = [];
		});
	}

	exportPartial() {
		const modalRef = this.modalService.open(ExportModalComponent);
		modalRef.componentInstance.exchangeIndex = '50'; // Partial export exchange index
	}

	private registerBallotBoxes(ballotBoxes: BallotBox[]) {
		ballotBoxes.forEach((ballotBox) => {
			this.ballotBoxById.set(ballotBox.id, ballotBox);
		});
	}

	private getAllDownloadedBallotBoxStates(ballotBoxes: BallotBox[]): Observable<WorkflowState> {
		const ballotBoxStates = ballotBoxes.map((ballotBox) =>
			this.workflowStates.get(WorkflowStep.DownloadBallotBox, ballotBox.id)
		);

		return merge(...ballotBoxStates);
	}

	private moveBallotBoxDependingOnState(state: WorkflowState) {
		const ballotBox = this.ballotBoxById.get(state.contextId ?? '');
		if (!ballotBox) return;

		if (WorkflowStatus.Complete !== state.status) {
			this.mixableBallotBoxes.add(ballotBox);
		} else {
			this.mixableBallotBoxes.delete(ballotBox);
			this.exportableBallotBoxes = new Set(this.exportableBallotBoxes).add(ballotBox);
		}
	}

}
