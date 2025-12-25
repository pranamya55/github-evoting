/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {SVK_ALPHABET} from "../start-voting-key-alphabet";
import {CHARACTER_LENGTH_OF_START_VOTING_KEY} from "../voting-options-constants";
import {validateInAlphabet} from "./validations";

/**
 * Validates that the input string is in SVK alphabet and has length of 24.
 * The alphabet corresponds to the Base32 lowercase version excluding padding "=" of "Table 3: The Base 32 Alphabet" from RFC3548.
 * Moreover, the letters "l" and "o" are replaced by "8" and "9".
 *
 * @param {string} toValidate - the string to validate. Must be non-null.
 *
 * @throws NullPointerError if the input string is null.
 * @throws FailedValidationError if the input string validation fails.
 *
 * @returns {string} - the validated input string.
 */
export function validateSVK(toValidate: string): string {
	checkNotNull(toValidate);
	return validateInAlphabet(toValidate, `^[${SVK_ALPHABET.join("")}]{${CHARACTER_LENGTH_OF_START_VOTING_KEY}}$`);
}