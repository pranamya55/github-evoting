/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component, inject, OnInit} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ActivatedRoute, Router} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {BallotBoxListComponent} from '@sdm/shared-feature-ballot-box-list';
import {WorkflowStateService} from '@sdm/shared-ui-services';
import {BallotBox, WorkflowState, WorkflowStatus, WorkflowStep} from '@sdm/shared-util-types';
import {map, merge, Observable, startWith, switchMap, tap} from 'rxjs';
import {DecryptService} from './decrypt.service';
import {NextButtonComponent, PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';


@Component({
	selector: 'sdm-decrypt',
	standalone: true,
	imports: [
		CommonModule,
		TranslateModule,
		BallotBoxListComponent,
		PageActionsComponent,
		NextButtonComponent,
		PageTitleComponent
	],
	templateUrl: './decrypt.component.html',
})
export class DecryptComponent implements OnInit {
	readonly WorkflowStatus = WorkflowStatus;
	decryptStatus$: Observable<WorkflowStatus>;
	exportableBallotBoxes = new Set<BallotBox>();
	decryptableBallotBoxes = new Set<BallotBox>();
	selectedBallotBoxes: BallotBox[] = [];
	ballotBoxById = new Map<BallotBox['id'], BallotBox>();

	private readonly router = inject(Router);
	private readonly route = inject(ActivatedRoute);
	private readonly decryptService = inject(DecryptService);
	private readonly workflowStates = inject(WorkflowStateService);

	constructor() {
		this.decryptStatus$ = this.workflowStates.get(WorkflowStep.Decrypt).pipe(
			map((state) => state.status),
			startWith(WorkflowStatus.Ready),
		);

		this.decryptService.getBallotBoxes().pipe(
			tap((ballotBoxes) => {
				this.registerBallotBoxes(ballotBoxes);
			}),
			switchMap(ballotBoxes => this.getAllBallotBoxStates(ballotBoxes)),
			takeUntilDestroyed()
		).subscribe((state) => {
			this.moveBallotBoxDependingOnState(state);
		});
	}

	get hasSelection(): boolean {
		return this.selectedBallotBoxes.length > 0;
	}

	ngOnInit(): void {
		const passwords: string[] = history.state['passwords'];
		const ballotBoxes: BallotBox[] = history.state['ballotBoxesToDecrypt'];
		if (passwords?.length > 0 && ballotBoxes?.length > 0) {
			this.decryptService.decrypt(ballotBoxes, passwords);
		}
		history.replaceState('passwords', '');
		history.replaceState('ballotBoxesToDecrypt', '');
	}

	decrypt() {
		if (this.selectedBallotBoxes.length === 0) return;
		this.router.navigate(['./authorise'], {
			relativeTo: this.route,
			state: {ballotBoxesToDecrypt: this.selectedBallotBoxes},
		});
	}

	private registerBallotBoxes(ballotBoxes: BallotBox[]) {
		ballotBoxes.forEach((ballotBox) => {
			this.ballotBoxById.set(ballotBox.id, ballotBox);
		});
	}

	private getAllBallotBoxStates(ballotBoxes: BallotBox[]): Observable<WorkflowState> {
		const ballotBoxStates = ballotBoxes.map((ballotBox) =>
			this.workflowStates.get(WorkflowStep.DecryptBallotBox, ballotBox.id)
		);

		return merge(...ballotBoxStates);
	}

	private moveBallotBoxDependingOnState(state: WorkflowState) {
		const ballotBox = this.ballotBoxById.get(state.contextId ?? '');
		if (!ballotBox) return;

		if (state.status === WorkflowStatus.Ready) {
			this.decryptableBallotBoxes.add(ballotBox);
		}

		if (state.status === WorkflowStatus.Complete) {
			this.decryptableBallotBoxes.delete(ballotBox);
			this.exportableBallotBoxes = new Set(this.exportableBallotBoxes).add(ballotBox);
		}
	}
}
