/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { formatDate } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { RandomDate, RandomDateStruct, RandomInt } from '@vp/shared-util-testing';
import { TranslateTestingModule } from 'ngx-translate-testing';

import { SwpDateParserFormatter } from './datepicker-formatter.service';

describe('DatepickerFormatterService', () => {
	let service: SwpDateParserFormatter;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [TranslateTestingModule.withTranslations({})],
			providers: [SwpDateParserFormatter],
		});
		service = TestBed.inject(SwpDateParserFormatter);
	});

	describe('parse', () => {
		it('should correctly parse a given date', () => {
			const date = RandomDate();
			const dateString = formatDate(date, 'dd.MM.yyyy', 'en');
			const expectedResult: NgbDateStruct = {
				day: date.getDate(),
				month: date.getMonth() + 1,
				year: date.getFullYear(),
			};

			expect(service.parse(dateString)).toEqual(expectedResult);
		});

		it('should return null if the day of the given date is not two digits', () => {
			const date = RandomDate().setDate(RandomInt(9, 1));
			const dateString = formatDate(date, 'd.MM.yyyy', 'en');
			expect(service.parse(dateString)).toBeNull();
		});

		it('should return null if the month of the given date is not two digits', () => {
			const date = RandomDate().setMonth(RandomInt(9, 1));
			const dateString = formatDate(date, 'dd.M.yyyy', 'en');
			expect(service.parse(dateString)).toBeNull();
		});

		it('should return null if the year of the given date is not four digits', () => {
			const date = RandomDate().setFullYear(RandomInt(999, 1));
			const dateString = formatDate(date, 'dd.MM.y', 'en');
			expect(service.parse(dateString)).toBeNull();
		});

		it('should return null if no date is provided', () => {
			expect(service.parse('')).toBeNull();
		});
	});

	describe('format', () => {
		let dateStruct: NgbDateStruct;
		let formattedResult: string;

		beforeEach(() => {
			dateStruct = RandomDateStruct();
			formattedResult = service.format(dateStruct);
		});

		it('should correctly format a given date', () => {
			const { day, month, year } = dateStruct;

			[day, month, year].forEach((dateElement, i) => {
				expect(formattedResult.split(service.DELIMITER)[i]).toContain(
					String(dateElement),
				);
			});
		});

		it('should always return the day and the month on two digits and the year on four', () => {
			const expectedFormat = `[0-9]{2}${service.DELIMITER}[0-9]{2}${service.DELIMITER}[0-9]{4}`;
			expect(new RegExp(expectedFormat).test(formattedResult)).toBeTruthy();
		});

		it('should return an empty string if no date is provided', () => {
			expect(service.format(null)).toBe('');
		});
	});
});
