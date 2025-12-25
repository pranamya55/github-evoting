/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {PrimeGqElement} from "crypto-primitives-ts/lib/esm/math/prime_gq_element";
import {PrimesMappingTableEntry} from "./primes-mapping-table-entry";
import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS} from "../voting-options-constants";

/**
 * Represents the primes mapping table - pTable = ((v<sub>0</sub>, p&#771;<sub>0</sub>, &#963;<sub>0</sub>, &#964;<sub>0</sub>),...,(v<sub>n-1</sub>,
 * p&#771;<sub>n-1</sub>, &#963;<sub>n-1</sub>, &#964;<sub>n-1</sub>)) - an ordered table of {@link PrimesMappingTableEntry} elements with
 * <ul>
 *     <li>Actual voting options v&#771; = (v<sub>0</sub>,...,v<sub>n-1</sub>)</li>
 *     <li>Encoded voting options p&#771; = (p&#771;<sub>0</sub>,...,p&#771;<sub>n-1</sub>)</li>
 *     <li>Semantic information &#963; = (&#963;<sub>0</sub>,...,&#963;<sub>n-1</sub>)</li>
 *     <li>Correctness information &#964; = (&#964;<sub>0</sub>,...,&#964;<sub>n-1</sub>)</li>
 * </ul>
 * This class is immutable.
 */
export class PrimesMappingTable {

	private readonly pTableInternal: GroupVector<PrimesMappingTableEntry, GqGroup>;

	constructor(pTable: GroupVector<PrimesMappingTableEntry, GqGroup>) {
		this.pTableInternal = checkNotNull(pTable);
		checkArgument(this.pTableInternal.size !== 0, "The primes mapping table cannot be empty.");
		checkArgument(this.pTableInternal.size <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
			`The primes mapping table cannot have more elements than the maximum supported number of voting options. [n: ${this.pTableInternal.size}, n_sup: ${MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS}]`
		);
	}

	get pTable(): GroupVector<PrimesMappingTableEntry, GqGroup> {
		return this.pTableInternal;
	}

	/**
	 * Builds a primes mapping table based on the given list of primes mapping table entries.
	 *
	 * @param primesMappingTableEntries - the list of primes mapping table entries to be built from. Must be non-null, non-empty and its encoded voting
	 * options must not contain any duplicate.
	 *
	 * @throws NullPointerException     if the primes mapping table entries is null.
	 * @throws IllegalArgumentException if the encoded voting options of the primes mapping table entries contain duplicates, or if the given list of
	 * primes mapping table entries is empty or has more than {@value Constants#MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS} elements.
	 */
	public static from(primesMappingTableEntries: PrimesMappingTableEntry[]): PrimesMappingTable {
		const primesMappingTableEntriesCopy: PrimesMappingTableEntry[] = [...primesMappingTableEntries];

		const encodedVotingOptions: ReadonlySet<PrimeGqElement> = new Set<PrimeGqElement>(
			primesMappingTableEntriesCopy.map((entry) => entry.encodedVotingOption)
		);

		if (encodedVotingOptions.size !== primesMappingTableEntriesCopy.length) {
			throw new Error("The primes mapping table entries contain duplicated encoded voting options.");
		}

		return new PrimesMappingTable(GroupVector.from(primesMappingTableEntriesCopy));
	}

	/**
	 * Returns the number of elements in this primes mapping table.
	 *
	 * @return the number of elements in this primes mapping table.
	 */
	public getNumberOfVotingOptions(): number {
		return this.pTableInternal.size;
	}

	/**
	 * Returns the i-th entry, of the primes mapping table - pTable - corresponding to the given (actual | encoded) voting option.
	 *
	 * @param {PrimeGqElement | string} votingOption - (v<sub>i</sub> | p&#771;<sub>i</sub>), the (actual | encoded) voting option. Must be non-null.
	 * @return {PrimesMappingTableEntry} the i-th entry of the primes mapping table, (v<sub>i</sub>, p&#771;<sub>i</sub>, &#963;<sub>i</sub>, &#964;<sub>i</sub>).
	 * @throws NullPointerError if the (actual | encoded) voting option is null.
	 * @throws Error if no primes mapping table entry corresponds to the given (actual | encoded) voting option.
	 */
	public getPrimesMappingTableEntry(votingOption: PrimeGqElement | string): PrimesMappingTableEntry {
		checkNotNull(votingOption);

		let matchingEntry: PrimesMappingTableEntry;

		if (typeof votingOption === "string") {
			matchingEntry = this.pTableInternal.elements.find(
				(primesMappingTableEntry) => primesMappingTableEntry.actualVotingOption === votingOption
			);
		} else {
			matchingEntry = this.pTableInternal.elements.find(
				(primesMappingTableEntry) => primesMappingTableEntry.encodedVotingOption.equals(votingOption)
			);
		}

		if (matchingEntry === undefined) {
			throw new Error("No primes mapping table entry corresponds to the given voting option.");
		}

		return matchingEntry;
	}
}
