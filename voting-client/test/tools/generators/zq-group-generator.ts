/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {ZqGroup} from "crypto-primitives-ts/lib/esm/math/zq_group";
import {ZqElement} from "crypto-primitives-ts/lib/esm/math/zq_element";
import {checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {RandomService} from "crypto-primitives-ts/lib/esm/math/random_service";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";

export class ZqGroupGenerator {
  private readonly zqGroup: ZqGroup;
	private readonly random: RandomService;

	constructor(zqGroup: ZqGroup) {
		this.zqGroup = checkNotNull(zqGroup);
		this.random = new RandomService();
	}

	public genRandomZqElementMember() {
		const value: ImmutableBigInteger = this.random.genRandomInteger(this.zqGroup.q);
		return ZqElement.create(value, this.zqGroup);
	}

}
