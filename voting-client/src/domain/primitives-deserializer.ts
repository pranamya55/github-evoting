/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GqElement} from "crypto-primitives-ts/lib/esm/math/gq_element";
import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {Base64Service} from "crypto-primitives-ts/lib/esm/math/base64_service";
import {byteArrayToInteger} from "crypto-primitives-ts/lib/esm/conversions";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {ImmutableUint8Array} from "crypto-primitives-ts/lib/esm/immutable_uint8Array";
import {EncryptionParameters} from "./authenticate-voter.types";
import {ElGamalMultiRecipientPublicKey} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_public_key";

const base64Service: Base64Service = new Base64Service();

/**
 * Deserializes an {@link ImmutableBigInteger}.
 *
 * @param {string} element - the element to deserialize. Must be not null.
 *
 * @returns {ImmutableBigInteger} the deserialized {ImmutableBigInteger}.
 */
export function deserializeImmutableBigInteger(element: string): ImmutableBigInteger {
	checkNotNull(element);

	return byteArrayToInteger(ImmutableUint8Array.from(base64Service.base64Decode(element)));
}

/**
 * Deserializes a {@link GqGroup}.
 *
 * @param {EncryptionParameters} encryptionParameters - the encryption parameters to deserialize. Must be not null.
 *
 * @returns {GqGroup} the deserialized GqGroup.
 */
export function deserializeGqGroup(encryptionParameters: EncryptionParameters): GqGroup {
	checkNotNull(encryptionParameters);
	const p: ImmutableBigInteger = deserializeImmutableBigInteger(encryptionParameters.p);
	const q: ImmutableBigInteger = deserializeImmutableBigInteger(encryptionParameters.q);
	const g: ImmutableBigInteger = deserializeImmutableBigInteger(encryptionParameters.g);
	return new GqGroup(p, q, g);
}

/**
 * Deserializes a {@link GqElement}.
 *
 * @param {string} element - the element to deserialize. Must be not null.
 * @param {GqGroup} gqGroup - the GqGroup. Must be not null.
 *
 * @returns {GqElement} the deserialized {GqElement}.
 */
export function deserializeGqElement(element: string, gqGroup: GqGroup): GqElement {
	checkNotNull(element);
	checkNotNull(gqGroup);
	return GqElement.fromValue(deserializeImmutableBigInteger(element), gqGroup);
}

/**
 * Deserializes an {@link ElGamalMultiRecipientPublicKey}.
 *
 * @param {string[]} element - the element to deserialize. Must be not null.
 * @param {GqGroup} gqGroup - the GqGroup. Must be not null.
 *
 * @returns {ElGamalMultiRecipientPublicKey} the deserialized {ElGamalMultiRecipientPublicKey}.
 */
export function deserializeElGamalMultiRecipientPublicKey(element: string[], gqGroup: GqGroup): ElGamalMultiRecipientPublicKey {
	checkNotNull(element);
	checkNotNull(gqGroup);
	return new ElGamalMultiRecipientPublicKey(element.map(gqElement => deserializeGqElement(gqElement, gqGroup)));
}