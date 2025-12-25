/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Injectable } from '@angular/core';
import { NgbDateAdapter, NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';

/**
 * This Service handles how the date is represented in scripts i.e. ngModel.
 */
@Injectable()
export class SwpDateAdapter extends NgbDateAdapter<string> {
	fromModel(value: string): NgbDateStruct | null {
		if (value && /\d{8}/.test(value)) {
			return {
				day: parseInt(value.substring(0, 2), 10),
				month: parseInt(value.substring(2, 4), 10),
				year: parseInt(value.substring(4, 8), 10),
			};
		}

		return null;
	}

	toModel(date: NgbDateStruct | null): string {
		if (date) {
			const day = String(date.day).padStart(2, '0');
			const month = String(date.month).padStart(2, '0');
			const year = String(date.year).padStart(4, '0');

			return day + month + year;
		}

		return '';
	}
}
