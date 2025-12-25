/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {PrimesMappingTable} from "../../../../domain/election/primes-mapping-table";
import {PrimesMappingTableEntry} from "../../../../domain/election/primes-mapping-table-entry";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";

/**
 * Implements the GetSemanticInformation algorithm.
 *
 * @param {PrimesMappingTable} primesMappingTable - pTable, the primes mapping table of size n. Must be non-null.
 *
 * @returns {ImmutableArray<string>} - the list of semantic information of size n.
 */
export function getSemanticInformation(primesMappingTable: PrimesMappingTable): ImmutableArray<string> {
	checkNotNull(primesMappingTable);

	// Context.
	const pTable: GroupVector<PrimesMappingTableEntry, GqGroup> = primesMappingTable.pTable;

	// Operation.
	return ImmutableArray.from(pTable.elements.map(entry => entry.semanticInformation));
}

