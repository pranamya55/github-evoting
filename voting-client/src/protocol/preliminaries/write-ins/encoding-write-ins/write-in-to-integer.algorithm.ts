/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {ZqGroup} from "crypto-primitives-ts/lib/esm/math/zq_group";
import {ZqElement} from "crypto-primitives-ts/lib/esm/math/zq_element";
import {LATIN_ALPHABET} from "../../../../domain/latin-alphabet";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {WriteInToIntegerContext} from "./write-ins.types";

/**
 * Implements the WriteInToInteger algorithm described in the cryptographic protocol.
 * Maps a character string to an integer value.
 *
 * @param {WriteInToIntegerContext} context - the WriteInToInteger context.
 * @param {string} characterString - s, the character string to map.
 *
 * @returns {ZqElement} - x, the mapped value.
 */
export function writeInToInteger(
	context: WriteInToIntegerContext,
	characterString: string
): ZqElement {
	checkNotNull(context);

	// Context.
	const p_q_g: GqGroup = checkNotNull(context.encryptionGroup);

	// Input.
	const s: string = checkNotNull(characterString);
	checkArgument(s.split('').every(char => LATIN_ALPHABET.includes(char)),
		"All characters in write-in must be in A_latin alphabet.");

	// Require.
	const a: ImmutableBigInteger = ImmutableBigInteger.fromNumber(LATIN_ALPHABET.length);
	const s_length: ImmutableBigInteger = ImmutableBigInteger.fromNumber(s.length);
	checkArgument(checkExpLength(a, s_length, p_q_g), "The exponential form of a to s_length must be smaller than q.");
	checkArgument(s_length.intValue() > 0, "The character string length must be greater than 0.");
	checkArgument(!s.startsWith(LATIN_ALPHABET.get(0)), `The character string must not start with rank 0 character. [rank 0 character: ${LATIN_ALPHABET.get(0)}]`);

	// Operation.
	let x: ImmutableBigInteger = ImmutableBigInteger.fromNumber(0);
	for (let i: number = 0; i < s.length; i++) {
		const c: string = s.charAt(i);
		const b: ImmutableBigInteger = ImmutableBigInteger.fromNumber(LATIN_ALPHABET.indexOf(c));

		x = x.multiply(a).add(b);
	}

	// Output.
	return ZqElement.create(x, ZqGroup.sameOrderAs(p_q_g));
}

/**
 * Provides a check on the exponential form of a to s_length compared to q.
 * We use logarithm operation on lengths instead of exponentiation on big numbers, because exponentiation can be expensive.
 * Corresponds to a ** s_length < q.
 *
 * @param {ImmutableBigInteger} a - the write-in alphabet length.
 * @param {ImmutableBigInteger} s_length - the string length.
 * @param {GqGroup} encryptionGroup - the encryption group.
 * @returns {boolean} - true if the exponential form of a to s_length is smaller than q.
 */
export function checkExpLength(a: ImmutableBigInteger, s_length: ImmutableBigInteger, encryptionGroup: GqGroup): boolean {
	checkNotNull(a);
	checkNotNull(s_length);
	checkNotNull(encryptionGroup);
	const a_int: number = a.intValue();
	const s_length_int: number = s_length.intValue();
	const q_bitLength: number = encryptionGroup.q.bitLength();
	const p: ImmutableBigInteger = encryptionGroup.p;

	const exp: number = Math.ceil(Math.log2(a_int) * s_length_int);

	if (exp < q_bitLength) {
		return true;
	}
	if (exp > q_bitLength) {
		return false;
	}
	return a.modPow(s_length, p).intValue() < q_bitLength;
}

