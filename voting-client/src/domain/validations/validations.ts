/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {VotingOptionType} from "../election/voting-option.type";
import {FailedValidationError} from "./failed-validation-error";
import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {
	ACTUAL_VOTING_OPTION_DELIMITER,
	CHARACTER_LENGTH_OF_BALLOT_CASTING_KEY,
	CHARACTER_LENGTH_OF_UNIQUE_IDENTIFIER,
	CORRECTNESS_INFORMATION_CANDIDATE_PREFIX,
	CORRECTNESS_INFORMATION_DELIMITER,
	CORRECTNESS_INFORMATION_LIST_PREFIX,
	MAXIMUM_ACTUAL_VOTING_OPTION_LENGTH
} from "../voting-options-constants";
import {TranslatableText} from "e-voting-libraries-ui-kit";

const BASE64_REGEX: string = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{4})$";
const BASE16_ALPHABET: string = "a-fA-F0-9";

/**
 * Validates that the input string is in Base16 alphabet with lowercase and has length of 32.
 * The alphabet corresponds to the uppercase and lowercase version of "Table 5: The Base 16 Alphabet" from RFC3548.
 *
 * @param {string} toValidate - the string to validate. Must be non-null.
 *
 * @throws NullPointerError if the input string is null.
 * @throws FailedValidationError if the input string validation fails.
 *
 * @returns {string} - the validated input string.
 */
export function validateUUID(toValidate: string): string {
	checkNotNull(toValidate);
	return validateInAlphabet(toValidate, `^[${BASE16_ALPHABET}]{${CHARACTER_LENGTH_OF_UNIQUE_IDENTIFIER}}$`);
}

/**
 * Validates that the input string is in Base64 lowercase alphabet including padding "=".
 * The alphabet corresponds to "Table 1: The Base 64 Alphabet" from RFC3548.
 *
 * @param {string} toValidate - the string to validate. Must be non-null.
 *
 * @throws NullPointerError if the input string is null.
 * @throws IllegalArgumentError if the input length is not a multiple of 4.
 * @throws FailedValidationError if the input string validation fails.
 *
 * @returns {string} - the validated input string.
 */
export function validateBase64String(toValidate: string): string {
	checkNotNull(toValidate);
	checkArgument(toValidate.length % 4 === 0);
	return validateInAlphabet(toValidate, BASE64_REGEX);
}

/**
 * Validates all actual voting options are a valid xml xs:token.
 * @param {ImmutableArray<string>} actualVotingOptions - the list of actual voting options. Must be non-null.
 *
 * @return {ImmutableArray<string>} the validated input array.
 */
export function validateActualVotingOptions(actualVotingOptions: ImmutableArray<string>): ImmutableArray<string> {
	checkNotNull(actualVotingOptions);
	actualVotingOptions.forEach(actualVotingOption => {
		validateActualVotingOption(actualVotingOption);
	});

	return actualVotingOptions;
}

/**
 * Validate an actual voting option is a valid combination of xml xs:token.
 * @param {string} actualVotingOption - the actual voting option. Must be non-null.
 *
 * @return {string} - the validated input string.
 */
export function validateActualVotingOption(actualVotingOption: string): string {
	checkNotNull(actualVotingOption);

	const identifications: string[] = actualVotingOption.split(ACTUAL_VOTING_OPTION_DELIMITER);
	checkArgument(identifications.length === 2 || identifications.length === 3,
		`The actual voting option should be either two or three identifications concatenated using ${ACTUAL_VOTING_OPTION_DELIMITER}.`);

	identifications.forEach(identification => {
		checkArgument(0 < identification.length && identification.length <= MAXIMUM_ACTUAL_VOTING_OPTION_LENGTH,
			`The length of each actual voting option must be in between 1 and ${MAXIMUM_ACTUAL_VOTING_OPTION_LENGTH}.`);
		validateXsToken(identification);
	});

	return actualVotingOption;
}

/**
 * Validate a semantic information is a valid non blank UTF8 string with the correct format.
 * @param {string} semanticInformation - the semantic information. Must be non-null.
 *
 * @return {string} - the validated input string.
 */
export function validateSemanticInformation(semanticInformation: string): string {
	validateNonBlankUCS(semanticInformation);

	const isValidPrefix: boolean = Object.values(VotingOptionType)
		.filter(votingOptionType => semanticInformation.startsWith(votingOptionType))
		.length === 1
	checkArgument(isValidPrefix, 'The semantic information prefix is not valid.')

	return semanticInformation;
}

