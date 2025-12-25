/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {PrimesMappingTable} from "../../../../domain/election/primes-mapping-table";
import {getBlankCorrectnessInformation} from "./get-blank-correctness-information.algorithm";

/**
 * Implements the GetPsi algorithm.
 *
 * @param {PrimesMappingTable} primesMappingTable - pTable, the primes mapping table of size n. Must be non-null.
 *
 * @returns {number} -  the number of selectable voting options.
 */
export function getPsi(primesMappingTable: PrimesMappingTable): number {

	// Context.
	const pTable: PrimesMappingTable = checkNotNull(primesMappingTable);

	// Operation
	const tau_hat_vector: ImmutableArray<string> = getBlankCorrectnessInformation(pTable);

	return tau_hat_vector.length;
}

