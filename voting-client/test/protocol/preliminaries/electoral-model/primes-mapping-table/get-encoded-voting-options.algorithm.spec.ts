/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {PrimeGqElement} from "crypto-primitives-ts/lib/esm/math/prime_gq_element";
import {NullPointerError} from "crypto-primitives-ts/lib/esm/error/null_pointer_error";
import {PrimitivesParams} from "../../../../../src/domain/primitives-params.types";
import {IllegalArgumentError} from "crypto-primitives-ts/lib/esm/error/illegal_argument_error";
import {parsePrimitivesParams} from "../../../../../src/domain/primitives-params-parser";
import {getEncodedVotingOptions} from "../../../../../src/protocol/preliminaries/electoral-model/primes-mapping-table/get-encoded-voting-options.algorithm";

import authenticateVoterResponseJson from "../../../../tools/data/authenticate-voter-response.json";

describe("GetEncodedVotingOptions algorithm", (): void => {

	const primitivesParams: PrimitivesParams = parsePrimitivesParams(
		authenticateVoterResponseJson.votingClientPublicKeys,
		authenticateVoterResponseJson.primesMappingTable
	);

	describe("should return", (): void => {

		test("all encoded voting options when actual vector is empty", (): void => {
			const encodedVotingOptions: GroupVector<PrimeGqElement, GqGroup> = getEncodedVotingOptions(primitivesParams.primesMappingTable, ImmutableArray.of());
			expect(primitivesParams.primesMappingTable.getNumberOfVotingOptions()).toEqual(encodedVotingOptions.size);
		});

		test("the correct number of encoded voting options", (): void => {
			const actualVotingOptions: ImmutableArray<string> = ImmutableArray.of(
				primitivesParams.primesMappingTable.pTable.elements[0].actualVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[2].actualVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[4].actualVotingOption
			);
			const encodedVotingOptions: GroupVector<PrimeGqElement, GqGroup> = getEncodedVotingOptions(primitivesParams.primesMappingTable, actualVotingOptions);

			expect(actualVotingOptions.length).toEqual(encodedVotingOptions.size);
		});

		test("the encoded voting options in the provided order", (): void => {
			const actualVotingOptions: ImmutableArray<string> = ImmutableArray.of(
				primitivesParams.primesMappingTable.pTable.elements[4].actualVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[2].actualVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[0].actualVotingOption
			);
			const encodedVotingOptions: GroupVector<PrimeGqElement, GqGroup> = getEncodedVotingOptions(primitivesParams.primesMappingTable, actualVotingOptions);

			expect(encodedVotingOptions.elements[0].value.intValue()).toEqual(primitivesParams.primesMappingTable.pTable.elements[4].encodedVotingOption.value.intValue());
			expect(encodedVotingOptions.elements[1].value.intValue()).toEqual(primitivesParams.primesMappingTable.pTable.elements[2].encodedVotingOption.value.intValue());
			expect(encodedVotingOptions.elements[2].value.intValue()).toEqual(primitivesParams.primesMappingTable.pTable.elements[0].encodedVotingOption.value.intValue());
		});

	});

	describe("should throw", (): void => {

		test("when primesMappingTable is null", (): void => {
			expect(() => getEncodedVotingOptions(null, ImmutableArray.of())).toThrow(NullPointerError);
		});

		test("when actualVotingOptions is null", (): void => {
			expect(() => getEncodedVotingOptions(primitivesParams.primesMappingTable, null)).toThrow(NullPointerError);
		});

		test("when pTable not contains all the actualVotingOptions", (): void => {
			const actualVotingOptions: ImmutableArray<string> = ImmutableArray.of(
				primitivesParams.primesMappingTable.pTable.elements[0].actualVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[1].actualVotingOption,
				"question-id|not-in-pTable"
			);

			expect(() => getEncodedVotingOptions(primitivesParams.primesMappingTable, actualVotingOptions))
				.toThrow(new IllegalArgumentError("Each actual voting option must be part of the pTable."));
		});

		test("when actualVotingOptions are not distinct", (): void => {
			const actualVotingOptions: ImmutableArray<string> = ImmutableArray.of(
				primitivesParams.primesMappingTable.pTable.elements[0].actualVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[0].actualVotingOption
			);

			expect(() => getEncodedVotingOptions(primitivesParams.primesMappingTable, actualVotingOptions))
				.toThrow(new IllegalArgumentError("All actual voting options must be distinct."));
		});

	});

});
