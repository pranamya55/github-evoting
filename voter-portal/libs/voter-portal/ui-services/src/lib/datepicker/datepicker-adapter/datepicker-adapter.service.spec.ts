/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { formatDate } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { RandomDate, RandomDateStruct, RandomInt } from '@vp/shared-util-testing';
import { TranslateTestingModule } from 'ngx-translate-testing';

import { SwpDateAdapter } from './datepicker-adapter.service';

describe('DatepickerAdapterService', () => {
	let service: SwpDateAdapter;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [TranslateTestingModule.withTranslations({})],
			providers: [SwpDateAdapter],
		});
		service = TestBed.inject(SwpDateAdapter);
	});

	describe('fromModel', () => {
		it('should correctly parse a given date', () => {
			const date = RandomDate();
			const dateString = formatDate(date, 'ddMMyyyy', 'en');
			const expectedResult: NgbDateStruct = {
				day: date.getDate(),
				month: date.getMonth() + 1,
				year: date.getFullYear(),
			};

			expect(service.fromModel(dateString)).toEqual(expectedResult);
		});

		it('should return null if the given date is not eight digits', () => {
			const date = RandomDate().setDate(RandomInt(9, 1));
			const dateString = formatDate(date, 'dMy', 'en');
			expect(service.fromModel(dateString)).toBeNull();
		});

		it('should return null if no date is provided', () => {
			expect(service.fromModel('')).toBeNull();
		});
	});

	describe('toModel', () => {
		let dateStruct: NgbDateStruct;
		let resultingModel: string;

		beforeEach(() => {
			dateStruct = RandomDateStruct();
			resultingModel = service.toModel(dateStruct);
		});

		it('should correctly format a given date', () => {
			const { day, month, year } = dateStruct;

			expect(resultingModel.substring(0, 2)).toContain(String(day));
			expect(resultingModel.substring(2, 4)).toContain(String(month));
			expect(resultingModel.substring(4, 8)).toContain(String(year));
		});

		it('should always return the date on eight digits', () => {
			const expectedFormat = `[0-9]{8}`;
			expect(new RegExp(expectedFormat).test(resultingModel)).toBeTruthy();
		});

		it('should return an empty string if no date is provided', () => {
			expect(service.toModel(null)).toBe('');
		});
	});
});
