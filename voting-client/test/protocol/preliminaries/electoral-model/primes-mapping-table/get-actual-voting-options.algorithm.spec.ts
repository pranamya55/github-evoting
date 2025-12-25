/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {PrimeGqElement} from "crypto-primitives-ts/lib/esm/math/prime_gq_element";
import {NullPointerError} from "crypto-primitives-ts/lib/esm/error/null_pointer_error";
import {PrimitivesParams} from "../../../../../src/domain/primitives-params.types";
import {IllegalArgumentError} from "crypto-primitives-ts/lib/esm/error/illegal_argument_error";
import {parsePrimitivesParams} from "../../../../../src/domain/primitives-params-parser";
import {getActualVotingOptions} from "../../../../../src/protocol/preliminaries/electoral-model/primes-mapping-table/get-actual-voting-options.algorithm";

import authenticateVoterResponseJson from "../../../../tools/data/authenticate-voter-response.json";

describe("GetActualVotingOptions algorithm", (): void => {

	const primitivesParams: PrimitivesParams = parsePrimitivesParams(
		authenticateVoterResponseJson.votingClientPublicKeys,
		authenticateVoterResponseJson.primesMappingTable
	);

	describe("should return", (): void => {

		test("all actual voting options when encoded vector is empty", (): void => {
			const actualVotingOptions: ImmutableArray<string> = getActualVotingOptions(primitivesParams.primesMappingTable, GroupVector.from([]));
			expect(primitivesParams.primesMappingTable.getNumberOfVotingOptions()).toEqual(actualVotingOptions.length);
		});

		test("the correct number of actual voting options", (): void => {
			const encodedVotingOptions: PrimeGqElement[] = [
				primitivesParams.primesMappingTable.pTable.elements[0].encodedVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[2].encodedVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[4].encodedVotingOption
			];
			const actualVotingOptions: ImmutableArray<string> = getActualVotingOptions(primitivesParams.primesMappingTable, GroupVector.from(encodedVotingOptions));

			expect(encodedVotingOptions.length).toEqual(actualVotingOptions.length);
		});

		test("the encoded voting options in the provided order", (): void => {
			const encodedVotingOptions: PrimeGqElement[] = [
				primitivesParams.primesMappingTable.pTable.elements[4].encodedVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[2].encodedVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[0].encodedVotingOption
			];
			const actualVotingOptions: ImmutableArray<string> = getActualVotingOptions(primitivesParams.primesMappingTable, GroupVector.from(encodedVotingOptions));

			expect(actualVotingOptions.get(0)).toEqual(primitivesParams.primesMappingTable.pTable.elements[4].actualVotingOption);
			expect(actualVotingOptions.get(1)).toEqual(primitivesParams.primesMappingTable.pTable.elements[2].actualVotingOption);
			expect(actualVotingOptions.get(2)).toEqual(primitivesParams.primesMappingTable.pTable.elements[0].actualVotingOption);
		});

	});

	describe("should throw", (): void => {

		test("when primesMappingTable is null", (): void => {
			expect(() => getActualVotingOptions(null, GroupVector.from([]))).toThrow(NullPointerError);
		});

		test("when encodedVotingOptions is null", (): void => {
			expect(() => getActualVotingOptions(primitivesParams.primesMappingTable, null)).toThrow(NullPointerError);
		});

		test("when pTable not contains all the encodedVotingOptions", (): void => {
			const encodedVotingOptions: PrimeGqElement[] = [
				primitivesParams.primesMappingTable.pTable.elements[0].encodedVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[1].encodedVotingOption,
				PrimeGqElement.fromValue(853, primitivesParams.encryptionGroup)
			];

			expect(() => getActualVotingOptions(primitivesParams.primesMappingTable, GroupVector.from(encodedVotingOptions)))
				.toThrow(new IllegalArgumentError("Each encoded voting option must be part of the pTable."));
		});

		test("when encodedVotingOptions are not distinct", (): void => {
			const encodedVotingOptions: PrimeGqElement[] = [
				primitivesParams.primesMappingTable.pTable.elements[0].encodedVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[0].encodedVotingOption
			];

			expect(() => getActualVotingOptions(primitivesParams.primesMappingTable, GroupVector.from(encodedVotingOptions)))
				.toThrow(new IllegalArgumentError("All encoded voting options must be distinct."));
		});

	});

});
