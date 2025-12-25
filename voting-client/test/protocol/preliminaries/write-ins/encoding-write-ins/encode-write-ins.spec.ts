/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {encodeWriteIns} from "../../../../../src/protocol/preliminaries/write-ins/creating-vote-with-write-ins/encode-write-ins.algorithm";
import {NullPointerError} from "crypto-primitives-ts/lib/esm/error/null_pointer_error";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {IllegalArgumentError} from "crypto-primitives-ts/lib/esm/error/illegal_argument_error";

import realValuesJson from "../../../../tools/data/encode-write-ins.json";
import {EncodeWriteInsContext} from "../../../../../src/protocol/preliminaries/write-ins/creating-vote-with-write-ins/encode-write-ins.types";
import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {GqElement} from "crypto-primitives-ts/lib/esm/math/gq_element";
import {deserializeImmutableBigInteger} from "../../../../../src/domain/primitives-deserializer";

describe("Encode WriteIns algorithm", (): void => {
	const group: GqGroup = new GqGroup(ImmutableBigInteger.fromNumber(59), ImmutableBigInteger.fromNumber(29), ImmutableBigInteger.fromNumber(3));

	test("with null values throws", (): void => {
		expect(() => encodeWriteIns(null, ImmutableArray.of())).toThrow(new NullPointerError());
		expect(() => encodeWriteIns({encryptionGroup: null, numberOfAllowedWriteInsPlusOne: 0}, ImmutableArray.of())).toThrow(new NullPointerError());
		expect(() => encodeWriteIns({encryptionGroup: group, numberOfAllowedWriteInsPlusOne: 0}, null)).toThrow(new NullPointerError());
	});

	test("with invalid parameters throws", (): void => {
		expect(() => encodeWriteIns({encryptionGroup: group, numberOfAllowedWriteInsPlusOne: 32}, ImmutableArray.of())).toThrow(IllegalArgumentError);
		expect(() => encodeWriteIns({encryptionGroup: group, numberOfAllowedWriteInsPlusOne: 1}, ImmutableArray.of("<>"))).toThrow(IllegalArgumentError);
		expect(() => encodeWriteIns({encryptionGroup: group, numberOfAllowedWriteInsPlusOne: 0}, ImmutableArray.of("Name 1", "Name 2"))).toThrow(IllegalArgumentError);
	});

	describe("with real values", (): void => {
		const parametersList = JSON.parse(JSON.stringify(realValuesJson));
		parametersList.forEach(testParameters => {

			// Context.
			const p: ImmutableBigInteger = deserializeImmutableBigInteger(testParameters.context.p);
			const q: ImmutableBigInteger = deserializeImmutableBigInteger(testParameters.context.q);
			const g: ImmutableBigInteger = deserializeImmutableBigInteger(testParameters.context.g);
			const gqGroup: GqGroup = new GqGroup(p, q, g);
			const delta: number = testParameters.context.delta;
			const context: EncodeWriteInsContext = {encryptionGroup: gqGroup, numberOfAllowedWriteInsPlusOne: delta};

			// Input.
			const selectedWriteIns: ImmutableArray<string> = ImmutableArray.from(testParameters.input.s_hat);

			// Output.
			const expected: GroupVector<GqElement, GqGroup> = GroupVector.from(testParameters.output.w.map((writeIn: string) => {
				const writeInValue = deserializeImmutableBigInteger(writeIn);
				return GqElement.fromValue(writeInValue, gqGroup);
			}));

			test(testParameters.description, (): void => {
				const result = encodeWriteIns(context, selectedWriteIns);
				expect(result.equals(expected)).toBe(true);
			})
		});
	});
});