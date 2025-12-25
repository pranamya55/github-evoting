/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GqElement} from "crypto-primitives-ts/lib/esm/math/gq_element";
import {ZqElement} from "crypto-primitives-ts/lib/esm/math/zq_element";
import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {Base64Service} from "crypto-primitives-ts/lib/esm/math/base64_service";
import {integerToByteArray} from "crypto-primitives-ts/lib/esm/conversions";
import {ExponentiationProof} from "crypto-primitives-ts/lib/esm/zeroknowledgeproofs/exponentiation_proof";
import {PlaintextEqualityProof} from "crypto-primitives-ts/lib/esm/zeroknowledgeproofs/plaintext_equality_proof";
import {ElGamalMultiRecipientMessage} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_message";
import {ElGamalMultiRecipientPublicKey} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_public_key";
import {ElGamalMultiRecipientCiphertext} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_ciphertext";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";

const base64Service: Base64Service = new Base64Service();

/**
 * Serializes a {@link GroupElement} to a standalone string.
 *
 * @param {GqGroup} group - the group to serialize. Must be not null.
 *
 * @returns {string} the serialized group.
 */
export function serializeGqGroup(group: GqGroup): string {
	checkNotNull(group);
	const object = {
		p: base64Service.base64Encode(integerToByteArray(group.p).value()),
		q: base64Service.base64Encode(integerToByteArray(group.q).value()),
		g: serializeGroupElement(group.generator)
	};
	return JSON.stringify(object);
}

/**
 * Serializes a {@link GqElement} or {@link ZqElement} to a standalone string, i.e. by itself it does not return a valid JSON. We consider the GroupElement as a
 * primitive and not an object, hence it cannot be directly serialized to a JSON with this method.
 *
 * @param {GqElement | ZqElement} element - the element to serialize. Must be not null.
 *
 * @returns {string} the serialized element.
 */
export function serializeGroupElement(element: GqElement | ZqElement): string {
	checkNotNull(element);
	return base64Service.base64Encode(integerToByteArray(element.value).value());
}

/**
 * Serializes an {@link ElGamalMultiRecipientMessage}.
 *
 * @param {ElGamalMultiRecipientMessage} message - the message to serialize.
 *
 * @returns {string} the serialized string.
 */
export function serializeElGamalMultiRecipientMessage(message: ElGamalMultiRecipientMessage): string {
	return JSON.stringify(message.stream().map(gqElement => serializeGroupElement(gqElement)));
}

/**
 * Serializes an {@link ElGamalMultiRecipientCiphertext}.
 *
 * @param {ElGamalMultiRecipientCiphertext} ciphertext - the ciphertext to serialize.
 *
 * @returns {string} the serialized string.
 */
export function serializeElGamalCiphertext(ciphertext: ElGamalMultiRecipientCiphertext): string {
	const serializedPhis = ciphertext.phis.elements
		.map(phi => serializeGroupElement(phi));

	const object = {
		gamma: serializeGroupElement(ciphertext.gamma),
		phis: serializedPhis
	};

	return JSON.stringify(object);
}

/**
 * Serializes an {@link ElGamalMultiRecipientPublicKey}.
 *
 * @param {ElGamalMultiRecipientPublicKey} publicKey - the public key to serialize.
 *
 * @returns {string} the serialized string.
 */
export function serializeElGamalMultiRecipientPublicKey(publicKey: ElGamalMultiRecipientPublicKey): string {
	return JSON.stringify(publicKey.stream().map(gqElement => serializeGroupElement(gqElement)));
}

/**
 * Serializes an {@link ExponentiationProof}.
 *
 * @param {ExponentiationProof} proof - the proof to serialize.
 *
 * @returns {string} the serialized string.
 */
export function serializeExponentiationProof(proof: ExponentiationProof): string {
	const object = {
		e: serializeGroupElement(proof.e),
		z: serializeGroupElement(proof.z)
	};

	return JSON.stringify(object);
}

/**
 * Serializes a {@link PlaintextEqualityProof}.
 *
 * @param {PlaintextEqualityProof} proof - the proof to serialize.
 *
 * @returns {string} the serialized string.
 */
export function serializePlaintextEqualityProof(proof: PlaintextEqualityProof): string {
	const serializedZ = proof.z.elements
		.map((el: ZqElement) => serializeGroupElement(el));

	const object = {
		e: serializeGroupElement(proof.e),
		z: serializedZ
	};

	return JSON.stringify(object);
}

/**
 * Serializes a {@link ImmutableBigInteger} to a standalone string, i.e. by itself it does not return a valid JSON. We consider the ImmutableBigInteger as a
 * primitive and not an object, hence it cannot be directly serialized to a JSON with this method.
 *
 * @param {ImmutableBigInteger} element - the element to serialize. Must be not null.
 *
 * @returns {string} the serialized element.
 */
export function serializeImmutableBigInteger(element: ImmutableBigInteger): string {
	checkNotNull(element);
	return base64Service.base64Encode(integerToByteArray(element).value());
}