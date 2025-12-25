/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {PrimeGqElement} from "crypto-primitives-ts/lib/esm/math/prime_gq_element";
import {PrimesMappingTable} from "../../../../domain/election/primes-mapping-table";
import {getWriteInEncodedVotingOptions} from "./get-write-in-encoded-voting-options.algorithm";

/**
 * Implements the GetDelta algorithm.
 *
 * @param {PrimesMappingTable} primesMappingTable - pTable, the primes mapping table of size n. Must be non-null.
 *
 * @returns {number} -  the number of allowed write-ins + 1.
 */
export function getDelta(primesMappingTable: PrimesMappingTable): number {

	// Context
	const pTable: PrimesMappingTable = checkNotNull(primesMappingTable);

	// Operation
	const p_tilde_vector: GroupVector<PrimeGqElement, GqGroup> = getWriteInEncodedVotingOptions(pTable)

	return p_tilde_vector.size + 1;
}

