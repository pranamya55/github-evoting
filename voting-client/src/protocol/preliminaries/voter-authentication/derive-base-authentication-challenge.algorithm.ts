/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {HashService} from "crypto-primitives-ts/lib/esm/hashing/hash_service";
import {Argon2Profile} from "crypto-primitives-ts/lib/esm/hashing/argon2_profile";
import {Argon2Service} from "crypto-primitives-ts/lib/esm/hashing/argon2_service";
import {Base64Service} from "crypto-primitives-ts/lib/esm/math/base64_service";
import {cutToBitLength} from "crypto-primitives-ts/lib/esm/arrays";
import {stringToByteArray} from "crypto-primitives-ts/lib/esm/conversions";
import {ImmutableUint8Array} from "crypto-primitives-ts/lib/esm/immutable_uint8Array";
import {validateEA} from "../../../domain/validations/extended-authentication-factor-validation";
import {validateSVK} from "../../../domain/validations/start-voting-key-validation";
import {validateUUID} from "../../../domain/validations/validations";
import {DeriveBaseAuthenticationChallengeContext} from "./derive-base-authentication-challenge.types";
import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";


/**
 * Derives the base authentication challenge for the given election event from the given start voting key and extended authentication factor
 *
 * 	 *                                           extended authentication factor character lengths.
 * @param {DeriveBaseAuthenticationChallengeContext} context - the DeriveBaseAuthenticationChallenge context.
 * @param {string} startVotingKey - SVK<sub>id</sub>, a start voting key. Must be a valid Base32 string without padding of length l<sub>SVK</sub>.
 * @param {string} extendedAuthenticationFactor - EA<sub>id</sub>, an extended authentication factor. Must be a valid Base10 string of length l<sub>EA</sub>.
 *
 * @return {Promise<string>} hAuth<sub>id</sub> the base authentication challenge as a string.
 *
 * @throws NullPointerError if any of the inputs is null.
 * @throws FailedValidationError if
 * <ul>
 *     <li>the election event id is not a valid UUID</li>
 *     <li>the start voting key is not a valid Base32 string</li>
 * </ul>
 * @throws IllegalArgumentError if
 * <ul>
 *     <li>the start voting key is not of size l<sub>SVK</sub></li>
 *     <li>the extended authentication factor is not a valid Base10 string</li>
 * </ul>
 */
export async function deriveBaseAuthenticationChallenge(
	context: DeriveBaseAuthenticationChallengeContext,
	startVotingKey: string,
	extendedAuthenticationFactor: string
): Promise<string> {

	// Context.
	checkNotNull(context);
	const ee: string = validateUUID(context.electionEventId);
	const l_EA: number = context.extendedAuthenticationFactorLength;

	// Input.
	const SVK_id: string = validateSVK(startVotingKey);
	const EA_id: string = validateEA(extendedAuthenticationFactor, l_EA);

	const hashService: HashService = new HashService();
	const argon2Service: Argon2Service = new Argon2Service(Argon2Profile.LESS_MEMORY);
	const base64Service: Base64Service = new Base64Service();

	// Operation.
	const salt_auth: ImmutableUint8Array = cutToBitLength(hashService.recursiveHash(ee, "hAuth"), 128);

	const k: number[] = [...stringToByteArray(EA_id).value(), ...stringToByteArray("Auth").value(), ...stringToByteArray(SVK_id).value()];
	const bhAuth_id: ImmutableUint8Array = await argon2Service.getArgon2id(ImmutableUint8Array.from(k), salt_auth);

	// Output.
	// hAuth_id
	return base64Service.base64Encode(bhAuth_id.value());
}
