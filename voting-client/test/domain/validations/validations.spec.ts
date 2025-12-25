/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {Base64Service} from "crypto-primitives-ts/lib/esm/math/base64_service";
import {RandomService} from "crypto-primitives-ts/lib/esm/math/random_service";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {BASE64_ALPHABET} from "crypto-primitives-ts/lib/esm/math/base64-alphabet";
import {NullPointerError} from "crypto-primitives-ts/lib/esm/error/null_pointer_error";
import {stringToByteArray} from "crypto-primitives-ts/lib/esm/conversions";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {ImmutableUint8Array} from "crypto-primitives-ts/lib/esm/immutable_uint8Array";
import {IllegalArgumentError} from "crypto-primitives-ts/lib/esm/error/illegal_argument_error";
import {FailedValidationError} from "../../../src/domain/validations/failed-validation-error";
import {
	validateActualVotingOptions,
	validateBallotCastingKey,
	validateBase64String,
	validateNonBlankUCS,
	validateSemanticInformation,
	validateUUID,
	validateXsToken
} from "../../../src/domain/validations/validations";

import authenticateVoterResponseJson from "../../tools/data/authenticate-voter-response.json";

describe('Validations methods', function (): void {
	describe('validateUUID', function (): void {
		test('with valid argument should validate and return a UUID', function (): void {
			expect(() => validateUUID(authenticateVoterResponseJson.voterAuthenticationData.electionEventId)).not.toThrow();
		});

		test('with null argument should throw a NullPointerError', function (): void {
			expect(() => validateUUID(null)).toThrow(new NullPointerError());
		});
	})

	describe('validateBase64String', function (): void {
		test('with null argument should throw NullPointerError', function (): void {
			expect(() => validateBase64String(null))
				.toThrow(new NullPointerError());
		});

		test('with argument of invalid size should throw an IllegalArgumentError', function (): void {
			const invalidSizeStrings = ['ab=', 'abcdef='];

			invalidSizeStrings.forEach(invalidSizeString => {
				expect(() => validateBase64String(invalidSizeString))
					.toThrow(new IllegalArgumentError());
			});
		});

		test('with empty argument should throw an FailedValidationError', function (): void {
			expect(() => validateBase64String(''))
				.toThrow(FailedValidationError);
		});

		test('with arguments not in alphabet should throw a FailedValidationError', function (): void {
			const invalidBase64Strings = ['àb==', 'àbç=', 'àbçd'];

			invalidBase64Strings.forEach(invalidBase64String => {
				expect(() => validateBase64String(invalidBase64String))
					.toThrow(FailedValidationError);
			});
		});

		test('with valid arguments should validate and return a base64 string', function (): void {
			const validBase64Strings = ['ab==', 'abc=', 'abcd', 'abcdef==', 'abcdefg=', 'abcdefgh'];

			validBase64Strings.forEach(validBase64String => {
				expect(validateBase64String(validBase64String))
					.toBe(validBase64String);
			});
		});

		test('with 100 random arguments should validate and return a base64 string or throw', (): void => {
			const randomService: RandomService = new RandomService();
			const base64service: Base64Service = new Base64Service();
			for(let i = 0; i < 100; i++) {
				const randomInteger: ImmutableBigInteger = randomService.genRandomInteger(ImmutableBigInteger.fromNumber(100));
				const randomString: string = randomService.genRandomString(randomInteger.intValue(), BASE64_ALPHABET);
				const convertedToByteArray: ImmutableUint8Array = stringToByteArray(randomString);
				const randomBase64String: string = base64service.base64Encode(convertedToByteArray.value());
				if (randomBase64String.length === 0) {
					expect(() => validateBase64String(randomBase64String)).toThrow(FailedValidationError);
				} else if (randomBase64String.length % 4 === 0) {
					expect(validateBase64String(randomBase64String)).toBe(randomBase64String);
				}
			}
		});


	});

	describe('validateActualVotingOptions', function (): void {
		test('should fail with invalid XML xs:token actual voting option', function (): void {
			const invalidXMLTokens = ['aposidvnbq13458zœ¶@¼←“þ“ ¢@]œ“→”@µ€ĸ@{þ|other-token', ' spacestart|other-token', 'space between|other-token', 'spaceend|other-token ', 'pk23]|other-token', 'asdacà!32|other-token', ' a p2o3m|other-token', '<xyz>|other-token'];

			invalidXMLTokens.forEach(invalidXMLToken => {
				expect(() => validateActualVotingOptions(ImmutableArray.of(invalidXMLToken)))
					.toThrow(FailedValidationError);
			});
		});

		test('should accept actual voting option when valid XML xs:token', function (): void {
			const validXMLTokens = ['123-456|other-token', 'abc123|other-token', '789xyz|other-token', 'xyz|other-token', 'a_token|other-token', 'another|token|0'];

			validXMLTokens.forEach(validXMLToken => {
				expect(() => validateActualVotingOptions(ImmutableArray.of(validXMLToken)))
					.not.toThrow();
			});
		});
	});

	describe('validateSemanticInformation', function (): void {
		test('should fail with blank semantic information', function (): void {
			expect(() => validateSemanticInformation(' '))
				.toThrow(new IllegalArgumentError('String to validate must not be blank.'));
		});

		test('should fail with invalid prefix semantic information', function (): void {
			const invalidPrefix = 'NON__BLANK|First|Jack|JJ|1956-02-02';

			expect(() => validateSemanticInformation(invalidPrefix))
				.toThrow(new IllegalArgumentError('The semantic information prefix is not valid.'));
		});

		test('should accept semantic information when valid UTF8 and prefix', function (): void {
			const validSemanticInformations = ['NON_BLANK|Vier|Kandidat|Dagmar|1968-01-01', 'BLANK|EMPTY_CANDIDATE_POSITION-3', 'WRITE_IN|WRITE_IN_POSITION-3'];

			validSemanticInformations.forEach(validSemanticInformation => {
				expect(() => validateSemanticInformation(validSemanticInformation))
					.not.toThrow();
			});
		});
	});

	describe('validateBallotCastingKey', function () {
		test('with null argument should throw a NullPointerError', () => {
			expect(() => validateBallotCastingKey(null)).toThrow(new NullPointerError());
		});

		test('with ballot casting key wrong size should throw an IllegalArgumentError', () => {
			expect(() => validateBallotCastingKey('12345678')).toThrow(new IllegalArgumentError("The ballot casting key must have the correct size"));
			expect(() => validateBallotCastingKey('1234567890')).toThrow(new IllegalArgumentError("The ballot casting key must have the correct size"));
		});

		test('with non numerical values should throw an IllegalArgumentError', function (): void {
			expect(() => validateBallotCastingKey('1234P6789')).toThrow(new IllegalArgumentError("The ballot casting key must be a numeric value"));
		});

		test(' with all zero string should throw an IllegalArgumentError', function (): void {
			expect(() => validateBallotCastingKey('000000000')).toThrow(new IllegalArgumentError("The ballot casting key must contain at least one non-zero element"));
		});
	});

	describe('validateNonBlankUCS', function (): void {
		test('with null argument should throw a NullPointerError', function (): void {
			expect(() => validateNonBlankUCS(null)).toThrow(new NullPointerError());
		});

		test('should throw an IllegalArgumentError', function (): void {
			expect(() => validateNonBlankUCS('  ')).toThrow(new IllegalArgumentError("String to validate must not be blank."));
		});

		test('with valid input should return input', function (): void {
			const validInput = 'Valid input with (€&%)';
			expect(validateNonBlankUCS(validInput)).toBe(validInput);
		});
	});

	describe('validateXsToken', function (): void {
		test('with null argument should throw a NullPointerError', function (): void {
			expect(() => validateXsToken(null)).toThrow(new NullPointerError());
		});

		test('with an invalid token should throw a FailedValidationError', function (): void {
			const invalidXMLTokens = [' a  b', 'abc  ', 'aposidvnbq13458zœ¶@¼←“þ“ ¢@]œ“→”@µ€ĸ@{þ', ' spacestart', 'space between', 'spaceend ', 'pk23]', 'asdacà!32', ' a p2o3m', '<xyz>'];

			invalidXMLTokens.forEach(invalidXMLToken => {
				expect(() => validateXsToken(invalidXMLToken))
					.toThrow(FailedValidationError);
			});
		});

		test('with a valid xs:token should return that token', function (): void {
			const validXMLTokens = ['123-456', 'abc123', '789xyz', 'xyz', 'a_token', 'another-token', 'aposidvnbq13458z'];

			validXMLTokens.forEach(validXMLToken => {
				expect(validateXsToken(validXMLToken)).toBe(validXMLToken);
			});
		});
	});

});
