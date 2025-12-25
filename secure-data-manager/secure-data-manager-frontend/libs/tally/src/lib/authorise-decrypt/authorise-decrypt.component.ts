/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';
import {PasswordValidationComponent} from '@sdm/shared-feature-passwords';
import {PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';
import {RouterLinkToDirective} from '@sdm/shared-ui-directives';
import {BoardMember, WorkflowStep} from '@sdm/shared-util-types';
import {Observable, Subject} from 'rxjs';
import {AuthoriseDecryptService} from './authorise-decrypt.service';
import {ActivatedRoute, Router} from '@angular/router';

@Component({
	selector: 'sdm-authorise-decrypt',
	standalone: true,
	imports: [
		CommonModule,
		PasswordValidationComponent,
		TranslateModule,
		PageActionsComponent,
		RouterLinkToDirective,
		PageTitleComponent,
	],
	templateUrl: './authorise-decrypt.component.html',
})
export class AuthoriseDecryptComponent {
	readonly WorkflowStep = WorkflowStep;

	isPasswordValid = new Subject<boolean>();
	boardMembers$: Observable<BoardMember[]>;
	passwords: string[] = [];
	areAllPasswordsSet = false;
	cannotValidatePassword = false;

	constructor(
		private readonly authoriseDecryptService: AuthoriseDecryptService,
		private readonly router: Router,
		private readonly route: ActivatedRoute,
	) {
		this.boardMembers$ =
			this.authoriseDecryptService.getElectoralBoardMembers();
	}

	validatePassword([boardMember, password]: [BoardMember, string]) {
		this.cannotValidatePassword = false;
		this.authoriseDecryptService
			.validatePassword(boardMember, password)
			.subscribe({
				next: (wasValidated) => {
					if (wasValidated) {
						this.passwords.push(password);
					}
					this.isPasswordValid.next(wasValidated);
				},
				error: () => {
					this.cannotValidatePassword = true;
					this.isPasswordValid.next(false);
				},
			});
	}

	decrypt() {
		const ballotBoxesToDecrypt = history.state['ballotBoxesToDecrypt'];
		this.router.navigate(['../'], {
			relativeTo: this.route,
			state: {
				passwords: this.passwords,
				ballotBoxesToDecrypt: ballotBoxesToDecrypt,
			},
		});
		history.replaceState('ballotBoxesToDecrypt', '');
	}

	back() {
		this.router.navigate(['../'], {
			relativeTo: this.route,
		});
	}
}
