/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {PrimeGqElement} from "crypto-primitives-ts/lib/esm/math/prime_gq_element";
import {PrimesMappingTable} from "../../../../domain/election/primes-mapping-table";
import {PrimesMappingTableEntry} from "../../../../domain/election/primes-mapping-table-entry";
import {validateActualVotingOption} from "../../../../domain/validations/validations";
import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";

/**
 * Implements the GetEncodedVotingOptions algorithm.
 *
 * @param {PrimesMappingTable} primesMappingTable - pTable, the primes mapping table of size n. Must be non-null. The {@link PrimesMappingTableEntry}
 * constructor validates the actual voting option.
 * @param {ImmutableArray<string>} actualVotingOptions - the list of actual voting options. Must be non-null and a subset of the pTable's actual voting options.
 * All actual voting options must be distinct and valid xs:token.
 *
 * @returns {GroupVector<PrimeGqElement, GqGroup>} - the list of encoded voting options.
 */
export function getEncodedVotingOptions(primesMappingTable: PrimesMappingTable, actualVotingOptions: ImmutableArray<string>): GroupVector<PrimeGqElement, GqGroup> {
	checkNotNull(primesMappingTable);
	checkNotNull(actualVotingOptions);

	// Context.
	const pTable: GroupVector<PrimesMappingTableEntry, GqGroup> = primesMappingTable.pTable;

	// Input.
	const v_prime_vector: ImmutableArray<string> = actualVotingOptions;
	v_prime_vector.forEach(v => validateActualVotingOption(v));

	const m_prime: number = v_prime_vector.length;
	const n: number = pTable.size;

	// Require.
	checkArgument(0 <= m_prime, `The size of the actual voting options must be greater than or equal to zero. [m_prime: ${m_prime}]`);
	checkArgument(m_prime <= n, `The size of the actual voting options must be smaller or equal to the size of the primes mapping table. [m_prime: ${m_prime}, n: ${n}]`);
	const pTable_v_vector: string[] = pTable.elements.map(entry => entry.actualVotingOption);
	checkArgument(v_prime_vector.every(elem => pTable_v_vector.includes(elem)), "Each actual voting option must be part of the pTable.");
	checkArgument(new Set(v_prime_vector).size === m_prime, "All actual voting options must be distinct.");

	// Operation.
	if (m_prime === 0) {
		// m <- n
		return GroupVector.from(pTable.elements.map(entry => entry.encodedVotingOption));
	} else {
		// m <- m'
		return GroupVector.from(v_prime_vector
			.map(actualVotingOption => primesMappingTable.getPrimesMappingTableEntry(actualVotingOption))
			.map(pTableEntry => pTableEntry.encodedVotingOption).elements());
	}
}

