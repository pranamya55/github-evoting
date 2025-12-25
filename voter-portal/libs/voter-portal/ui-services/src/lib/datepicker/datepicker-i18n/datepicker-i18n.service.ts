/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Injectable, inject } from '@angular/core';
import { NgbDatepickerI18n, NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { TranslateService } from '@ngx-translate/core';

@Injectable({
	providedIn: 'root',
})
export class SwpDatepickerI18n extends NgbDatepickerI18n {
	private readonly translate = inject(TranslateService);

	weekdays = [
		'monday',
		'tuesday',
		'wednesday',
		'thursday',
		'friday',
		'saturday',
		'sunday',
	];
	months = [
		'january',
		'february',
		'march',
		'april',
		'may',
		'june',
		'july',
		'august',
		'september',
		'october',
		'november',
		'december',
	];

	constructor() {
		super();
	}

	getWeekdayLabel(weekday: number): string {
		return this.translate.instant(
			`datepicker.weekday.${this.weekdays[weekday - 1]}`,
		);
	}

	getMonthFullName(month: number): string {
		return this.translate.instant(`datepicker.month.${this.months[month - 1]}`);
	}

	getMonthShortName(month: number): string {
		return this.getMonthFullName(month);
	}

	getDayAriaLabel(date: NgbDateStruct): string {
		return `${date.day}-${date.month}-${date.year}`;
	}
}