/**
 * Validate a correctness information is a valid combination of xml xs:token.
 * @param {string} correctnessInformation - the correctness information. Must be non-null.
 *
 * @return {string} - the validated input string.
 */
export function validateCorrectnessInformation(correctnessInformation: string): string {
	checkNotNull(correctnessInformation);

	const identifications: string[] = correctnessInformation.split(CORRECTNESS_INFORMATION_DELIMITER);
	checkArgument(identifications.length === 1 || identifications.length === 2,
		`The correctness information should be either one identification or one prefix and one identification concatenated using ${CORRECTNESS_INFORMATION_DELIMITER}.`);

	if (identifications.length === 2) {
		const prefix: string = identifications[0];
		checkArgument(prefix === CORRECTNESS_INFORMATION_LIST_PREFIX || prefix === CORRECTNESS_INFORMATION_CANDIDATE_PREFIX,
			`The correctness information must have either ${CORRECTNESS_INFORMATION_LIST_PREFIX} or ${CORRECTNESS_INFORMATION_CANDIDATE_PREFIX} as prefix.`);
	}

	identifications.forEach(identification => validateXsToken(identification));

	return correctnessInformation;
}

/**
 * Validates a Ballot Casting Key.
 * @param {string} ballotCastingKey - the ballot casting key. Must be non-null.
 */
export function validateBallotCastingKey(ballotCastingKey: string): void {
	checkNotNull(ballotCastingKey);
	checkArgument(ballotCastingKey.length === CHARACTER_LENGTH_OF_BALLOT_CASTING_KEY, "The ballot casting key must have the correct size");
	checkArgument(ballotCastingKey.match("^\\d+$") != null, "The ballot casting key must be a numeric value");
	checkArgument(ballotCastingKey.match("^0+$") == null, "The ballot casting key must contain at least one non-zero element");
}

/**
 * Validates that the input string is a valid non blank UTF8 string.
 *
 * @param {string} toValidate - the string to validate. Must be non-null.
 *
 * @return {string} - the validated input string.
 */
export function validateNonBlankUCS(toValidate: string): string {
	checkNotNull(toValidate);
	checkArgument(toValidate.trim().length !== 0, "String to validate must not be blank.");

	const encoder: TextEncoder = new TextEncoder();

	try {
		// Check that s is a valid UTF-8 string
		const buffer: Uint8Array = encoder.encode(toValidate);
		new Uint8Array(buffer);
	} catch (e) {
		throw new Error("The string does not correspond to a valid sequence of UTF-8 encoding.");
	}
	return toValidate;
}

/**
 * Validates that the input string is a valid xml xs:token.
 *
 * @param {string} toValidate - the string to validate. Must be non-null.
 *
 * @returns {string}
 */
export function validateXsToken(toValidate: string): string {
	checkNotNull(toValidate);

	return validateInAlphabet(toValidate, `^[\\w-]{1,${MAXIMUM_ACTUAL_VOTING_OPTION_LENGTH}}$`);
}

/**
 * Validates the input string match the given pattern.
 *
 * @param {string} toValidate - the string to validate. Must be non-null.
 * @param {string} pattern - the pattern to match. Must be non-null.
 *
 * @returns {string}
 */
export function validateInAlphabet(toValidate: string, pattern: string): string {
	checkNotNull(toValidate);
	checkNotNull(pattern);

	const regExp: RegExp = new RegExp(pattern);
	if (!toValidate.match(regExp)) {
		throw new FailedValidationError(`The given string does not comply with the required format. [string: ${toValidate}, format: ${pattern}].`);
	}
	return toValidate;
}

/**
 * Validates that the translatable text contains the expected number of languages. And the values are valid non blank UTF8 string.
 *
 * @param {string} toValidate - the {@link TranslatableText} to validate. Must be non-null.
 *
 * @returns {string}
 */
export function validateTranslatableText(toValidate: TranslatableText): TranslatableText {
	checkNotNull(toValidate);

	const supportedLanguages: ImmutableArray<string> = ImmutableArray.of('DE', 'FR', 'IT', 'RM');
	checkArgument(supportedLanguages.length === Object.entries(toValidate).length, "The text must contain all languages.");

	Object.entries(toValidate).forEach(([language, translation]) => {
		checkArgument(supportedLanguages.elements().includes(language), `The language ${language} is not supported.`);
		validateNonBlankUCS(translation);
	});

	return toValidate;
}
