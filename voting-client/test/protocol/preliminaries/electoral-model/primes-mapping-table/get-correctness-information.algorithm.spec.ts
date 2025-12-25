/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {NullPointerError} from "crypto-primitives-ts/lib/esm/error/null_pointer_error";
import {PrimitivesParams} from "../../../../../src/domain/primitives-params.types";
import {IllegalArgumentError} from "crypto-primitives-ts/lib/esm/error/illegal_argument_error";
import {parsePrimitivesParams} from "../../../../../src/domain/primitives-params-parser";
import {getCorrectnessInformation} from "../../../../../src/protocol/preliminaries/electoral-model/primes-mapping-table/get-correctness-information.algorithm";

import authenticateVoterResponseJson from "../../../../tools/data/authenticate-voter-response.json";

describe("GetCorrectnessInformation algorithm", (): void => {

	const primitivesParams: PrimitivesParams = parsePrimitivesParams(
		authenticateVoterResponseJson.votingClientPublicKeys,
		authenticateVoterResponseJson.primesMappingTable
	);

	describe("should return", (): void => {

		test("all encoded voting options when actual vector is empty", (): void => {
			const correctnessInformationList: ImmutableArray<string> = getCorrectnessInformation(primitivesParams.primesMappingTable, ImmutableArray.of());
			expect(primitivesParams.primesMappingTable.getNumberOfVotingOptions()).toEqual(correctnessInformationList.length);
		});

		test("the correct number of encoded voting options", (): void => {
			const actualVotingOptions: ImmutableArray<string> = ImmutableArray.of(
				primitivesParams.primesMappingTable.pTable.elements[0].actualVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[2].actualVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[4].actualVotingOption
			);
			const correctnessInformationList: ImmutableArray<string> = getCorrectnessInformation(primitivesParams.primesMappingTable, actualVotingOptions);

			expect(actualVotingOptions.length).toEqual(correctnessInformationList.length);
		});

		test("the encoded voting options in the provided order", (): void => {
			const actualVotingOptions: ImmutableArray<string> = ImmutableArray.of(
				primitivesParams.primesMappingTable.pTable.elements[4].actualVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[2].actualVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[0].actualVotingOption
			);
			const correctnessInformationList: ImmutableArray<string> = getCorrectnessInformation(primitivesParams.primesMappingTable, actualVotingOptions);

			expect(correctnessInformationList.get(0)).toEqual(primitivesParams.primesMappingTable.pTable.elements[4].correctnessInformation);
			expect(correctnessInformationList.get(1)).toEqual(primitivesParams.primesMappingTable.pTable.elements[2].correctnessInformation);
			expect(correctnessInformationList.get(2)).toEqual(primitivesParams.primesMappingTable.pTable.elements[0].correctnessInformation);
		});

	});

	describe("should throw", (): void => {

		test("when primesMappingTable is null", (): void => {
			expect(() => getCorrectnessInformation(null, ImmutableArray.of())).toThrow(NullPointerError);
		});

		test("when actualVotingOptions is null", (): void => {
			expect(() => getCorrectnessInformation(primitivesParams.primesMappingTable, null)).toThrow(NullPointerError);
		});

		test("when pTable not contains all the actualVotingOptions", (): void => {
			const actualVotingOptions: ImmutableArray<string> = ImmutableArray.of(
				primitivesParams.primesMappingTable.pTable.elements[0].actualVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[1].actualVotingOption,
				"question-id|not-in-pTable"
			);

			expect(() => getCorrectnessInformation(primitivesParams.primesMappingTable, actualVotingOptions))
				.toThrow(new IllegalArgumentError("Each actual voting option must be part of the pTable."));
		});

		test("when actualVotingOptions are not distinct", (): void => {
			const actualVotingOptions: ImmutableArray<string> = ImmutableArray.of(
				primitivesParams.primesMappingTable.pTable.elements[0].actualVotingOption,
				primitivesParams.primesMappingTable.pTable.elements[0].actualVotingOption
			);

			expect(() => getCorrectnessInformation(primitivesParams.primesMappingTable, actualVotingOptions))
				.toThrow(new IllegalArgumentError("All actual voting options must be distinct."));
		});

	});

});
