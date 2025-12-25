/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {NullPointerError} from "crypto-primitives-ts/lib/esm/error/null_pointer_error";
import {PrimitivesParams} from "../../../../../src/domain/primitives-params.types";
import {parsePrimitivesParams} from "../../../../../src/domain/primitives-params-parser";
import {getSemanticInformation} from "../../../../../src/protocol/preliminaries/electoral-model/primes-mapping-table/get-semantic-information.algorithm";

import authenticateVoterResponseJson from "../../../../tools/data/authenticate-voter-response.json";

describe("GetSemanticInformation algorithm", (): void => {

	const primitivesParams: PrimitivesParams = parsePrimitivesParams(
		authenticateVoterResponseJson.votingClientPublicKeys,
		authenticateVoterResponseJson.primesMappingTable
	);

	describe("should return", (): void => {

		test("all semantic information", (): void => {
			const semanticInformationList: ImmutableArray<string> = getSemanticInformation(primitivesParams.primesMappingTable);
			expect(primitivesParams.primesMappingTable.getNumberOfVotingOptions()).toEqual(semanticInformationList.length);
		});

	});

	describe("should throw", (): void => {

		test("when primesMappingTable is null", (): void => {
			expect(() => getSemanticInformation(null)).toThrow(NullPointerError);
		});

	});

});
