/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';
import {PasswordCreationComponent} from '@sdm/shared-feature-passwords';
import {ProgressComponent} from '@sdm/shared-feature-progress';
import {BoardMember, WorkflowStatus, WorkflowStep,} from '@sdm/shared-util-types';
import {WorkflowStateService} from '@sdm/shared-ui-services';
import {ConstituteElectoralBoardService} from './constitute-electoral-board.service';
import {map, Observable} from 'rxjs';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NextButtonComponent, PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';

@Component({
	selector: 'sdm-constitute-electoral-board',
	standalone: true,
	imports: [
		CommonModule,
		TranslateModule,
		ProgressComponent,
		PasswordCreationComponent,
		PageActionsComponent,
		NextButtonComponent,
		PageTitleComponent,
	],
	templateUrl: './constitute-electoral-board.component.html',
})
export class ConstituteElectoralBoardComponent {
	readonly WorkflowStatus = WorkflowStatus;
	constituteStatus$: Observable<WorkflowStatus>;
	boardMembers: BoardMember[] = [];
	passwords: string[] = [];

	constructor(
		private readonly constituteElectoralBoardService: ConstituteElectoralBoardService,
		private readonly workflowStates: WorkflowStateService,
	) {
		this.constituteStatus$ = this.workflowStates
			.get(WorkflowStep.ConstituteElectoralBoard)
			.pipe(
				map((event) => event.status),
				takeUntilDestroyed(),
			);

		this.constituteElectoralBoardService
			.getMembers()
			.subscribe((members: BoardMember[]) => {
				this.boardMembers = members;
			});
	}

	get areAllPasswordsSet(): boolean {
		if (!this.passwords) return false;
		return this.passwords.length === this.boardMembers.length;
	}

	constitute(): void {
		if (!this.areAllPasswordsSet) return;
		this.constituteElectoralBoardService.constitute(this.passwords);
	}
}
