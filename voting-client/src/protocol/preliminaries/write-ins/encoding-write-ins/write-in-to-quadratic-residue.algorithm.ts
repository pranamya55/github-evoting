/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GqElement} from "crypto-primitives-ts/lib/esm/math/gq_element";
import {ZqElement} from "crypto-primitives-ts/lib/esm/math/zq_element";
import {LATIN_ALPHABET} from "../../../../domain/latin-alphabet";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {checkExpLength, writeInToInteger} from "./write-in-to-integer.algorithm";
import {WriteInToQuadraticResidueContext} from "./write-ins.types";

/**
 * Implements the WriteInToQuadraticResidue algorithm described in the cryptographic protocol.
 * Maps a character string to a value ∈ Gq.
 *
 * @param {WriteInToQuadraticResidueContext} context - the WriteInToQuadraticResidue context.
 * @param {string} characterString - s, the character string to map.
 * @returns {GqElement} - y, the mapped value.
 */
export function writeInToQuadraticResidue(
	context: WriteInToQuadraticResidueContext,
	characterString: string
): GqElement {
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
	checkArgument(checkExpLength(a, s_length, p_q_g),
		"The exponential form of a to s_length must be smaller than q.");
	checkArgument(s_length.intValue() > 0, "The character string length must be greater than 0.");
	checkArgument(!s.startsWith(LATIN_ALPHABET.get(0)), `The character string must not start with rank 0 character. [rank 0 character: ${LATIN_ALPHABET.get(0)}]`);

	// Operation.
	const x: ZqElement = writeInToInteger({encryptionGroup: p_q_g}, s);

	// Output.
	return GqElement.fromSquareRoot(x.value, p_q_g);
}
