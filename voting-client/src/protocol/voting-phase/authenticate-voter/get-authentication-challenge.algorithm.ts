/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {HashService} from "crypto-primitives-ts/lib/esm/hashing/hash_service";
import {Argon2Profile} from "crypto-primitives-ts/lib/esm/hashing/argon2_profile";
import {Argon2Service} from "crypto-primitives-ts/lib/esm/hashing/argon2_service";
import {Base64Service} from "crypto-primitives-ts/lib/esm/math/base64_service";
import {RandomService} from "crypto-primitives-ts/lib/esm/math/random_service";
import {cutToBitLength} from "crypto-primitives-ts/lib/esm/arrays";
import {deriveCredentialId} from "../../preliminaries/voter-authentication/derive-credential-id.algorithm";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {ImmutableUint8Array} from "crypto-primitives-ts/lib/esm/immutable_uint8Array";
import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {deriveBaseAuthenticationChallenge} from "../../preliminaries/voter-authentication/derive-base-authentication-challenge.algorithm";
import {integerToByteArray, stringToByteArray} from "crypto-primitives-ts/lib/esm/conversions";
import {AuthenticationChallengeContext, AuthenticationChallengeOutput} from "./get-authentication-challenge.types";
import {validateUUID} from "../../../domain/validations/validations";
import {validateSVK} from "../../../domain/validations/start-voting-key-validation";
import {validateEA} from "../../../domain/validations/extended-authentication-factor-validation";

const AUTHENTICATION_STEPS: string[] = ["authenticateVoter", "sendVote", "confirmVote"];
const TWO_POW_256: ImmutableBigInteger = ImmutableBigInteger.ONE.shiftLeft(256);

/**
 * Implements the GetAuthenticationChallenge algorithm described in the cryptographic protocol.
 *
 * @param {AuthenticationChallengeContext} context - the AuthenticationChallenge context.
 * @param {string} authenticationStep - authStep, the corresponding authentication step.
 * @param {string} startVotingKey - svk_id, the start voting key.
 * @param {string} extendedAuthenticationFactor - EA_id, the extended authentication factor.
 *
 * @returns {AuthenticationChallengeOutput} - the AuthenticationChallenge output.
 */
export async function getAuthenticationChallenge(
	context: AuthenticationChallengeContext,
	authenticationStep: string,
	startVotingKey: string,
	extendedAuthenticationFactor: string
): Promise<AuthenticationChallengeOutput> {
	checkNotNull(context);

	// Context.
	const ee: string = validateUUID(context.electionEventId);
	const l_EA: number = context.extendedAuthenticationFactorLength;

	// Input.
	const authStep: string = checkNotNull(authenticationStep);
	checkArgument(AUTHENTICATION_STEPS.includes(authStep), "The authentication step must be one of the valid values.");
	const SVK_id: string = validateSVK(startVotingKey);
	const EA_id: string = validateEA(extendedAuthenticationFactor, l_EA);

	const hashService: HashService = new HashService();
	const argon2Service: Argon2Service = new Argon2Service(Argon2Profile.LESS_MEMORY);
	const base64Service: Base64Service = new Base64Service();

	// Operation.
	const credentialID_id: string = await deriveCredentialId({
		electionEventId: ee
	}, SVK_id);
	const hAuth_id: string = await deriveBaseAuthenticationChallenge({
		electionEventId: ee,
		extendedAuthenticationFactorLength: l_EA
	}, SVK_id, EA_id);
	const nonce: ImmutableBigInteger = new RandomService().genRandomInteger(TWO_POW_256);
	const TS: number = getTimestamp();
	const T: number = Math.floor(TS / 300);
	const salt_id: ImmutableUint8Array = cutToBitLength(hashService.recursiveHash(ee, credentialID_id, "dAuth", authStep, nonce), 128);
	const k: ImmutableUint8Array = ImmutableUint8Array.from([...stringToByteArray(hAuth_id).value(), ...stringToByteArray("Auth").value(), ...integerToByteArray(ImmutableBigInteger.fromNumber(T)).value()]);
	// @ts-ignore
	const bhhAuth_id: ImmutableUint8Array = await argon2Service.getArgon2id(k, salt_id);
	const hhAuth_id: string = base64Service.base64Encode(bhhAuth_id.value());

	// Output.
	return {
		derivedVoterIdentifier: credentialID_id,
		derivedAuthenticationChallenge: hhAuth_id,
		authenticationNonce: nonce
	};
}

/**
 * Gets a time stamp of the current UNIX time in seconds.
 *
 * @return {number} - the number of seconds passed since 1 January 1970
 */
function getTimestamp(): number {
	return Math.floor(Date.now() / 1000);
}
