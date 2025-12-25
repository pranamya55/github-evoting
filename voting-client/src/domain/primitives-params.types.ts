/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {PrimesMappingTable} from "./election/primes-mapping-table";
import {ElGamalMultiRecipientPublicKey} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_public_key";

/**
 * @property {ElGamalMultiRecipientPublicKey} electionPublicKey electionPublicKey, the election public key.
 * @property {ElGamalMultiRecipientPublicKey} choiceReturnCodesEncryptionPublicKey choiceReturnCodesEncryptionPublicKey, the CCR encryption public key.
 * @property {GqGroup} encryptionGroup encryptionGroup, the encryption group.
 * @property {PrimesMappingTable} primesMappingTable primesMappingTable, the prime mapping table.
 */
export interface PrimitivesParams {
	electionPublicKey: ElGamalMultiRecipientPublicKey;
	choiceReturnCodesEncryptionPublicKey: ElGamalMultiRecipientPublicKey;
	encryptionGroup: GqGroup;
	primesMappingTable: PrimesMappingTable
}
