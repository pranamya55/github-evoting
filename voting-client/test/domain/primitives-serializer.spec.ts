/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {ZqGroup} from "crypto-primitives-ts/lib/esm/math/zq_group";
import {GqElement} from "crypto-primitives-ts/lib/esm/math/gq_element";
import {ZqElement} from "crypto-primitives-ts/lib/esm/math/zq_element";
import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {ExponentiationProof} from "crypto-primitives-ts/lib/esm/zeroknowledgeproofs/exponentiation_proof";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {PlaintextEqualityProof} from "crypto-primitives-ts/lib/esm/zeroknowledgeproofs/plaintext_equality_proof";
import {ElGamalMultiRecipientCiphertext} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_ciphertext";
import {serializeElGamalCiphertext, serializeExponentiationProof, serializePlaintextEqualityProof} from "../../src/domain/primitives-serializer";

import testData from "./mocks/primitives.json";

describe("Primitives serializer", function (): void {

	const g: ImmutableBigInteger = ImmutableBigInteger.fromNumber(testData.g);
	const p: ImmutableBigInteger = ImmutableBigInteger.fromString(testData.p);
	const q: ImmutableBigInteger = ImmutableBigInteger.fromString(testData.q);
	const bigGqGroup: GqGroup = new GqGroup(p, q, g);

	test("should serialize ElGamalCiphertext", function (): void {
		const smallP: ImmutableBigInteger = ImmutableBigInteger.fromNumber(testData.smallP);
		const smallQ: ImmutableBigInteger = ImmutableBigInteger.fromNumber(testData.smallQ);
		const smallGqGroup: GqGroup = new GqGroup(smallP, smallQ, g);

		const gFour: GqElement = GqElement.fromValue(ImmutableBigInteger.fromNumber(testData.elGamalCiphertext.phis[0]), smallGqGroup);
		const gEight: GqElement = GqElement.fromValue(ImmutableBigInteger.fromNumber(testData.elGamalCiphertext.phis[1]), smallGqGroup);
		const gThirteen: GqElement = GqElement.fromValue(ImmutableBigInteger.fromNumber(testData.elGamalCiphertext.gamma), smallGqGroup);
		const ciphertext: ElGamalMultiRecipientCiphertext = ElGamalMultiRecipientCiphertext.create(gThirteen, [gFour, gEight]);

		const expectedCiphertext: string = JSON.stringify(testData.elGamalCiphertext.expected);

		expect(expectedCiphertext).toEqual(serializeElGamalCiphertext(ciphertext));
	});

	test("should serialize ExponentiationProof", function (): void {
		const zqGroup: ZqGroup = ZqGroup.sameOrderAs(bigGqGroup);
		const eBigInteger: ImmutableBigInteger = ImmutableBigInteger.fromString(testData.exponentiationProof.e);
		const e: ZqElement = ZqElement.create(eBigInteger, zqGroup);
		const zBigInteger: ImmutableBigInteger = ImmutableBigInteger.fromString(testData.exponentiationProof.z);
		const z: ZqElement = ZqElement.create(zBigInteger, zqGroup);
		const proof: ExponentiationProof = new ExponentiationProof(e, z);

		const expectedProof: string = JSON.stringify(testData.exponentiationProof.expected);

		expect(expectedProof).toEqual(serializeExponentiationProof(proof));
	});

	test("should serialize PlaintextEqualityProof", function (): void {
		const zqGroup: ZqGroup = ZqGroup.sameOrderAs(bigGqGroup);
		const eBigInteger: ImmutableBigInteger = ImmutableBigInteger.fromString(testData.plaintextEqualityProof.e);
		const e: ZqElement = ZqElement.create(eBigInteger, zqGroup);
		const z1BigInteger: ImmutableBigInteger = ImmutableBigInteger.fromString(testData.plaintextEqualityProof.z[0]);
		const z1: ZqElement = ZqElement.create(z1BigInteger, zqGroup);
		const z2BigInteger: ImmutableBigInteger = ImmutableBigInteger.fromString(testData.plaintextEqualityProof.z[1]);
		const z2: ZqElement = ZqElement.create(z2BigInteger, zqGroup);
		const z: GroupVector<ZqElement, ZqGroup> = GroupVector.from([z1, z2]);
		const proof: PlaintextEqualityProof = new PlaintextEqualityProof(e, z);

		const expectedProof: string = JSON.stringify(testData.plaintextEqualityProof.expected);

		expect(expectedProof).toEqual(serializePlaintextEqualityProof(proof));
	});
});


