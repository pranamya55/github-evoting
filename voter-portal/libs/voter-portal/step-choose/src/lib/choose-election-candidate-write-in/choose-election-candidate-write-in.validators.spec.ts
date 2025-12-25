/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { AbstractControl } from '@angular/forms';
import { WriteInValidators } from './choose-election-candidate-write-in.validators';
import { ChosenWriteIn } from 'e-voting-libraries-ui-kit';

describe('WriteInValidators', () => {
	describe('format', () => {
		it('should return null if the control value is valid', () => {
			const control = { value: 'John Doe' } as AbstractControl;
			const result = WriteInValidators.format(control);
			expect(result).toBeNull();
		});

		it('should return an error if the control value is empty', () => {
			const control = { value: '' } as AbstractControl;
			const result = WriteInValidators.format(control);
			expect(result).toEqual({ incorrectFormat: true });
		});

		it('should return an error if the control value exceeds the max length', () => {
			const longString = 'a'.repeat(96);
			const control = { value: longString } as AbstractControl;
			const result = WriteInValidators.format(control);
			expect(result).toEqual({ incorrectFormat: true });
		});

		it('should return an error if the control value has incorrect format', () => {
			const control = { value: 'JohnDoe' } as AbstractControl;
			const result = WriteInValidators.format(control);
			expect(result).toEqual({ incorrectFormat: true });
		});
	});

	describe('characters', () => {
		const alphabet = ' abcdefghijklmnopqrstuvwxyz';

		it('should return null if the control value contains only valid characters', () => {
			const control = { value: 'abc' } as AbstractControl;
			const validator = WriteInValidators.characters(alphabet);
			const result = validator(control);
			expect(result).toBeNull();
		});

		it('should return an error if the control value contains invalid characters', () => {
			const control = { value: 'abc!' } as AbstractControl;
			const validator = WriteInValidators.characters(alphabet);
			const result = validator(control);
			expect(result).toEqual({ incorrectCharacters: true });
		});
	});

	describe('allowed', () => {
		const chosenWriteIns: ChosenWriteIn[] = [
			{ writeIn: 'John Doe' } as ChosenWriteIn,
			{ writeIn: 'Jane Smith' } as ChosenWriteIn,
		];

		it('should return null if no chosen write-ins are provided', () => {
			const control = { value: 'John Doe' } as AbstractControl;
			const validator = WriteInValidators.allowed(undefined, []);
			const result = validator(control);
			expect(result).toBeNull();
		});

		it('should return null if the control value is allowed', () => {
			['John Doe', 'John Snow'].forEach((v) => {
				const control = { value: v } as AbstractControl;
				const validator = WriteInValidators.allowed(chosenWriteIns, [
					'John Snow',
				]);
				const result = validator(control);
				expect(result).toBeNull();
			});
		});

		it('should return an error if the control value is not allowed', () => {
			const control = { value: 'John Snow' } as AbstractControl;
			const validator = WriteInValidators.allowed(chosenWriteIns, []);
			const result = validator(control);
			expect(result).toEqual({ notSelectedInPrimary: true });
		});
	});
});
