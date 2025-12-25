/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {PrimeGqElement} from "crypto-primitives-ts/lib/esm/math/prime_gq_element";
import {NullPointerError} from "crypto-primitives-ts/lib/esm/error/null_pointer_error";
import {PrimitivesParams} from "../../../../../src/domain/primitives-params.types";
import {parsePrimitivesParams} from "../../../../../src/domain/primitives-params-parser";
import {
	getWriteInEncodedVotingOptions
} from "../../../../../src/protocol/preliminaries/electoral-model/primes-mapping-table/get-write-in-encoded-voting-options.algorithm";

import authenticateVoterResponseJson from "../../../../tools/data/authenticate-voter-response.json";

describe("GetWriteInEncodedVotingOptions algorithm", (): void => {

	const primitivesParams: PrimitivesParams = parsePrimitivesParams(
		authenticateVoterResponseJson.votingClientPublicKeys,
		authenticateVoterResponseJson.primesMappingTable
	);

	describe("should return", (): void => {

		test("the correct number of write-in encoded voting options", (): void => {
			const writeInEncodedVotingOptions: GroupVector<PrimeGqElement, GqGroup> = getWriteInEncodedVotingOptions(primitivesParams.primesMappingTable);
			const numberOfWriteInOptions = 1;

			expect(writeInEncodedVotingOptions.size).toEqual(numberOfWriteInOptions);
		});

	});

	describe("should throw", (): void => {

		test("when primesMappingTable is null", (): void => {
			expect(() => getWriteInEncodedVotingOptions(null)).toThrow(NullPointerError);
		});

	});

});
