/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {PrimeGqElement} from "crypto-primitives-ts/lib/esm/math/prime_gq_element";
import {PrimitivesParams} from "./primitives-params.types";
import {PrimesMappingTable} from "./election/primes-mapping-table";
import {PrimesMappingTableEntry} from "./election/primes-mapping-table-entry";
import {ElGamalMultiRecipientPublicKey} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_public_key";
import {PrimesMappingTableRaw, VotingClientPublicKeys} from "./authenticate-voter.types";
import {deserializeElGamalMultiRecipientPublicKey, deserializeGqGroup} from "./primitives-deserializer";

/**
 * Computes and deserializes the primitives parameters from an {@link AuthenticateVoterResponsePayload}.
 *
 * @param {VotingClientPublicKeys} votingClientPublicKeys - the voting client public keys.
 * @param {PrimesMappingTableRaw} primesMappingTableRaw - the raw primes mapping table.
 *
 * @returns {PrimitivesParams}, the primitives params object.
 */
export function parsePrimitivesParams(votingClientPublicKeys: VotingClientPublicKeys, primesMappingTableRaw: PrimesMappingTableRaw): PrimitivesParams {

	// Encryption group, we assume the signature has been checked and verified
	const encryptionParameters = deserializeGqGroup(votingClientPublicKeys.encryptionParameters);

	// Election public key
	const electionPublicKey: ElGamalMultiRecipientPublicKey = deserializeElGamalMultiRecipientPublicKey(
		votingClientPublicKeys.electionPublicKey,
		encryptionParameters
	);

	// Choice return codes encryption public key
	const choiceReturnCodesEncryptionPublicKey: ElGamalMultiRecipientPublicKey = deserializeElGamalMultiRecipientPublicKey(
		votingClientPublicKeys.choiceReturnCodesEncryptionPublicKey,
		encryptionParameters
	);

	// Primes Mapping Table
	const primesMappingTableEntries: PrimesMappingTableEntry[] = primesMappingTableRaw.pTable.map(entry => new PrimesMappingTableEntry(
		entry.actualVotingOption,
		PrimeGqElement.fromValue(entry.encodedVotingOption, encryptionParameters),
		entry.semanticInformation,
		entry.correctnessInformation
	));
	const primesMappingTable: PrimesMappingTable = new PrimesMappingTable(GroupVector.from(primesMappingTableEntries));

	return {
		electionPublicKey: electionPublicKey,
		choiceReturnCodesEncryptionPublicKey: choiceReturnCodesEncryptionPublicKey,
		encryptionGroup: encryptionParameters,
		primesMappingTable: primesMappingTable
	};
}
