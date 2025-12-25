/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {VotingOptionType} from "../../../../domain/election/voting-option.type";
import {IllegalStateError} from "crypto-primitives-ts/lib/esm/error/illegal_state_error";
import {PrimesMappingTable} from "../../../../domain/election/primes-mapping-table";
import {PrimesMappingTableEntry} from "../../../../domain/election/primes-mapping-table-entry";

/**
 * Implements the GetBlankCorrectnessInformation algorithm.
 *
 * @param {PrimesMappingTable} primesMappingTable - pTable, the primes mapping table of size n. Must be non-null.
 *
 * @returns {ImmutableArray<string>} -  the list of correctness information corresponding to the blank voting options of size &psi;.
 */
export function getBlankCorrectnessInformation(primesMappingTable: PrimesMappingTable): ImmutableArray<string> {
	checkNotNull(primesMappingTable);

	// Context.
	const pTable: GroupVector<PrimesMappingTableEntry, GqGroup> = primesMappingTable.pTable;

	// Operation.
	const tau_hat_vector = pTable.elements
		.filter(entry => entry.semanticInformation.startsWith(VotingOptionType.BLANK))
		.map(entry => entry.correctnessInformation);

	const k = tau_hat_vector.length;
	if (k === 0) {
		throw new IllegalStateError("There must be at least one blank voting option.")
	}
	return ImmutableArray.from(tau_hat_vector);
}

