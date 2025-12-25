/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { TestBed } from '@angular/core/testing';
import { RandomDate, RandomDateStruct } from '@vp/shared-util-testing';
import { TranslateTestingModule } from 'ngx-translate-testing';

import { SwpDatepickerI18n } from './datepicker-i18n.service';

describe('DatepickerI18nService', () => {
	let service: SwpDatepickerI18n;
	let date: Date;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [TranslateTestingModule.withTranslations({})],
			providers: [SwpDatepickerI18n],
		});
		service = TestBed.inject(SwpDatepickerI18n);
		date = RandomDate();
	});

	it('should return the correct day label for any given date', () => {
		const weekday = date.getDay();
		const expectedDayLabel = `datepicker.weekday.${
			service.weekdays[weekday - 1]
		}`;
		expect(service.getWeekdayLabel(weekday)).toBe(expectedDayLabel);
	});

	it('should return the correct month label for any given date', () => {
		const month = date.getMonth();
		const expectedMonthLabel = `datepicker.month.${service.months[month - 1]}`;
		expect(service.getMonthFullName(month)).toBe(expectedMonthLabel);
	});

	it('should return the same content for the short month label as for the full month label', () => {
		const month = date.getMonth();
		expect(service.getMonthShortName(month)).toEqual(
			service.getMonthFullName(month),
		);
	});

	it('should return a proper aria label for the day of any given date', () => {
		const dateStruct = RandomDateStruct();
		const { day, month, year } = dateStruct;

		[day, month, year].forEach((dateElement) => {
			expect(service.getDayAriaLabel(dateStruct)).toContain(
				String(dateElement),
			);
		});
	});
});
