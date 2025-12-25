/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {getKey} from "../../../../src/protocol/voting-phase/authenticate-voter/get-key.algorithm";
import {ZqElement} from "crypto-primitives-ts/lib/esm/math/zq_element";
import {PrimitivesParams} from "../../../../src/domain/primitives-params.types";
import {parsePrimitivesParams} from "../../../../src/domain/primitives-params-parser";

import authenticateVoterResponseJson from "../../../tools/data/authenticate-voter-response.json";

describe("Get key algorithm", function (): void {

	test("should return a private key", async function (): Promise<void> {
		const primitivesParams: PrimitivesParams = parsePrimitivesParams(
			authenticateVoterResponseJson.votingClientPublicKeys,
			authenticateVoterResponseJson.primesMappingTable
		);

		const expectedValue: string = "1118200155444143997759434154028589936697659512956944883104651084466257234897413407704236055443177217803507750334920643646168740438667128333782220642597097582745621730774165967399984925372207984328438596246907661261032948810482645226589899939644905426951417645582249978696068123082764313758867618185908610665798403816841928474447114307542312434814028916590917398980584762496641952830307046982342498887443735181365770156144687287036671696699559742838115985452436285208827455409098211045281315562234513298888210888461575030542230910849106393025382621974288345766943937692643996995110985569489852210459010050157430339778432806429904154480761773052795721305538992662372303079888552148174640644192744525040862720613751095704202387430045802469564828718410465993155169018599272831400641606013504944564712723625572838739337221527776741998238301415211681931181544730014960204463315267237626395649844803446469979557753445275064391062710"

		const key: ZqElement = await getKey(
			{
				encryptionGroup: primitivesParams.encryptionGroup,
				electionEventId: authenticateVoterResponseJson.voterAuthenticationData.electionEventId,
				verificationCardSetId: authenticateVoterResponseJson.voterAuthenticationData.verificationCardSetId,
				verificationCardId: authenticateVoterResponseJson.verificationCardKeystore.verificationCardId,
				primesMappingTable: primitivesParams.primesMappingTable,
				electionPublicKey: primitivesParams.electionPublicKey,
				choiceReturnCodesEncryptionPublicKey: primitivesParams.choiceReturnCodesEncryptionPublicKey
			},
			"mdb8zg7jxehhcik4nzutfdxp",
			authenticateVoterResponseJson.verificationCardKeystore.verificationCardKeystore
		);

		expect(key.value.toString()).toBe(expectedValue);
	}, 30000);

});


