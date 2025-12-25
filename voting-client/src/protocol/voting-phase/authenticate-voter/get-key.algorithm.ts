/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {ZqGroup} from "crypto-primitives-ts/lib/esm/math/zq_group";
import {ZqElement} from "crypto-primitives-ts/lib/esm/math/zq_element";
import {HashService} from "crypto-primitives-ts/lib/esm/hashing/hash_service";
import {Argon2Profile} from "crypto-primitives-ts/lib/esm/hashing/argon2_profile";
import {Argon2Service} from "crypto-primitives-ts/lib/esm/hashing/argon2_service";
import {Base64Service} from "crypto-primitives-ts/lib/esm/math/base64_service";
import {GetKeyContext} from "./get-key.types";
import {getHashContext} from "../../preliminaries/agreement-algorithms/get-hash-context.algorithm";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {SymmetricService} from "crypto-primitives-ts/lib/esm/symmetric/symmetric_service";
import {PrimesMappingTable} from "../../../domain/election/primes-mapping-table";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {ImmutableUint8Array} from "crypto-primitives-ts/lib/esm/immutable_uint8Array";
import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {SymmetricEncryptionAlgorithm} from "crypto-primitives-ts/lib/esm/symmetric/symmetric_encryption_algorithm";
import {ElGamalMultiRecipientPublicKey} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_public_key";
import {byteArrayToInteger, stringToByteArray} from "crypto-primitives-ts/lib/esm/conversions";
import {validateBase64String, validateUUID} from "../../../domain/validations/validations";
import {validateSVK} from "../../../domain/validations/start-voting-key-validation";

const VERIFICATION_CARD_KEYSTORE_MAX_LENGTH: number = 600;
const VERIFICATION_CARD_KEYSTORE_MIN_LENGTH: number = 56;

/**
 * Retrieves the verification card secret key from the verification card keystore.
 *
 * @param {GetKeyContext} context - the getKey context.
 * @param {string} startVotingKey - SVK_id, the start voting key.
 * @param {string} verificationCardKeystore - VCks_id, the verification card keystore.
 * @return {ZqElement} - k_id, the verification card secret key.
 */
export async function getKey(
	context: GetKeyContext,
	startVotingKey: string,
	verificationCardKeystore: string
): Promise<ZqElement> {
	checkNotNull(context);

	// Context.
	const p_q_g: GqGroup = checkNotNull(context.encryptionGroup);
	const ee: string = validateUUID(context.electionEventId);
	const vcs: string = validateUUID(context.verificationCardSetId);
	const vc_id: string = validateUUID(context.verificationCardId);
	const pTable: PrimesMappingTable = checkNotNull(context.primesMappingTable);
	const EL_pk: ElGamalMultiRecipientPublicKey = checkNotNull(context.electionPublicKey);
	const pk_CCR: ElGamalMultiRecipientPublicKey = checkNotNull(context.choiceReturnCodesEncryptionPublicKey);

	// Input.
	const SVK_id: string = validateSVK(startVotingKey);
	const VCks_id: string = validateBase64String(verificationCardKeystore);
	checkArgument(VERIFICATION_CARD_KEYSTORE_MIN_LENGTH <= VCks_id.length && VCks_id.length <= VERIFICATION_CARD_KEYSTORE_MAX_LENGTH,
		`The verification card keystore length must be between ${VERIFICATION_CARD_KEYSTORE_MIN_LENGTH} and ${VERIFICATION_CARD_KEYSTORE_MAX_LENGTH}.`);

	const hashService: HashService = new HashService();
	const base64Service: Base64Service = new Base64Service();
	const symmetricService: SymmetricService = new SymmetricService();
	const nonceLength: number = SymmetricEncryptionAlgorithm.AES256_GCM_NOPADDING().nonceLength;
	const argon2Service: Argon2Service = new Argon2Service(Argon2Profile.LESS_MEMORY);
	const saltLength: number = 16;

	// Cross-checks.
	checkArgument(p_q_g.equals(pTable.pTable.group), "The context encryption group must equal the primes mapping table group.")
	checkArgument(p_q_g.equals(EL_pk.group), "The context encryption group must equal the election public key group.");
	checkArgument(p_q_g.equals(pk_CCR.group), "The context encryption group must equal the choice return codes encryption public key group.");

	// Operation.
	// @ts-ignore
	const i_aux: ImmutableArray<string> = ImmutableArray.of("GetKey", getHashContext(p_q_g, ee, vcs, pTable, EL_pk, pk_CCR));

	const VCks_id_combined: Uint8Array = base64Service.base64Decode(VCks_id);

	const length: number = VCks_id_combined.byteLength;
	const split_ciphertext: number = length - (nonceLength + saltLength);
	const split_nonce: number = split_ciphertext + nonceLength;
	const VCks_id_ciphertext: ImmutableUint8Array = ImmutableUint8Array.from(VCks_id_combined.subarray(0, split_ciphertext));
	const VCks_id_nonce: ImmutableUint8Array = ImmutableUint8Array.from(VCks_id_combined.subarray(split_ciphertext, split_nonce));
	const VCks_id_salt: ImmutableUint8Array = ImmutableUint8Array.from(VCks_id_combined.subarray(split_nonce, length));

	const dSVK_id: ImmutableUint8Array = await argon2Service.getArgon2id(stringToByteArray(SVK_id), VCks_id_salt);

	const KSkey_id: ImmutableUint8Array = hashService.recursiveHash("VerificationCardKeystore", ee, vcs, vc_id, dSVK_id);

	const k_id_bytes: ImmutableUint8Array = await symmetricService.getPlaintextSymmetric(KSkey_id, VCks_id_ciphertext, VCks_id_nonce, i_aux);

	const k_id: ImmutableBigInteger = byteArrayToInteger(k_id_bytes);

	// Output.
	return ZqElement.create(k_id, ZqGroup.sameOrderAs(p_q_g));
}
