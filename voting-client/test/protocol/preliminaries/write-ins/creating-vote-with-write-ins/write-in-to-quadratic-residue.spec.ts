/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {ZqGroup} from "crypto-primitives-ts/lib/esm/math/zq_group";
import {LATIN_ALPHABET} from "../../../../../src/domain/latin-alphabet";
import {GqElement} from "crypto-primitives-ts/lib/esm/math/gq_element";
import {ZqElement} from "crypto-primitives-ts/lib/esm/math/zq_element";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {PrimitivesParams} from "../../../../../src/domain/primitives-params.types";
import {parsePrimitivesParams} from "../../../../../src/domain/primitives-params-parser";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {IllegalArgumentError} from "crypto-primitives-ts/lib/esm/error/illegal_argument_error";
import {writeInToQuadraticResidue} from "../../../../../src/protocol/preliminaries/write-ins/encoding-write-ins/write-in-to-quadratic-residue.algorithm";

import realValuesJson from "../../../../tools/data/write-in-to-quadratic-residue.json";
import authenticateVoterResponseJson from "../../../../tools/data/authenticate-voter-response.json";

describe("WriteIn to quadratic residue algorithm", (): void => {

	const args = [];
	const parametersList = JSON.parse(JSON.stringify(realValuesJson));
	parametersList.forEach(testParameters => {

		// Context.
		const p: ImmutableBigInteger = readValue(testParameters.context.p);
		const q: ImmutableBigInteger = readValue(testParameters.context.q);
		const g: ImmutableBigInteger = readValue(testParameters.context.g);

		const gqGroup: GqGroup = new GqGroup(p, q, g);

		// Parse input parameter.
		const s = testParameters.input.s;

		// Parse output parameters.
		let output: GqElement, error: string;
		if (testParameters.output.output) {
			output = GqElement.fromValue(readValue(testParameters.output.output), gqGroup);
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

	args.forEach((arg): void => {
		test(arg.description, (): void => {
			if (arg.output) {
				const actual: GqElement = writeInToQuadraticResidue({encryptionGroup: arg.encryptionGroup}, arg.s);
				expect(arg.output.equals(actual)).toBe(true);
			} else {
				expect(() => writeInToQuadraticResidue({encryptionGroup: arg.encryptionGroup}, arg.s))
					.toThrow(new IllegalArgumentError(arg.error));
			}
		});
	});

	describe("checking requires", (): void => {
		const primitivesParams: PrimitivesParams = parsePrimitivesParams(
			authenticateVoterResponseJson.votingClientPublicKeys,
			authenticateVoterResponseJson.primesMappingTable
		);
		const encryptionGroup: GqGroup = primitivesParams.encryptionGroup;

		test("with exponential form of a too large", (): void => {
			expect(() => writeInToQuadraticResidue({encryptionGroup: encryptionGroup}, "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet."))
				.toThrow(new IllegalArgumentError("The exponential form of a to s_length must be smaller than q."));
		});

		test("with empty characterString", (): void => {
			expect(() => writeInToQuadraticResidue({encryptionGroup: encryptionGroup}, ""))
				.toThrow(new IllegalArgumentError("The character string length must be greater than 0."));
		});

		test("with characterString starting with rank 0 character", (): void => {
			expect(() => writeInToQuadraticResidue({encryptionGroup: encryptionGroup}, LATIN_ALPHABET.get(0)))
				.toThrow(new IllegalArgumentError(`The character string must not start with rank 0 character. [rank 0 character: ${LATIN_ALPHABET.get(0)}]`));
		});

		test("then quadratic residue to WriteIn algorithm", (): void => {
			const original = "We want to get this one back."
			const transformed: GqElement = writeInToQuadraticResidue({encryptionGroup: encryptionGroup}, original);
			expect(quadraticResidueToWriteIn(transformed) === original).toBe(true);
		});
	});
});

function readValue(value: string): ImmutableBigInteger {
	return ImmutableBigInteger.fromString(value.substring(2), 16);
}

function quadraticResidueToWriteIn(quadraticResidue: GqElement): string {
	const gqGroup: GqGroup = quadraticResidue.group;
	const zqGroup = ZqGroup.sameOrderAs(gqGroup);
	const p: ImmutableBigInteger = gqGroup.p;
	const q: ImmutableBigInteger = gqGroup.q;

	// Input.
	const y: ImmutableBigInteger = quadraticResidue.value;

	// Operation.
	let x: ImmutableBigInteger = y.modPow(p.add(ImmutableBigInteger.ONE).divide(ImmutableBigInteger.fromNumber(4)), p);
	if (x.compareTo(q) > 0) {
		x = p.subtract(x);
	}

	return integerToWriteIn(ZqElement.create(x, zqGroup));
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