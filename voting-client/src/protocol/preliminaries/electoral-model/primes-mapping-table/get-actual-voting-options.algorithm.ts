/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {PrimeGqElement} from "crypto-primitives-ts/lib/esm/math/prime_gq_element";
import {PrimesMappingTable} from "../../../../domain/election/primes-mapping-table";
import {PrimesMappingTableEntry} from "../../../../domain/election/primes-mapping-table-entry";
import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";

/**
 * Implements the GetActualVotingOptions algorithm.
 *
 * @param {PrimesMappingTable} primesMappingTable - pTable, the primes mapping table of size n. Must be non-null. The {@link PrimesMappingTableEntry}
 * constructor validates the actual voting option.
 * @param {GroupVector<PrimeGqElement, GqGroup>} encodedVotingOptions - the list of encoded voting options. Must be non-null and a subset of the pTable's actual voting options.
 * All encoded voting options must be distinct.
 *
 * @returns {ImmutableArray<string>} - the list of actual voting options.
 */
export function getActualVotingOptions(primesMappingTable: PrimesMappingTable, encodedVotingOptions: GroupVector<PrimeGqElement, GqGroup>): ImmutableArray<string> {
	checkNotNull(primesMappingTable);
	checkNotNull(encodedVotingOptions);

	checkArgument(encodedVotingOptions.size == 0 || primesMappingTable.pTable.group.equals(encodedVotingOptions.group), "The pTable's group must be the same as the encoded voting options' group.")

	// Context.
	const pTable: GroupVector<PrimesMappingTableEntry, GqGroup> = primesMappingTable.pTable;

	// Input.
	const p_tilde_prime_vector: GroupVector<PrimeGqElement, GqGroup> = encodedVotingOptions;

	const m_prime: number = p_tilde_prime_vector.size;
	const n: number = pTable.size;

	// Require.
	checkArgument(0 <= m_prime, `The size of the encoded voting options must be greater than or equal to zero. [m_prime: ${m_prime}]`);
	checkArgument(m_prime <= n, `The size of the encoded voting options must be smaller or equal to the size of the primes mapping table. [m_prime: ${m_prime}, n: ${n}]`);
	const pTable_p_tilde_vector: PrimeGqElement[] = pTable.elements.map(entry => entry.encodedVotingOption);
	checkArgument(p_tilde_prime_vector.elements.every(p_i_tilde_prime => pTable_p_tilde_vector.some(p_i_tilde => p_i_tilde.value.equals(p_i_tilde_prime.value))),
		"Each encoded voting option must be part of the pTable.");
	checkArgument(new Set(p_tilde_prime_vector.elements).size === m_prime, "All encoded voting options must be distinct.");

	// Operation.
	if (m_prime === 0) {
		// m <- n
		return ImmutableArray.from(pTable.elements.map(entry => entry.actualVotingOption));
	} else {
		// m <- m'
		return ImmutableArray.from(p_tilde_prime_vector.elements
			.map(encodedVotingOption => primesMappingTable.getPrimesMappingTableEntry(encodedVotingOption))
			.map(primesMappingTableEntry => primesMappingTableEntry.actualVotingOption));

	}
}

