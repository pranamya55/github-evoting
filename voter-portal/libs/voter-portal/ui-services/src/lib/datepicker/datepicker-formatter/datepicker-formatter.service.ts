/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Injectable } from '@angular/core';
import {
	NgbDateParserFormatter,
	NgbDateStruct,
} from '@ng-bootstrap/ng-bootstrap';

/**
 * This Service handles how the date is rendered and parsed from keyboard i.e. in the bound input field.
 */
@Injectable()
export class SwpDateParserFormatter extends NgbDateParserFormatter {
	readonly DELIMITER = '.';

	parse(value: string): NgbDateStruct | null {
		if (
			value &&
			new RegExp(
				`[0-9]{2}${this.DELIMITER}[0-9]{2}${this.DELIMITER}[0-9]{4}`,
			).test(value)
		) {
			const date = value.split(this.DELIMITER);
			return {
				day: parseInt(date[0], 10),
				month: parseInt(date[1], 10),
				year: parseInt(date[2], 10),
			};
		}
		return null;
	}

	format(date: NgbDateStruct | null): string {
		if (date) {
			const day = String(date.day).padStart(2, '0');
			const month = String(date.month).padStart(2, '0');
			const year = String(date.year).padStart(4, '0');

			return day + this.DELIMITER + month + this.DELIMITER + year;
		}

		return '';
	}
}
