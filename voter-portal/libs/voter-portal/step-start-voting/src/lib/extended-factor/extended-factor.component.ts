/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {AfterViewInit, Component, ElementRef, inject, Input, ViewChild,} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {NgbCalendar, NgbDateStruct} from '@ng-bootstrap/ng-bootstrap';
import {ConfigurationService, SwpDateAdapter} from '@vp/voter-portal-ui-services';
import {ErrorStatus, ExtendedFactor} from '@vp/voter-portal-util-types';
import {NgxMaskPipe} from 'ngx-mask';
import {TranslateService} from "@ngx-translate/core";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";

@Component({
	selector: 'vp-extended-factor',
	templateUrl: './extended-factor.component.html',
	providers: [SwpDateAdapter, NgxMaskPipe],
	standalone: false,
})
export class ExtendedFactorComponent implements AfterViewInit {
	readonly configuration = inject(ConfigurationService);
	private readonly dateAdapter = inject(SwpDateAdapter);
	private readonly calendar = inject(NgbCalendar);
	private readonly translate = inject(TranslateService);

	@ViewChild('dateOfBirthInput') dateOfBirthInput:
		| ElementRef<HTMLInputElement>
		| undefined;
	@Input() voterForm!: FormGroup;
	@Input() formSubmitted = false;
	datepickerMinDate: NgbDateStruct;
	datepickerMaxDate: NgbDateStruct;
	datepickerDefaultDate: NgbDateStruct;
	dateMaskExpression = '';
	dateSpecialCharacters: string[] = [];

	readonly ExtendedFactor = ExtendedFactor;
	readonly ErrorMessage = ErrorStatus;

	constructor() {
		const today = this.calendar.getToday();
		this.datepickerMinDate = this.calendar.getPrev(today, 'y', 120);
		this.datepickerMaxDate = this.calendar.getPrev(today, 'y', 16);
		this.datepickerDefaultDate = this.calendar.getPrev(today, 'y', 40);

		this.translate.stream('datepicker.placeholder')
			.pipe(takeUntilDestroyed())
			.subscribe((placeholder: string) => {
				this.dateMaskExpression = placeholder.toUpperCase();
				this.dateSpecialCharacters = this.dateMaskExpression.split('');
			});
	}

	get extendedFactor(): FormControl {
		return this.voterForm.get('extendedFactor') as FormControl;
	}

	get datePickerSelectedDate(): NgbDateStruct | null {
		return this.dateAdapter.fromModel(this.extendedFactor.value);
	}

	set datePickerSelectedDate(date: NgbDateStruct | null) {
		this.extendedFactor.setValue(this.dateAdapter.toModel(date));
	}

	public ngAfterViewInit(): void {
		if (this.configuration.identification === ExtendedFactor.YearOfBirth) {
			this.extendedFactor.addValidators(Validators.pattern(/^\d{4}$/));
		}

		if (this.configuration.identification === ExtendedFactor.DateOfBirth) {
			this.extendedFactor.addValidators(Validators.pattern(/^\d{8}$/));
		}
	}
}
