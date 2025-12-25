/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {NullPointerError} from "crypto-primitives-ts/lib/esm/error/null_pointer_error";
import {PrimitivesParams} from "../../../../../src/domain/primitives-params.types";
import {parsePrimitivesParams} from "../../../../../src/domain/primitives-params-parser";
import {getDelta} from "../../../../../src/protocol/preliminaries/electoral-model/primes-mapping-table/get-delta.algorithm";

import authenticateVoterResponseJson from "../../../../tools/data/authenticate-voter-response.json";

describe("GetDelta algorithm", (): void => {

	const primitivesParams: PrimitivesParams = parsePrimitivesParams(
		authenticateVoterResponseJson.votingClientPublicKeys,
		authenticateVoterResponseJson.primesMappingTable
	);

	describe("should return", (): void => {

		test("the correct number of write-in encoded voting options", (): void => {
			const numberOfWriteInOptions = 1;

			expect(getDelta(primitivesParams.primesMappingTable)).toEqual(numberOfWriteInOptions + 1);
		});

	});

	describe("should throw", (): void => {

		test("when primesMappingTable is null", (): void => {
			expect(() => getDelta(null)).toThrow(NullPointerError);
		});

	});

});
