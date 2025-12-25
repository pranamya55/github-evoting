/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {FailedValidationError} from "./failed-validation-error";
import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";

const BIRTH_YEAR: string = "yob";
const BIRTH_DATE: string = "dob";
const BIRTH_YEAR_LENGTH: number = 4;
const BIRTH_DATE_LENGTH: number = 8;

const EXTENDED_AUTHENTICATION_FACTORS: Record<string, number> = {
	[BIRTH_YEAR]: BIRTH_YEAR_LENGTH,
	[BIRTH_DATE]: BIRTH_DATE_LENGTH
};

/**
 * * Validates the provided extended authentication factor using the provided extended authentication factor length.
 *
 * @param extendedAuthenticationFactor       the extended authentication factor. Must be non-null and be a sequence of
 *                                           {@code extendedAuthenticationFactorLength} digits.
 * @param extendedAuthenticationFactorLength the extended authentication factor length. Must be part of the supported
 *                                           {@code EXTENDED_AUTHENTICATION_FACTORS}.
 *
 * @returns {string} - the validated input string.
 */
export function validateEA(extendedAuthenticationFactor: string, extendedAuthenticationFactorLength: number): string {
	checkNotNull(extendedAuthenticationFactor);

	checkArgument(Object.values(EXTENDED_AUTHENTICATION_FACTORS).includes(extendedAuthenticationFactorLength),
		`Unsupported extended authentication factor length provided. [length: ${extendedAuthenticationFactorLength}]`
	);

	const regex = new RegExp(`^.{${extendedAuthenticationFactorLength}}$`);
	if (!regex.test(extendedAuthenticationFactor)) {
		throw new FailedValidationError("The extended authentication factor must be a digit of correct size.");
	}

	return extendedAuthenticationFactor;
}

/**
 * Validates that the identification type is part of the allowed identifications and converts it to the corresponding extended authentication factor length.
 *
 * @param {string} identification - the identification to validate. Must be non-null.
 *
 * @returns {number} - the extended authentication factor length corresponding to this identification.
 */
export function validateIdentificationAndConvert(identification: string): number {
	checkNotNull(identification);
	checkArgument(Object.keys(EXTENDED_AUTHENTICATION_FACTORS).includes(identification), `Unsupported identification provided. [provided: ${identification}]`);

	return EXTENDED_AUTHENTICATION_FACTORS[identification];
}
