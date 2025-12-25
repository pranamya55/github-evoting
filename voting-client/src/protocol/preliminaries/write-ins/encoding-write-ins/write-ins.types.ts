/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";

/**
 * @property {GqGroup} encryptionGroup - (p, q, g), the encryption group.
 */
export interface WriteInToQuadraticResidueContext {
	encryptionGroup: GqGroup;
}

/**
 * @property {GqGroup} encryptionGroup - (p, q, g), the encryption group.
 */
export interface WriteInToIntegerContext {
	encryptionGroup: GqGroup;
}
