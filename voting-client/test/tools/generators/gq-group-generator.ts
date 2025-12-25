/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {Primes} from "crypto-primitives-ts/lib/esm/math/primes";
import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GqElement} from "crypto-primitives-ts/lib/esm/math/gq_element";
import {RandomService} from "crypto-primitives-ts/lib/esm/math/random_service";
import {PrimeGqElement} from "crypto-primitives-ts/lib/esm/math/prime_gq_element";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS} from "../../../src/domain/voting-options-constants";

export class GqGroupGenerator {
	private readonly random: RandomService;
	private readonly groupMembers: ImmutableBigInteger[];
	private readonly gqGroup: GqGroup;
	private readonly MAX_GROUP_SIZE: ImmutableBigInteger = ImmutableBigInteger.fromNumber(1000);

	constructor(gqGroup: GqGroup) {
		this.gqGroup = checkNotNull(gqGroup);
		this.groupMembers = this.getMembers();
		this.random = new RandomService();
	}

	genRandomMembers(count: number): ImmutableBigInteger[] {
		checkArgument(count <= this.groupMembers.length, "It would take too much time to generate as many members.");

		const randomIndexes = new Set();

		while (randomIndexes.size < count) {
			randomIndexes.add(this.random.nextInt(this.groupMembers.length));
		}

		// @ts-ignore
		return Array.from(randomIndexes).map(index => this.groupMembers[index]);
	}

	public genRandomGqElements(count: number): GqElement[] {
		return this.genRandomMembers(count).map(groupMember => GqElement.fromValue(groupMember, this.gqGroup));
	}

	public getSmallPrimeMembers(count: number): PrimeGqElement[] {
		checkArgument(count <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);

		let current: ImmutableBigInteger = ImmutableBigInteger.fromNumber(5);
		let p_vector: PrimeGqElement[] = [];
		let i: number = 0;
		while (i < count && current.compareTo(this.gqGroup.p) < 0 && current.compareTo(ImmutableBigInteger.fromNumber(2147483647)) < 0) {
			if (this.gqGroup.isGroupMember(current) && Primes.isSmallPrime(current.intValue()) && !current.equals(this.gqGroup.generator.value)) {
				p_vector = [...p_vector, PrimeGqElement.fromValue(current.intValue(), this.gqGroup)];
				i = i + 1;
			}
			current = current.add(ImmutableBigInteger.fromNumber(2));
		}
		return p_vector;
	}

	getMembers(): ImmutableBigInteger[] {
		return this.integersModP()
			.map(bigInteger => bigInteger.modPow(ImmutableBigInteger.fromNumber(2), this.gqGroup.p))
			.filter(bigInteger => !bigInteger.equals(ImmutableBigInteger.ZERO));
	}

	integersModP(): ImmutableBigInteger[] {
		const length = Math.min(this.gqGroup.p.intValue(), this.MAX_GROUP_SIZE.intValue());
		return Array.from({length}, (_, i) => ImmutableBigInteger.fromNumber(i + 1));
	}

}