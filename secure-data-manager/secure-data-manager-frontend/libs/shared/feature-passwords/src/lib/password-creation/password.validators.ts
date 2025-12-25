/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {AbstractControl, FormGroup, ValidationErrors} from '@angular/forms';

export class PasswordValidators {
	static validate(control: AbstractControl): ValidationErrors | null {
		const password = control.value;

		if (!password) {
			return null;
		}

		const isLengthValid = /^.{24,64}$/.test(password);
		const isDigitValid = /\d/.test(password);
		const isSpecialCharValid = /[^0-9a-zA-Z]/.test(password);
		const isLowerCharValid = /[a-z]/.test(password);
		const isUpperCharValid = /[A-Z]/.test(password);

		const isPasswordValid =
			isLengthValid &&
			isDigitValid &&
			isSpecialCharValid &&
			isLowerCharValid &&
			isUpperCharValid;

		return !isPasswordValid
			? {
				length: !isLengthValid,
				digit: !isDigitValid,
				specialChar: !isSpecialCharValid,
				lowerCaseChar: !isLowerCharValid,
				uppercaseChar: !isUpperCharValid,
			}
			: null;
	}

	static confirm(control: AbstractControl): ValidationErrors | null {
		if (
			!(
				control instanceof FormGroup &&
				'password' in control.value &&
				'confirmation' in control.value
			)
		) {
			throw new Error(
				'PasswordValidators.confirm validator should only be used on a FormGroup with both a "password" and a "confirmation" field.',
			);
		}

		const {password, confirmation} = control.value;
		return password !== confirmation ? {confirmation: true} : null;
	}
}
