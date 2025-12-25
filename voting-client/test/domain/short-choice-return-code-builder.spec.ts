/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {parsePrimitivesParams} from "../../src/domain/primitives-params-parser";

import {PrimesMappingTable} from "../../src/domain/election/primes-mapping-table";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {getBlankCorrectnessInformation} from "../../src/protocol/preliminaries/electoral-model/primes-mapping-table/get-blank-correctness-information.algorithm";
import {RandomService} from "crypto-primitives-ts/lib/esm/math/random_service";
import {getShortChoiceReturnCodes} from "../../src/domain/short-choice-return-code-builder";
import {
	CORRECTNESS_INFORMATION_CANDIDATE_PREFIX,
	CORRECTNESS_INFORMATION_DELIMITER,
	CORRECTNESS_INFORMATION_LIST_PREFIX
} from "../../src/domain/voting-options-constants";

import authenticateVoterResponseJson from "../tools/data/authenticate-voter-response.json";

describe("Short choice return code builder", function (): void {
	let primesMappingTable: PrimesMappingTable;
	let shortChoiceReturnCodes: ImmutableArray<string>;
	let blankCorrectnessInformation: ImmutableArray<string>;

	beforeEach(() => {
		primesMappingTable = parsePrimitivesParams(
			authenticateVoterResponseJson.votingClientPublicKeys,
			authenticateVoterResponseJson.primesMappingTable
		).primesMappingTable;

		blankCorrectnessInformation = getBlankCorrectnessInformation(primesMappingTable);

		const random: RandomService = new RandomService();
		shortChoiceReturnCodes = blankCorrectnessInformation
			.map(_ => Array.from({length: 4}, () => random.nextInt(10)).join(''));
	});

	test("should get the short choice return codes in the correct order", function (): void {
		expect(getShortChoiceReturnCodes(primesMappingTable, shortChoiceReturnCodes).map(obj => obj.shortChoiceReturnCode))
			.toStrictEqual(shortChoiceReturnCodes.elements());
	});

	test("should map the short choice return codes with the correct identifications", function (): void {
		const identifications: string[] = blankCorrectnessInformation.elements().map(correctnessInformation => {
			return correctnessInformation.startsWith(CORRECTNESS_INFORMATION_LIST_PREFIX) || correctnessInformation.startsWith(CORRECTNESS_INFORMATION_CANDIDATE_PREFIX)
				? correctnessInformation.split(CORRECTNESS_INFORMATION_DELIMITER)[1]
				: correctnessInformation;
		});
		expect(getShortChoiceReturnCodes(primesMappingTable, shortChoiceReturnCodes).map(obj => {
			return 'electionIdentification' in obj ? obj.electionIdentification : obj.questionIdentification;
		})).toStrictEqual(identifications);
	});
});
