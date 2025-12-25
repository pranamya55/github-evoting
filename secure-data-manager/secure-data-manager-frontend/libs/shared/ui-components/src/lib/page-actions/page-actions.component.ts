/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Component, ElementRef, Input, OnChanges, Self} from '@angular/core';
import {
	VotingServerHealthService,
	WorkflowStateService,
} from '@sdm/shared-ui-services';
import {ActivatedRoute} from '@angular/router';
import {
	VotingServerHealth,
	WorkflowExceptionCode,
	WorkflowStatus,
} from '@sdm/shared-util-types';
import {TranslateModule} from '@ngx-translate/core';
import {environment} from '@sdm/shared-ui-config';

@Component({
	selector: 'sdm-page-actions',
	standalone: true,
	imports: [TranslateModule],
	templateUrl: './page-actions.component.html',
})
export class PageActionsComponent implements OnChanges {
	@Input() forceReady = false;
	@Input() forceInProgress = false;
	@Input() forceComplete = false;
	@Input() forceWarning = false;
	@Input() forceError = false;

	private workflowStatus!: WorkflowStatus;
	private forcedStatus = false;
	private isConnectedToServer = true;

	public errorFeedbackCode!: WorkflowExceptionCode;

	constructor(
		@Self() private readonly el: ElementRef<HTMLElement>,
		private readonly workflowStates: WorkflowStateService,
		private readonly route: ActivatedRoute,
		private readonly votingServerHealth: VotingServerHealthService,
	) {
		const step = this.route.snapshot.data['workflowStep'];
		if (!step) return;

		if (!environment.workflowEnabled) return;

		this.workflowStates
			.get(step)
			.pipe(takeUntilDestroyed())
			.subscribe((state) => {
				this.workflowStatus = state.status;
				this.errorFeedbackCode =
					state.exceptionCode ?? WorkflowExceptionCode.None;
			});

		if (!environment.remoteServerAvailable) return;

		this.isConnectedToServer = false;
		this.votingServerHealth
			.get()
			.pipe(takeUntilDestroyed())
			.subscribe((votingServerHealth: VotingServerHealth) => {
				this.isConnectedToServer = votingServerHealth.status;
			});
	}

	get isReady(): boolean {
		return this.forceReady || this.isCurrentStatus(WorkflowStatus.Ready);
	}

	get isInProgress(): boolean {
		return (
			this.forceInProgress || this.isCurrentStatus(WorkflowStatus.InProgress)
		);
	}

	get isComplete(): boolean {
		return this.forceComplete || this.isCurrentStatus(WorkflowStatus.Complete);
	}

	get isWarning(): boolean {
		return this.forceWarning;
	}

	get isError(): boolean {
		return this.forceError || this.isCurrentStatus(WorkflowStatus.Error);
	}

	get hasErrorFeedback(): boolean {
		return (
			this.errorFeedbackCode !== null &&
			this.errorFeedbackCode !== WorkflowExceptionCode.None &&
			this.errorFeedbackCode !== WorkflowExceptionCode.Default
		);
	}

	ngOnChanges() {
		this.forcedStatus =
			this.forceReady ||
			this.forceInProgress ||
			this.forceComplete ||
			this.forceWarning ||
			this.forceError;
	}

	private isCurrentStatus(status: WorkflowStatus): boolean {
		return !this.forcedStatus && this.workflowStatus === status;
	}
}
