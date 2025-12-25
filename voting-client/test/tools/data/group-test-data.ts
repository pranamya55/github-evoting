/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

/* jshint node:true */
"use strict";

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {RandomService} from "crypto-primitives-ts/lib/esm/math/random_service";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";

const smallTestGroups: ImmutableArray<GqGroup> = getSmallTestGroups();
const random: RandomService = new RandomService();

/**
 * Creates a random GqGroup for testing purposes.
 *
 * @function getGqGroup
 * @returns {GqGroup} random test GqGroup.
 */
export function getGqGroup(): GqGroup {
	return getRandomGqGroupFrom(smallTestGroups);
}

function getRandomGqGroupFrom(groups: ImmutableArray<GqGroup>): GqGroup {
	return groups.get(random.nextInt(groups.length));
}

function getSmallTestGroups(): ImmutableArray<GqGroup> {
	const p1: ImmutableBigInteger = ImmutableBigInteger.fromNumber(11);
	const q1: ImmutableBigInteger = ImmutableBigInteger.fromNumber(5);
	const g1: ImmutableBigInteger = ImmutableBigInteger.fromNumber(3);
	const group1: GqGroup = new GqGroup(p1, q1, g1);
	const p2: ImmutableBigInteger = ImmutableBigInteger.fromNumber(23);
	const q2: ImmutableBigInteger = ImmutableBigInteger.fromNumber(11);
	const g2: ImmutableBigInteger = ImmutableBigInteger.fromNumber(2);
	const group2: GqGroup = new GqGroup(p2, q2, g2);
	const p3: ImmutableBigInteger = ImmutableBigInteger.fromNumber(47);
	const q3: ImmutableBigInteger = ImmutableBigInteger.fromNumber(23);
	const g3: ImmutableBigInteger = ImmutableBigInteger.fromNumber(2);
	const group3: GqGroup = new GqGroup(p3, q3, g3);
	const p4: ImmutableBigInteger = ImmutableBigInteger.fromNumber(59);
	const q4: ImmutableBigInteger = ImmutableBigInteger.fromNumber(29);
	const g4: ImmutableBigInteger = ImmutableBigInteger.fromNumber(3);
	const group4: GqGroup = new GqGroup(p4, q4, g4);
	return ImmutableArray.of(group1, group2, group3, group4);
}
