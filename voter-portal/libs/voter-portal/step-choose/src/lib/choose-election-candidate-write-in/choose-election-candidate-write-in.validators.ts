/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { ChosenWriteIn } from 'e-voting-libraries-ui-kit';

const WRITE_IN_MAX_LENGTH = 95;

export class WriteInValidators {
	static format(control: AbstractControl): ValidationErrors | null {
		return writeInFormatValidator(control);
	}

	static characters(alphabet: string): ValidatorFn {
		return writeInCharactersValidator(alphabet);
	}

	static allowed(
		writeInsChosenInPrimaryElection: ChosenWriteIn[] | undefined,
		implicitWriteInCandidates: string[],
	): ValidatorFn {
		return allowedWriteInValidator(
			writeInsChosenInPrimaryElection,
			implicitWriteInCandidates,
		);
	}
}

function writeInFormatValidator(
	control: AbstractControl,
): ValidationErrors | null {
	const hasCorrectLength =
		!!control.value && control.value.length <= WRITE_IN_MAX_LENGTH;
	const hasCorrectFormat = /^\S+\s\S.*$/.test(control.value);
	return hasCorrectLength && hasCorrectFormat
		? null
		: { incorrectFormat: true };
}

function writeInCharactersValidator(alphabet: string): ValidatorFn {
	return (control: AbstractControl): ValidationErrors | null => {
		const matchAlphabet = new RegExp(`^[${alphabet.substring(1)}]+$`).test(
			control.value,
		);
		return matchAlphabet ? null : { incorrectCharacters: true };
	};
}

function allowedWriteInValidator(
	writeInsChosenInPrimaryElection: ChosenWriteIn[] | undefined,
	implicitWriteInCandidates: string[],
): ValidatorFn {
	return (control: AbstractControl): ValidationErrors | null => {
		const isAllowed =
			writeInsChosenInPrimaryElection === undefined ||
			writeInsChosenInPrimaryElection.some((chosenWriteIn) => {
				return control.value === chosenWriteIn.writeIn;
			}) ||
			implicitWriteInCandidates.indexOf(control.value) > -1;
		return isAllowed ? null : { notSelectedInPrimary: true };
	};
}
