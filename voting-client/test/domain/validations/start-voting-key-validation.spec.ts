/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {validateSVK} from "../../../src/domain/validations/start-voting-key-validation";
import {SVK_ALPHABET} from "../../../src/domain/start-voting-key-alphabet";
import {NullPointerError} from "crypto-primitives-ts/lib/esm/error/null_pointer_error";
import {FailedValidationError} from "../../../src/domain/validations/failed-validation-error";

describe('Extended authentication factory validation methods', function (): void {
	describe('validateSVK', function (): void {
		test('with null argument should throw a NullPointerError', function (): void {
			expect(() => validateSVK(null)).toThrow(new NullPointerError());
		});

		test('with string to validate not of expected length should throw a FailedValidationError', function (): void {
			expect(() => validateSVK('jwwjecjkixjo7r6mdfxcim'))
				.toThrow(new FailedValidationError(`The given string does not comply with the required format. [string: jwwjecjkixjo7r6mdfxcim, format: ^[${SVK_ALPHABET.join("")}]{24}$].`));
		});

		test('with string to validate not in alphabet should throw a FailedValidationError', function (): void {
			expect(() => validateSVK('jwwjecjkixjo7r6mdfxcim7'))
				.toThrow(new FailedValidationError(`The given string does not comply with the required format. [string: jwwjecjkixjo7r6mdfxcim7, format: ^[${SVK_ALPHABET.join("")}]{24}$].`));
		});

		test('with valid arguments should validate and return base32 string', function (): void {
			expect(() => validateSVK('jwwjecjkixjk7r6mdfxcim7x')).not.toThrow();
		});
	});
});
