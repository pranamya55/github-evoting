/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {CHARACTER_LENGTH_SHORT_CHOICE_RETURN_CODE} from "../voting-options-constants";
import {validateInAlphabet} from "./validations";

/**
 * Validates that the input array contains only 4 digit strings.
 *
 * @param {string[]} toValidate - the string array to validate. Must be non-null.
 *
 * @throws NullPointerError if the input string array is null.
 * @throws IllegalArgumentError if the input string array is empty.
 * @throws FailedValidationError if the input string array elements validation fails.
 *
 * @returns {string[]} - the validated string array.
 */
export function validateShortChoiceReturnCodes(toValidate: string[]): void {
	checkNotNull(toValidate);
	checkArgument(toValidate.length !== 0, "There must be at least one short Choice Return Code.");
	toValidate.forEach(shortChoiceReturnCode => validateInAlphabet(shortChoiceReturnCode, `^.{${CHARACTER_LENGTH_SHORT_CHOICE_RETURN_CODE}}$`));
}