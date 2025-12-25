/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	AfterContentInit,
	Component,
	ElementRef,
	inject,
	Input,
	OnChanges,
	OnDestroy,
	Signal,
	SimpleChanges,
	ViewChild,
} from '@angular/core';
import { FormControl, ValidatorFn } from '@angular/forms';
import { Store } from '@ngrx/store';
import { FAQService } from '@vp/voter-portal-feature-faq';
import { FAQSection } from '@vp/voter-portal-util-types';
import {
	ChosenWriteIn,
	ElectionInformation,
	EmptyPosition,
} from 'e-voting-libraries-ui-kit';
import { WriteInValidators } from './choose-election-candidate-write-in.validators';
import { getDefinedWriteInAlphabet, getHasSubmittedAnswer } from '@vp/voter-portal-ui-state';
import { take } from 'rxjs/operators';

@Component({
	selector: 'vp-choose-election-candidate-write-in',
	templateUrl: './choose-election-candidate-write-in.component.html',
	standalone: false,
})
export class ChooseElectionCandidateWriteInComponent
	implements OnChanges, AfterContentInit, OnDestroy
{
	@ViewChild('writeIn', { read: ElementRef })
	writeInInput!: ElementRef<HTMLInputElement>;

	@Input({ required: true }) electionInformation!: ElectionInformation;
	@Input({ required: true }) emptyPosition!: EmptyPosition;
	@Input({ required: true }) writeInControl!: FormControl;
	@Input({ required: true }) writeInsChosenInPrimaryElection!:
		| ChosenWriteIn[]
		| undefined;

	initialWriteIn = '';

	private allowedWriteInValidator?: ValidatorFn;

	private readonly store: Store = inject(Store);
	private readonly faqService: FAQService = inject(FAQService);

	readonly shouldShowErrors: Signal<boolean> = this.store.selectSignal(
		getHasSubmittedAnswer,
	);

	get positionOnList(): number {
		return this.emptyPosition.positionOnList;
	}

	get electionIdentification(): string {
		return this.electionInformation.election.electionIdentification;
	}

	ngOnChanges(changes: SimpleChanges) {
		if (changes['writeInsChosenInPrimaryElection']) {
			if (this.allowedWriteInValidator)
				this.writeInControl.removeValidators(this.allowedWriteInValidator);

			const implicitWriteInCandidates: string[] =
				this.electionInformation.implicitWriteInCandidates ?? [];
			this.allowedWriteInValidator = WriteInValidators.allowed(
				this.writeInsChosenInPrimaryElection,
				implicitWriteInCandidates,
			);
			this.addValidator(this.allowedWriteInValidator);
		}
	}

	ngAfterContentInit() {
		this.initialWriteIn = this.writeInControl.value;
		this.addValidator(WriteInValidators.format);

		this.store
			.pipe(getDefinedWriteInAlphabet, take(1))
			.subscribe((alphabet) => {
				this.addValidator(WriteInValidators.characters(alphabet));
			});

		setTimeout(() => {
			this.writeInControl.updateValueAndValidity();
			this.writeInInput?.nativeElement.focus();
		});
	}

	ngOnDestroy(): void {
		this.writeInControl.clearValidators();
		setTimeout(() => this.writeInControl.updateValueAndValidity());
	}

	setWriteIn(writeIn: string): void {
		const normalizedValue = writeIn.trim().split(/\s+/).join(' ');
		this.writeInControl.setValue(normalizedValue);
	}

	showWriteInsFAQ(): void {
		this.faqService.showFAQ(FAQSection.HowToUseWriteIns);
	}

	preventFormSubmission(): false {
		return false;
	}

	private addValidator(validator: ValidatorFn) {
		this.writeInControl.addValidators(validator);
		setTimeout(() => {
			this.writeInControl.updateValueAndValidity();
		});
	}
}
