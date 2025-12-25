/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {Hashable} from "crypto-primitives-ts/lib/esm/hashing/hashable";
import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {PrimeGqElement} from "crypto-primitives-ts/lib/esm/math/prime_gq_element";
import {GroupVectorElement} from "crypto-primitives-ts/lib/esm/group_vector_element";
import {validateActualVotingOption, validateCorrectnessInformation, validateSemanticInformation} from "../validations/validations";

/**
 * Represents an entry, say the i-th entry, of the primes mapping table - pTable.
 */
export class PrimesMappingTableEntry implements GroupVectorElement<GqGroup> {

	private readonly actualVotingOptionInternal: string;
	private readonly encodedVotingOptionInternal: PrimeGqElement;
	private readonly semanticInformationInternal: string;
	private readonly correctnessInformationInternal: string;

  constructor(actualVotingOption: string, encodedVotingOption: PrimeGqElement, semanticInformation: string, correctnessInformation: string) {
		this.actualVotingOptionInternal = validateActualVotingOption(actualVotingOption);
		this.encodedVotingOptionInternal = checkNotNull(encodedVotingOption);
		this.semanticInformationInternal = validateSemanticInformation(semanticInformation);
		this.correctnessInformationInternal = validateCorrectnessInformation(correctnessInformation);
	}

	get actualVotingOption(): string {
		return this.actualVotingOptionInternal;
	}

	get encodedVotingOption(): PrimeGqElement {
		return this.encodedVotingOptionInternal;
	}

	get semanticInformation(): string {
		return this.semanticInformationInternal;
	}

	get correctnessInformation(): string {
		return this.correctnessInformationInternal;
	}

	get group(): GqGroup {
		return this.encodedVotingOptionInternal.group;
	}

	get size(): number {
		return 1;
	}

	public toHashableForm(): ImmutableArray<Hashable> {
		return ImmutableArray.of<Hashable>(this.actualVotingOptionInternal, this.encodedVotingOptionInternal.toHashableForm());
	}

	public equals(o: PrimesMappingTableEntry): boolean {
		if (this === o) {
			return true;
		}
		if (o == null || typeof this !== typeof o) {
			return false;
		}
		return this.actualVotingOptionInternal === o.actualVotingOption
			&& this.semanticInformationInternal === o.semanticInformation
			&& this.encodedVotingOptionInternal.value.equals(o.encodedVotingOption.value)
			&& this.correctnessInformationInternal === o.correctnessInformation;
	}
}
