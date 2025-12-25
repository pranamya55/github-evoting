/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {ZqGroup} from "crypto-primitives-ts/lib/esm/math/zq_group";
import {ZqElement} from "crypto-primitives-ts/lib/esm/math/zq_element";
import {getGqGroup} from "../../../../tools/data/group-test-data";
import {PrimitivesParams} from "../../../../../src/domain/primitives-params.types";
import {LATIN_ALPHABET} from "../../../../../src/domain/latin-alphabet";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {parsePrimitivesParams} from "../../../../../src/domain/primitives-params-parser";
import {checkExpLength, writeInToInteger} from "../../../../../src/protocol/preliminaries/write-ins/encoding-write-ins/write-in-to-integer.algorithm";

import realValuesJson from "../../../../tools/data/write-in-to-integer.json";
import authenticateVoterResponseJson from "../../../../tools/data/authenticate-voter-response.json";
import {IllegalArgumentError} from "crypto-primitives-ts/lib/esm/error/illegal_argument_error";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";

describe("WriteIn to integer algorithm", (): void => {

	describe("with real values", (): void => {

		interface JsonFileArgument {
			encryptionGroup: GqGroup;
			s: string;
			output: ZqElement;
			error: string;
			description: string;
		}

		function jsonFileArgumentProvider(): JsonFileArgument[] {
			const parametersList = JSON.parse(JSON.stringify(realValuesJson));

			const args: JsonFileArgument[] = [];
			parametersList.forEach(testParameters => {

				// Context.
				const p: ImmutableBigInteger = readValue(testParameters.context.p);
				const q: ImmutableBigInteger = readValue(testParameters.context.q);
				const g: ImmutableBigInteger = readValue(testParameters.context.g);

				const gqGroup: GqGroup = new GqGroup(p, q, g);
				const zqGroup: ZqGroup = new ZqGroup(q);

				// Parse input parameter.
				const s: string = testParameters.input.s;

				// Parse output parameters.
				let output: ZqElement, error: string;
				if (testParameters.output.output) {
					output = ZqElement.create(readValue(testParameters.output.output), zqGroup);
				} else {
					error = testParameters.output.error;
				}

				args.push({
					encryptionGroup: gqGroup,
					s: s,
					output: output,
					error: error,
					description: testParameters.description
				});
			});
			return args;
		}

		test.each(jsonFileArgumentProvider())("$description", ({
																   encryptionGroup,
																   s,
																   output,
																   error,
																   description
															   }) => {
			if (output) {
				const actual: ZqElement = writeInToInteger({encryptionGroup: encryptionGroup}, s);
				expect(output.equals(actual)).toBe(true);
			} else {
				expect(() => writeInToInteger({encryptionGroup: encryptionGroup}, s))
					.toThrow(new IllegalArgumentError(error));
			}
		})

	});

	describe("checking requires", (): void => {
		const primitivesParams: PrimitivesParams = parsePrimitivesParams(
			authenticateVoterResponseJson.votingClientPublicKeys,
			authenticateVoterResponseJson.primesMappingTable
		);
		const encryptionGroup: GqGroup = primitivesParams.encryptionGroup;

		test("with exponential form of a too large", (): void => {
			expect(() => writeInToInteger({encryptionGroup: encryptionGroup}, "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet."))
				.toThrow(new IllegalArgumentError("The exponential form of a to s_length must be smaller than q."));
		});

		test("with empty characterString", (): void => {
			expect(() => writeInToInteger({encryptionGroup: encryptionGroup}, ""))
				.toThrow(new IllegalArgumentError("The character string length must be greater than 0."));
		});

		test("with characterString starting with rank 0 character", (): void => {
			expect(() => writeInToInteger({encryptionGroup: encryptionGroup}, LATIN_ALPHABET.get(0)))
				.toThrow(new IllegalArgumentError(`The character string must not start with rank 0 character. [rank 0 character: ${LATIN_ALPHABET.get(0)}]`));
		});

		test("then integer to WriteIn algorithm", (): void => {
			const original = "We want to get this one back."
			const transformed: ZqElement = writeInToInteger({encryptionGroup: encryptionGroup}, original);
			expect(integerToWriteIn(transformed) === original).toBe(true);
		});
	});
});

describe("checkExpLength method", (): void => {
	const primitivesParams: PrimitivesParams = parsePrimitivesParams(
		authenticateVoterResponseJson.votingClientPublicKeys,
		authenticateVoterResponseJson.primesMappingTable
	);
	const encryptionGroup: GqGroup = primitivesParams.encryptionGroup;
	const a_int: number = LATIN_ALPHABET.length;
	const s_length_limit: number = Math.floor(encryptionGroup.q.bitLength() / Math.log2(a_int));

	test("should return true with exponential result smaller than q", function (): void {
		const a: ImmutableBigInteger = ImmutableBigInteger.fromNumber(a_int);
		const s_length: ImmutableBigInteger = ImmutableBigInteger.fromNumber(s_length_limit - 1);

		const actual: boolean = checkExpLength(a, s_length, encryptionGroup);

		expect(actual).toBe(true);
	});

	test("should return false with exponential result bigger than q", function (): void {
		const a: ImmutableBigInteger = ImmutableBigInteger.fromNumber(a_int);
		const s_length: ImmutableBigInteger = ImmutableBigInteger.fromNumber(s_length_limit + 1);

		const actual: boolean = checkExpLength(a, s_length, encryptionGroup);

		expect(actual).toBe(false);
	});

	test("should return a result with exponential result equal to q", function (): void {
		for (let i: number = 0; i < 100; i++) {
			const smallGroup: GqGroup = getGqGroup();
			const q_bitLength: number = smallGroup.q.bitLength();
			const a: ImmutableBigInteger = ImmutableBigInteger.fromNumber(2 ** q_bitLength);
			const s_length: ImmutableBigInteger = ImmutableBigInteger.fromNumber(1);

			const actual: boolean = checkExpLength(a, s_length, smallGroup);

			expect(actual).toBe(false);
		}
	});
});

function readValue(value: string): ImmutableBigInteger {
	return ImmutableBigInteger.fromString(value.substring(2), 16);
}

function integerToWriteIn(encoding: ZqElement): string {

	// Input.
	let x: ImmutableBigInteger = encoding.value;

	const A_latin: ImmutableArray<string> = LATIN_ALPHABET;
	const a: ImmutableBigInteger = ImmutableBigInteger.fromNumber(A_latin.length);

	// Operation.
	const s: string[] = [];
	while (x.signum() > 0) {
		const b: ImmutableBigInteger = x.mod(a);
		const c: string = A_latin.get(b.intValue());
		s.push(c);
		x = x.subtract(b).divide(a);
	}

	return s.reverse().join(""); // push adds at the end, so we need to reverse
}