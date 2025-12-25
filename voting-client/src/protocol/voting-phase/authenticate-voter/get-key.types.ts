/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {PrimesMappingTable} from "../../../domain/election/primes-mapping-table";
import {ElGamalMultiRecipientPublicKey} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_public_key";

/**
 * @property {GqGroup} encryptionGroup - (p, q, g), the encryption group.
 * @property {string} electionEventId - ee, the identifier of the election event.
 * @property {string} verificationCardSetId - vcs, the identifier of the verification card set.
 * @property {string} verificationCardId - vc_id, the identifier of the verification card.
 * @property {PrimesMappingTable} primesMappingTable - pTable, the primes mapping table.
 * @property {ElGamalMultiRecipientPublicKey} electionPublicKey - EL_pk, the election public key.
 * @property {ElGamalMultiRecipientPublicKey} choiceReturnCodesEncryptionPublicKey - pk_CCR, the Choice Return Codes encryption public key.
 */
export interface GetKeyContext {
	encryptionGroup: GqGroup;
	electionEventId: string;
	verificationCardSetId: string;
	verificationCardId: string;
	primesMappingTable: PrimesMappingTable;
	electionPublicKey: ElGamalMultiRecipientPublicKey;
	choiceReturnCodesEncryptionPublicKey: ElGamalMultiRecipientPublicKey;
}
