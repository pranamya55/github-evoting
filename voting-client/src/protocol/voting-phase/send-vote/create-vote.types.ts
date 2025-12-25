/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {PrimesMappingTable} from "../../../domain/election/primes-mapping-table";
import {ExponentiationProof} from "crypto-primitives-ts/lib/esm/zeroknowledgeproofs/exponentiation_proof";
import {PlaintextEqualityProof} from "crypto-primitives-ts/lib/esm/zeroknowledgeproofs/plaintext_equality_proof";
import {ElGamalMultiRecipientCiphertext} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_ciphertext";
import {ElGamalMultiRecipientPublicKey} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_public_key";

/**
 * @property {GqGroup} encryptionGroup - (p, q, g), the encryption group.
 * @property {string} electionEventId - ee, the election event id.
 * @property {string} verificationCardSetId - vcs, the verification card set id.
 * @property {string} verificationCardId - vcs, the verification card id.
 * @property {PrimesMappingTable} primesMappingTable - pTable, the primes mapping table.
 * @property {ElGamalMultiRecipientPublicKey} electionPublicKey - EL_pk, the election public key.
 * @property {ElGamalMultiRecipientPublicKey} choiceReturnCodesEncryptionPublicKey - pk_CCR, the Choice Return Codes encryption public key.
 */
export interface CreateVoteContext {
	encryptionGroup: GqGroup;
	electionEventId: string;
	verificationCardSetId: string;
	verificationCardId: string;
	primesMappingTable: PrimesMappingTable;
	electionPublicKey: ElGamalMultiRecipientPublicKey;
	choiceReturnCodesEncryptionPublicKey: ElGamalMultiRecipientPublicKey;
}

/**
 * @property {ElGamalMultiRecipientCiphertext} encryptedVote - E1, the encrypted vote.
 * @property {ElGamalMultiRecipientCiphertext} encryptedPartialChoiceReturnCodes - E2, the encrypted partial Choice Return Codes.
 * @property {ElGamalMultiRecipientCiphertext} exponentiatedEncryptedVote - E1_tilde, the exponentiated encrypted vote.
 * @property {ExponentiationProof} exponentiationProof - pi_Exp, the exponentiation proof.
 * @property {PlaintextEqualityProof} plaintextEqualityProof - pi_EqEnc, the plaintext equality proof.
 */
export interface Vote {
	encryptedVote: ElGamalMultiRecipientCiphertext;
	encryptedPartialChoiceReturnCodes: ElGamalMultiRecipientCiphertext;
	exponentiatedEncryptedVote: ElGamalMultiRecipientCiphertext;
	exponentiationProof: ExponentiationProof;
	plaintextEqualityProof: PlaintextEqualityProof;
}