/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {getPsi} from "../../../../../src/protocol/preliminaries/electoral-model/primes-mapping-table/get-psi.algorithm";
import {NullPointerError} from "crypto-primitives-ts/lib/esm/error/null_pointer_error";
import {PrimitivesParams} from "../../../../../src/domain/primitives-params.types";
import {parsePrimitivesParams} from "../../../../../src/domain/primitives-params-parser";

import authenticateVoterResponseJson from "../../../../tools/data/authenticate-voter-response.json";

describe("GetPsi algorithm", (): void => {

	const primitivesParams: PrimitivesParams = parsePrimitivesParams(
		authenticateVoterResponseJson.votingClientPublicKeys,
		authenticateVoterResponseJson.primesMappingTable
	);

	describe("should return", (): void => {

		test("the correct number of blank correctness information", (): void => {
			const numberOfBlankOptions = 12;

			expect(getPsi(primitivesParams.primesMappingTable)).toEqual(numberOfBlankOptions);
		});

	});

	describe("should throw", (): void => {

		test("when primesMappingTable is null", (): void => {
			expect(() => getPsi(null)).toThrow(NullPointerError);
		});

	});

});