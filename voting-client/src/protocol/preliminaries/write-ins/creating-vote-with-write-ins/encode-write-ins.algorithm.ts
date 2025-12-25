/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GqElement} from "crypto-primitives-ts/lib/esm/math/gq_element";
import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {LATIN_ALPHABET} from "../../../../domain/latin-alphabet";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {EncodeWriteInsContext} from "./encode-write-ins.types";
import {writeInToQuadraticResidue} from "../encoding-write-ins/write-in-to-quadratic-residue.algorithm";
import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS} from "../../../../domain/voting-options-constants";

/**
 * Implements the EncodeWriteIns algorithm described in the cryptographic protocol.
 * Provides a vector of values ∈ Gq for all allowed write-ins positions.
 *
 * @param {EncodeWriteInsContext} context - the EncodeWriteIns context.
 * @param {ImmutableArray<string>} selectedWriteIns - s_hat, the vector of selected write-ins.
 * @returns {GqElement} - w, the encoded write-ins.
 */
export function encodeWriteIns(
	context: EncodeWriteInsContext,
	selectedWriteIns: ImmutableArray<string>
): GroupVector<GqElement, GqGroup> {
	// Context.
	checkNotNull(context);
	const p_q_g: GqGroup = checkNotNull(context.encryptionGroup);
	const delta: number = context.numberOfAllowedWriteInsPlusOne;
	checkArgument(delta <= MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1,
		`The number of allowed write-ins + 1 must be smaller or equal to the maximum supported number of write-ins + 1. [delta: ${delta}, delta_sup: ${MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1}]`)

	// Input.
	const s_hat: ImmutableArray<string> = checkNotNull(selectedWriteIns);
	checkArgument(s_hat.every(s_i_hat => s_i_hat.split('').every(char => LATIN_ALPHABET.includes(char))),
		"All characters in each selected write-in must be in A_latin alphabet.");

	// Require.
	const k: number = s_hat.length;
	checkArgument(k <= delta - 1, `There must be at most delta - 1 selected write-ins. [k: ${k}, delta: ${delta}]`);

	// Operation.
	let w: GqElement[] = [];
	for (let i: number = 0; i < k; i++) {
		w[i] = writeInToQuadraticResidue({encryptionGroup: p_q_g}, s_hat.get(i));
	}

	const writeInDummyValue: GqElement = GqElement.fromValue(ImmutableBigInteger.ONE, p_q_g);
	for (let i: number = k; i < delta - 1; i++) {
		w[i] = writeInDummyValue;
	}

	// Output.
	return GroupVector.from(w);
}
