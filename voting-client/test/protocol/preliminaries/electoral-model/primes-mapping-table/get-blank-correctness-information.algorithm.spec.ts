/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {NullPointerError} from "crypto-primitives-ts/lib/esm/error/null_pointer_error";
import {PrimitivesParams} from "../../../../../src/domain/primitives-params.types";
import {parsePrimitivesParams} from "../../../../../src/domain/primitives-params-parser";
import {
	getBlankCorrectnessInformation
} from "../../../../../src/protocol/preliminaries/electoral-model/primes-mapping-table/get-blank-correctness-information.algorithm";

import authenticateVoterResponseJson from "../../../../tools/data/authenticate-voter-response.json";

describe("GetBlankCorrectnessInformation algorithm", (): void => {

	const primitivesParams: PrimitivesParams = parsePrimitivesParams(
		authenticateVoterResponseJson.votingClientPublicKeys,
		authenticateVoterResponseJson.primesMappingTable
	);

	describe("should return", (): void => {

		test("the correct number of blank correctness information", (): void => {
			const blankCorrectnessInformationList: ImmutableArray<string> = getBlankCorrectnessInformation(primitivesParams.primesMappingTable);
			const numberOfBlankOptions = 12;

			expect(blankCorrectnessInformationList.length).toEqual(numberOfBlankOptions);
		});

	});

	describe("should throw", (): void => {

		test("when primesMappingTable is null", (): void => {
			expect(() => getBlankCorrectnessInformation(null)).toThrow(NullPointerError);
		});

	});

});
