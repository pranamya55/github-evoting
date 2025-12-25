/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {ElGamalMultiRecipientPublicKey} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_public_key";
import {serializeElGamalMultiRecipientPublicKey} from "../../src/domain/primitives-serializer";
import {
	deserializeElGamalMultiRecipientPublicKey,
	deserializeGqGroup,
	deserializeImmutableBigInteger
} from "../../src/domain/primitives-deserializer";

import testData from "./mocks/primitives.json";

describe("Primitives deserializer", function (): void {

	const gqGroup: GqGroup = deserializeGqGroup(testData.gqGroup);

	test("should deserialize ImmutableBigInteger", function (): void {
		expect(ImmutableBigInteger.fromNumber(testData.gqGroup.expectedG)).toEqual(deserializeImmutableBigInteger(testData.gqGroup.g));
	});

	test("should deserialize ElGamalMultiRecipientPublicKey", function (): void {
		const pk: ElGamalMultiRecipientPublicKey = deserializeElGamalMultiRecipientPublicKey(testData.elGamalMultiRecipientPublicKey, gqGroup);
		expect(JSON.stringify(testData.elGamalMultiRecipientPublicKey)).toEqual(serializeElGamalMultiRecipientPublicKey(pk));
	});

});
