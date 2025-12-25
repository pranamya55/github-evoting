/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component, DestroyRef, EventEmitter, HostBinding, inject, Input, OnChanges, Output, SimpleChanges,} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {WorkflowStateService} from '@sdm/shared-ui-services';
import {BallotBox, BallotBoxCategory, WorkflowExceptionCode, WorkflowState, WorkflowStatus, WorkflowStep,} from '@sdm/shared-util-types';
import {combineLatest, map, pipe, startWith} from 'rxjs';
import {ResultsModalService} from "@sdm/shared-feature-results";

@Component({
	// should be a selector for proper table display
	// eslint-disable-next-line @angular-eslint/component-selector
	selector: 'tr[sdm-ballot-box-list-item]',
	standalone: true,
	imports: [CommonModule, FormsModule, TranslateModule],
	templateUrl: './ballot-box-list-item.component.html',
})
export class BallotBoxListItemComponent implements OnChanges {
	readonly WorkflowStatus = WorkflowStatus;
	mixingStatus?: WorkflowStatus;
	downloadStatus?: WorkflowStatus;
	decryptionStatus?: WorkflowStatus;
	globalStatus?: WorkflowStatus;
	exceptionCode?: WorkflowExceptionCode;
	indicatorClass = '';
	@Input({required: true}) ballotBox!: BallotBox;
	@Input({required: true}) isShownWhenIdle!: boolean;
	@Input({required: true}) isCheckboxEnabled!: boolean;
	@Input() isSelected?: boolean;
	@Output() selectedChange = new EventEmitter<boolean>();
	@Output() isSelectable = new EventEmitter<boolean>();
	@HostBinding('class.table-light') isTestBallotBox?: boolean;
	@HostBinding('class.table-warning') isRegularBallotBox?: boolean;
	protected readonly WorkflowExceptionCode = WorkflowExceptionCode;
	private currentStep?: WorkflowStep;

	private readonly resultsModalService = inject(ResultsModalService);
	private readonly workflowStates= inject(WorkflowStateService);
	private readonly destroyRef = inject(DestroyRef);
	private readonly route= inject(ActivatedRoute);

	constructor() {
		this.route.data.pipe(takeUntilDestroyed()).subscribe((data) => {
			this.currentStep = data['workflowStep'];
		});
	}

	@HostBinding('class.d-none') get isHidden(): boolean {
		return !this.isShownWhenIdle && this.globalStatus === WorkflowStatus.Idle;
	}

	private get allStatuses(): (WorkflowStatus | undefined)[] {
		switch (this.currentStep) {
			case WorkflowStep.MixAndDownload:
				return [this.mixingStatus, this.downloadStatus];
			case WorkflowStep.Decrypt:
				return [this.decryptionStatus];
			default:
				return [];
		}
	}

	ngOnChanges(changes: SimpleChanges): void {
		if (!changes['ballotBox']) return;

		this.isTestBallotBox = this.ballotBox.test;
		this.isRegularBallotBox = !this.ballotBox.test;

		const ballotBoxStatusPipe = this.getStatePipe();

		const mixingState$ = this.workflowStates
			.get(WorkflowStep.MixBallotBox, this.ballotBox.id)
			.pipe(ballotBoxStatusPipe);

		const downloadState$ = this.workflowStates
			.get(WorkflowStep.DownloadBallotBox, this.ballotBox.id)
			.pipe(ballotBoxStatusPipe);

		const decryptionState$ = this.workflowStates
			.get(WorkflowStep.DecryptBallotBox, this.ballotBox.id)
			.pipe(ballotBoxStatusPipe);

		combineLatest([
			mixingState$,
			downloadState$,
			decryptionState$,
		])
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe(([mixingState, downloadState, decryptionState]) => {
				this.mixingStatus = mixingState?.status;
				this.downloadStatus = downloadState?.status;
				this.decryptionStatus = decryptionState?.status;

				this.globalStatus = this.getGlobalStatus();
				this.indicatorClass = this.getIndicatorClass();

				this.isSelectable.emit(
					this.globalStatus === WorkflowStatus.Ready ||
					this.globalStatus === WorkflowStatus.Error,
				);

				if (this.mixingStatus === WorkflowStatus.Error) {
					this.exceptionCode = mixingState?.exceptionCode ?? WorkflowExceptionCode.Default;
				}

				if (this.downloadStatus === WorkflowStatus.Error) {
					this.exceptionCode = downloadState?.exceptionCode ?? WorkflowExceptionCode.Default;
				}

				if (this.decryptionStatus === WorkflowStatus.Error) {
					this.exceptionCode = decryptionState?.exceptionCode ?? WorkflowExceptionCode.Default;
				}
			});
	}

	ballotBoxCategory(): string {
		return this.ballotBox.test ? BallotBoxCategory.Test : BallotBoxCategory.Regular;
	}

	showResults(): void {
		if (this.decryptionStatus !== WorkflowStatus.Complete) {
			return;
		}
		this.resultsModalService.showResults(this.ballotBox.id);
	}

	private getStatePipe() {
		return pipe(
			map((state: WorkflowState) => state),
			// required for "combineLatest" to emit the first workflow state
			startWith(undefined),
		);
	}

	private getGlobalStatus(): WorkflowStatus {
		const leastAdvancedStatus =
			this.allStatuses.find(
				(status) => !!status && status !== WorkflowStatus.Complete,
			) ?? WorkflowStatus.Complete;

		return leastAdvancedStatus ?? WorkflowStatus.Complete;
	}

	private getIndicatorClass() {
		switch (this.globalStatus) {
			case WorkflowStatus.Ready:
				return 'bg-info border-info';
			case WorkflowStatus.Complete:
				return 'bg-success border-success text-white';
			case WorkflowStatus.Error:
				return 'bg-danger border-danger text-white';
			default:
				return 'bg-dark-subtle border-dark-subtle';
		}
	}
}
