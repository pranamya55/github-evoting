/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {CHARACTER_LENGTH_SHORT_VOTE_CAST_RETURN_CODE} from "../voting-options-constants";
import {validateInAlphabet} from "./validations";

/**
 * Validates that the input corresponds to an 8 digit string.
 *
 * @param {string} toValidate - the string to validate. Must be non-null.
 *
 * @throws NullPointerError if the input string is null.
 * @throws FailedValidationError if the input string validation fails.
 *
 * @returns {string} - the validated string.
 */
export function validateShortVoteCastReturnCodes(toValidate: string): void {
	checkNotNull(toValidate);
	validateInAlphabet(toValidate, `^.{${CHARACTER_LENGTH_SHORT_VOTE_CAST_RETURN_CODE}}$`)
}