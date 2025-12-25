/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {validateEA} from "../../../src/domain/validations/extended-authentication-factor-validation";
import {NullPointerError} from "crypto-primitives-ts/lib/esm/error/null_pointer_error";

describe('Extended authentication factory validation methods', function (): void {
	describe('validateEA', function (): void {

		test('with valid argument (4) should validate and return a extendedAuthenticationFactor', function (): void {
			const extendedAuthenticationFactor: string = "1993";
			expect(() => validateEA(extendedAuthenticationFactor, 4)).not.toThrow();
		});

		test('with valid argument (4) should validate and return a extendedAuthenticationFactor', function (): void {
			const extendedAuthenticationFactor: string = "a124";
			expect(() => validateEA(extendedAuthenticationFactor, 4)).not.toThrow();
		});

		test('with valid argument (8) should validate and return a extendedAuthenticationFactor', function (): void {
			const extendedAuthenticationFactor: string = "04111993";
			expect(() => validateEA(extendedAuthenticationFactor, 8)).not.toThrow();
		});

		test('with invalid argument (2) should validate and return a extendedAuthenticationFactor', function (): void {
			const extendedAuthenticationFactor: string = "93";
			expect(() => validateEA(extendedAuthenticationFactor, 4)).toThrow();
		});

		test('with null argument should throw a NullPointerError', function (): void {
			expect(() => validateEA(null, null)).toThrow(new NullPointerError());
		});
	});
});
