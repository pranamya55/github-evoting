/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {PrimeGqElement} from "crypto-primitives-ts/lib/esm/math/prime_gq_element";
import {VotingOptionType} from "../../../../domain/election/voting-option.type";
import {PrimesMappingTable} from "../../../../domain/election/primes-mapping-table";
import {PrimesMappingTableEntry} from "../../../../domain/election/primes-mapping-table-entry";

/**
 * Implements the GetWriteInEncodedVotingOptions algorithm.
 *
 * @param {PrimesMappingTable} primesMappingTable - pTable, the primes mapping table of size n. Must be non-null.
 *
 * @returns {GroupVector<PrimeGqElement, GqGroup>} -  the list of encoded voting options that correspond to write-in voting options of size &delta;-1.
 */
export function getWriteInEncodedVotingOptions(primesMappingTable: PrimesMappingTable): GroupVector<PrimeGqElement, GqGroup> {
	checkNotNull(primesMappingTable);

	// Context.
	const pTable: GroupVector<PrimesMappingTableEntry, GqGroup> = primesMappingTable.pTable;

	// Operation.
	return GroupVector.from(pTable.elements
		.filter(entry => entry.semanticInformation.startsWith(VotingOptionType.WRITE_IN))
		.map(entry => entry.encodedVotingOption));
}

