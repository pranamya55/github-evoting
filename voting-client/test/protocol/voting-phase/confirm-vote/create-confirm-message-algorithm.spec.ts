/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {ZqGroup} from "crypto-primitives-ts/lib/esm/math/zq_group";
import {GqElement} from "crypto-primitives-ts/lib/esm/math/gq_element";
import {ZqElement} from "crypto-primitives-ts/lib/esm/math/zq_element";
import {PrimitivesParams} from "../../../../src/domain/primitives-params.types";
import {byteArrayToInteger} from "crypto-primitives-ts/lib/esm/conversions";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {ImmutableUint8Array} from "crypto-primitives-ts/lib/esm/immutable_uint8Array";
import {createConfirmMessage} from "../../../../src/protocol/voting-phase/confirm-vote/create-confirm-message.algorithm";
import {parsePrimitivesParams} from "../../../../src/domain/primitives-params-parser";

import authenticateVoterResponseJson from "../../../tools/data/authenticate-voter-response.json";
import verificationCardSecretKeyBytes from "../../../tools/data/verificationCardSecretKeyBytes.json";

describe("Create confirm message algorithm", function (): void {

	const primitivesParams: PrimitivesParams = parsePrimitivesParams(authenticateVoterResponseJson.votingClientPublicKeys, authenticateVoterResponseJson.primesMappingTable);
	const k_id: ImmutableBigInteger = byteArrayToInteger(ImmutableUint8Array.from(verificationCardSecretKeyBytes));
	const verificationCardSecretKey: ZqElement = ZqElement.create(k_id, ZqGroup.sameOrderAs(primitivesParams.encryptionGroup));

	test("should generate a confirmation key", function (): void {

		const confirmationKey: GqElement = createConfirmMessage(
			{
				encryptionGroup: primitivesParams.encryptionGroup
			},
			"186077054",
			verificationCardSecretKey
		);

		expect("256384865125111931203210720040355539869403966075339964176751778419790110977859930950710413019616792501567258082601136123535033699100207593997034367558995950647279063366232151871154820091102735536895107689860770174796959587521744387197892060991935393656301869513082042198043886230987848448693068214125502381043618079013871355506949416546514237374515827976154009393889752300095432180924034186918080094832246441617815631363817381029526980364779793071047766347825524412295278939692151652765709981918835391508258201249643371483788875609091697780058297029800340559436724105754640629234714679616274690199760761492228598779732290221167038021595199397235311729096989653433434675467007536004077525312840573539683693942115787882699344351433090034780026980984655818993849727212335076100351143161342982374421828240138881001689143932081145764529680769421894650783660262446459932734821971217835265521463475273366289243858454026217993552139")
			.toBe(confirmationKey.value.toString());
	});

	describe("should fail with", function (): void {

		const bck_length_error: string = "The ballot casting key length must be 9";
		const bck_numeric_error: string = "The ballot casting key must be a numeric value";

		const parameters = [
			{
				description: "a shorter ballot casting key",
				bck: "12345678",
				vcsk: verificationCardSecretKey,
				context: {
					encryptionGroup: primitivesParams.encryptionGroup
				},
				error: new Error(bck_length_error)
			},
			{
				description: "a longer ballot casting key",
				bck: "1234567890",
				vcsk: verificationCardSecretKey,
				context: {
					encryptionGroup: primitivesParams.encryptionGroup
				},
				error: new Error(bck_length_error)
			},
			{
				description: "a zero ballot casting key",
				bck: "000000000",
				vcsk: verificationCardSecretKey,
				context: {
					encryptionGroup: primitivesParams.encryptionGroup
				},
				error: new Error("The ballot casting key must contain at least one non-zero element")
			},
			{
				description: "a non-numeric ballot casting key",
				bck: "1A3456789",
				vcsk: verificationCardSecretKey,
				context: {
					encryptionGroup: primitivesParams.encryptionGroup
				},
				error: new Error(bck_numeric_error)
			},
			{
				description: "a space starting ballot casting key",
				bck: " 23456789",
				vcsk: verificationCardSecretKey,
				context: {
					encryptionGroup: primitivesParams.encryptionGroup
				},
				error: new Error(bck_numeric_error)
			},
			{
				description: "a space ending ballot casting key",
				bck: "12345678 ",
				vcsk: verificationCardSecretKey,
				context: {
					encryptionGroup: primitivesParams.encryptionGroup
				},
				error: new Error(bck_numeric_error)
			},
			{
				description: "a null ballot casting key",
				bck: null,
				vcsk: verificationCardSecretKey,
				context: {
					encryptionGroup: primitivesParams.encryptionGroup
				},
				error: new Error()
			},
			{
				description: "a null verification card secret key",
				bck: "123456789",
				vcsk: null,
				context: {
					encryptionGroup: primitivesParams.encryptionGroup
				},
				error: new Error()
			},
			{
				description: "a null encryption params",
				bck: "123456789",
				vcsk: verificationCardSecretKey,
				context: {
					encryptionGroup: null
				},
				error: new Error()
			}
		];

		parameters.forEach((parameter): void => {
			it(parameter.description, function (): void {
				expect(function () {
					createConfirmMessage(
						parameter.context,
						parameter.bck,
						parameter.vcsk
					);
				}).toThrow(parameter.error);
			});
		});
	});

});


