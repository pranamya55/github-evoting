/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	Component,
	ElementRef,
	inject,
	Signal,
	ViewChild,
} from '@angular/core';
import { Store } from '@ngrx/store';
import {CancelMode, ErrorStatus, FAQSection} from '@vp/voter-portal-util-types';
import { getBackendError, getLoading, VerifyActions } from '@vp/voter-portal-ui-state';
import {
	FormBuilder,
	FormControl,
	FormGroup,
	Validators,
} from '@angular/forms';
import { isElectionTexts, isVoteTexts } from 'e-voting-libraries-ui-kit';
import { take } from 'rxjs/operators';
import { FAQService } from '@vp/voter-portal-feature-faq';
import { TranslateService } from '@ngx-translate/core';

@Component({
	selector: 'vp-cast',
	standalone: false,
	templateUrl: './cast.component.html',
})
export class CastComponent {
	readonly translate = inject(TranslateService);
	@ViewChild('confirmationKeyInput')
	confirmationKeyInput: ElementRef<HTMLInputElement> | null = null;
	confirmationKeyForm: FormGroup<{
		confirmationKey: FormControl<string>;
	}>;
	protected readonly FAQSection = FAQSection;
	protected readonly ErrorMessage = ErrorStatus;
	protected readonly isVoteTexts = isVoteTexts;
	protected readonly isElectionTexts = isElectionTexts;
	private readonly store = inject(Store);
	isLoading: Signal<boolean> = this.store.selectSignal(getLoading);
	private readonly fb = inject(FormBuilder);
	private readonly faqService = inject(FAQService);

	constructor() {
		this.confirmationKeyForm = this.fb.nonNullable.group({
			confirmationKey: ['', [Validators.required, Validators.minLength(9)]],
		});
	}

	get confirmationKey(): FormControl {
		return this.confirmationKeyForm.get('confirmationKey') as FormControl;
	}

	showFAQ(section: FAQSection): void {
		this.faqService.showFAQ(section);
	}

	cast(): void {
		if (this.confirmationKeyForm.invalid) {
			this.confirmationKeyForm.reset();
			this.focusFirstInvalidControl();
			return;
		}

		this.store.dispatch(
			VerifyActions.castVoteClicked({
				confirmationKey: this.confirmationKey.value,
			}),
		);
		this.store
			.select(getBackendError)
			.pipe(take(1))
			.subscribe(() => {
				this.focusFirstInvalidControl();
			});
	}

	private focusFirstInvalidControl(): void {
		setTimeout(() => {
			const firstInvalid = document.querySelector<HTMLInputElement>(
				'.form-control.is-invalid',
			);
			firstInvalid?.click();
		});
	}

	protected readonly CancelMode = CancelMode;
}
