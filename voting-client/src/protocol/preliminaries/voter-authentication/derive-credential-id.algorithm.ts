/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {HashService} from "crypto-primitives-ts/lib/esm/hashing/hash_service";
import {Base16Service} from "crypto-primitives-ts/lib/esm/math/base16_service";
import {Argon2Profile} from "crypto-primitives-ts/lib/esm/hashing/argon2_profile";
import {Argon2Service} from "crypto-primitives-ts/lib/esm/hashing/argon2_service";
import {cutToBitLength} from "crypto-primitives-ts/lib/esm/arrays";
import {stringToByteArray} from "crypto-primitives-ts/lib/esm/conversions";
import {ImmutableUint8Array} from "crypto-primitives-ts/lib/esm/immutable_uint8Array";
import {validateUUID} from "../../../domain/validations/validations";
import {validateSVK} from "../../../domain/validations/start-voting-key-validation";
import {DeriveCredentialIdContext} from "./derive-credential-id.types";
import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";

/**
 * Derives a voter's identifier credentialId from the Start Voting Key SVK_id.
 *
 * @param {DeriveCredentialIdContext} context - the DeriveCredentialId context.
 * @param {string} startVotingKey - SVK_id, the start voting key.
 *
 * @return {Promise<string>} - credentialID_id, the derived credentialID_id.
 */
export async function deriveCredentialId(
	context: DeriveCredentialIdContext,
	startVotingKey: string
): Promise<string> {
	// Context.
	checkNotNull(context);
	const ee: string = validateUUID(context.electionEventId);

	// Input.
	const SVK_id: string = validateSVK(startVotingKey);

	const hashService: HashService = new HashService();
	const base16Service: Base16Service = new Base16Service();
	const argon2Service: Argon2Service = new Argon2Service(Argon2Profile.LESS_MEMORY);

	// Operation.
	const salt: ImmutableUint8Array = cutToBitLength(hashService.recursiveHash(ee, "credentialId"), 128);

	const bCredentialID_id: ImmutableUint8Array = await argon2Service.getArgon2id(stringToByteArray(SVK_id), salt);

	// Output.
	// credentialID_id
	return base16Service.base16Encode(cutToBitLength(bCredentialID_id, 128).value());
}
