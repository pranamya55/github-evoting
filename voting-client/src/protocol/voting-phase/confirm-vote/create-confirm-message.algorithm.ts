/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GqElement} from "crypto-primitives-ts/lib/esm/math/gq_element";
import {ZqElement} from "crypto-primitives-ts/lib/esm/math/zq_element";
import {HashService} from "crypto-primitives-ts/lib/esm/hashing/hash_service";
import {stringToInteger} from "crypto-primitives-ts/lib/esm/conversions";
import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {CreateConfirmMessageContext} from "./create-confirm-message.types";
import {CHARACTER_LENGTH_OF_BALLOT_CASTING_KEY} from "../../../domain/voting-options-constants";

/**
 * Implements the CreateConfirmMessage algorithm described in the cryptographic protocol.
 * Generates a confirmation key.
 *
 * @param {CreateConfirmMessageContext} context - the CreateConfirmMessage context.
 * @param {string} ballotCastingKey - BCK_id, the ballot casting key.
 * @param {ZqElement} verificationCardSecretKey - k_id, the verification card secret key.
 * @returns {GqElement} - CK_id, the confirmation key.
 */
export function createConfirmMessage(
	context: CreateConfirmMessageContext,
	ballotCastingKey: string,
	verificationCardSecretKey: ZqElement
): GqElement {
	checkNotNull(context);

	// Context.
	const p_q_g: GqGroup = checkNotNull(context.encryptionGroup);

	// Input.
	const BCK_id: string = checkNotNull(ballotCastingKey);
	const k_id: ZqElement = checkNotNull(verificationCardSecretKey);

	const l_BCK: number = CHARACTER_LENGTH_OF_BALLOT_CASTING_KEY;
	checkArgument(BCK_id.length === l_BCK, `The ballot casting key length must be ${l_BCK}`);
	checkArgument(BCK_id.match("^\\d+$") != null, "The ballot casting key must be a numeric value");
	checkArgument(BCK_id.match("^0+$") == null, "The ballot casting key must contain at least one non-zero element");

	const hashService: HashService = new HashService();

	// Cross-checks.
	checkArgument(p_q_g.hasSameOrderAs(k_id.group),
		"The encryption group of the context must equal the order of the verification card secret key.");

	// Operation.
	const hBCK_id: GqElement = hashService.hashAndSquare(stringToInteger(BCK_id), p_q_g);

	// Output.
	// CK_id
	return hBCK_id.exponentiate(k_id);
}
