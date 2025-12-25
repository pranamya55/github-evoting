/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, ElementRef, inject, OnInit, ViewChild,} from '@angular/core';
import {FormBuilder, FormControl, FormGroup, Validators,} from '@angular/forms';
import {Store} from '@ngrx/store';
import {FAQService} from '@vp/voter-portal-feature-faq';
import {CancelState, ConfigurationService, ProcessCancellationService,} from '@vp/voter-portal-ui-services';
import {getBackendError, getLoading, SharedActions, StartVotingActions,} from '@vp/voter-portal-ui-state';
import {ErrorStatus, FAQSection} from '@vp/voter-portal-util-types';
import {Observable, take} from 'rxjs';
import {focusFirstInvalidControl} from "@vp/voter-portal-util-helpers";

@Component({
	selector: 'vp-start-voting',
	templateUrl: './start-voting.component.html',
	standalone: false,
})
export class StartVotingComponent implements OnInit {
	readonly configuration = inject(ConfigurationService);
	private readonly store = inject(Store);
	private readonly fb = inject(FormBuilder);
	private readonly faqService = inject(FAQService);
	readonly cancelProcessService = inject(ProcessCancellationService);

	@ViewChild('startVotingInput')
	startVotingInput: ElementRef<HTMLInputElement> | null = null;
	readonly CancelState = CancelState;
	readonly ErrorMessage = ErrorStatus;
	isLoading$: Observable<boolean> = this.store.select(getLoading);
	voterForm: FormGroup;
	formSubmitted = false;

	constructor() {
		this.voterForm = this.fb.group({
			startVotingKey: [
				'',
				[Validators.required, Validators.pattern(/^[0-9a-zA-Z]{24}$/)],
			],
		});

		if (this.configuration.identification) {
			this.voterForm.addControl(
				'extendedFactor',
				this.fb.control('', Validators.required),
			);
		}
	}

	get startVotingKey(): FormControl {
		return this.voterForm.get('startVotingKey') as FormControl;
	}

	ngOnInit(): void {
		if (
			this.cancelProcessService.cancelState !==
			CancelState.NO_CANCEL_VOTE_OR_LEAVE_PROCESS
		) {
			this.store.dispatch(SharedActions.serverErrorCleared());
		}
	}

	submitFormOnEnter(): void {
		if (this.voterForm.get('extendedFactor')?.value) {
			this.start();
		}
	}

	start(): void {
		this.formSubmitted = true;
		if (this.voterForm.invalid) {
			this.resetInvalidControls();
			focusFirstInvalidControl();
			return;
		}

		this.cancelProcessService.reset();
		this.store.dispatch(
			StartVotingActions.startClicked({ voter: this.voterForm.value }),
		);
		this.store
			.select(getBackendError)
			.pipe(take(1))
			.subscribe(() => {
				focusFirstInvalidControl();
			});
	}

	showFAQ(): void {
		this.faqService.showFAQ(FAQSection.WhatIsStartVotingKey);
	}

	private resetInvalidControls() {
		Object.values(this.voterForm.controls)
			.filter((control) => control.invalid)
			.forEach((control) => control.reset());
	}
}
