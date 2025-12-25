/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {Hashable} from "crypto-primitives-ts/lib/esm/hashing/hashable";
import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {HashService} from "crypto-primitives-ts/lib/esm/hashing/hash_service";
import {validateUUID} from "../../../domain/validations/validations";
import {Base64Service} from "crypto-primitives-ts/lib/esm/math/base64_service";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {PrimesMappingTable} from "../../../domain/election/primes-mapping-table";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {getActualVotingOptions} from "../electoral-model/primes-mapping-table/get-actual-voting-options.algorithm";
import {getSemanticInformation} from "../electoral-model/primes-mapping-table/get-semantic-information.algorithm";
import {getEncodedVotingOptions} from "../electoral-model/primes-mapping-table/get-encoded-voting-options.algorithm";
import {getCorrectnessInformation} from "../electoral-model/primes-mapping-table/get-correctness-information.algorithm";
import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {ElGamalMultiRecipientPublicKey} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_public_key";

const hash: HashService = new HashService();
const base64: Base64Service = new Base64Service();

/**
 * Gets the hash context for the given input.
 * <p>
 * Recursively hashes the context information and then base64 encodes the result.
 * </p>
 *
 * @param encryptionGroup                      (p, q, g), the encryption group. Must be non-null.
 * @param electionEventId                      ee, the election event id. Must be non-null and a valid UUID.
 * @param verificationCardSetId                vcs, the verification card set id. Must be non-null and a valid UUID.
 * @param primesMappingTable                   pTable, the primes mapping table. Must be non-null and defined for the encryption group.
 * @param electionPublicKey                    EL<sub>pk</sub>, the election public key. Must be non-null and defined for the encryption group.
 * @param choiceReturnCodesEncryptionPublicKey pk<sub>CCR</sub>, the choice return codes encryption public key. Must be non-null.
 * @return the hash context encoded in Base64.
 * @throws NullPointerException      if any of the arguments is {@code null}.
 * @throws IllegalArgumentException  if
 *                                   <ul>
 *                                       <li>the primes mapping table's group and the encryption group are not identical</li>
 *                                       <li>the election public key's group and the encryption group are not identical</li>
 *                                       <li>the choice return codes encryption public key's group and the encryption group are not identical</li>
 *                                   </ul>
 * @throws ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException if the election event id or the verification card set
 *                                                                                          id is not a valid UUID.
 */
export function getHashContext(encryptionGroup: GqGroup, electionEventId: string, verificationCardSetId: string,
							   primesMappingTable: PrimesMappingTable, electionPublicKey: ElGamalMultiRecipientPublicKey,
							   choiceReturnCodesEncryptionPublicKey: ElGamalMultiRecipientPublicKey): string {
	checkNotNull(encryptionGroup);

	// Context.
	const p: ImmutableBigInteger = encryptionGroup.p;
	const q: ImmutableBigInteger = encryptionGroup.q;
	const g: ImmutableBigInteger = encryptionGroup.generator.value;
	const ee: string = validateUUID(electionEventId);
	const vcs: string = validateUUID(verificationCardSetId);
	const pTable: PrimesMappingTable = checkNotNull(primesMappingTable);
	const EL_pk: ElGamalMultiRecipientPublicKey = checkNotNull(electionPublicKey);
	const pk_CCR: ElGamalMultiRecipientPublicKey = checkNotNull(choiceReturnCodesEncryptionPublicKey);

	checkArgument(encryptionGroup.equals(pTable.pTable.group), "The pTable's group must be the same as the encryption group.");
	checkArgument(encryptionGroup.equals(EL_pk.group), "The election public key's group must be the same as the encryption group.");
	checkArgument(encryptionGroup.equals(pk_CCR.group),
		"The choice return codes encryption public key's group must be the same as the encryption group.");

	// Operation.
	const h: ImmutableArray<Hashable> = ImmutableArray.of(
		"EncryptionParameters", p, q, g,
		"ElectionEventContext", ee, vcs,
		"ActualVotingOptions", ...getActualVotingOptions(pTable, GroupVector.of()).elements(),
		"EncodedVotingOptions", ...getEncodedVotingOptions(pTable, ImmutableArray.of()).toHashableForm().elements(),
		"SemanticInformation", ...getSemanticInformation(pTable).elements(),
		"CorrectnessInformation", ...getCorrectnessInformation(pTable, ImmutableArray.of()).elements(),
		"ELpk", ...EL_pk.stream().map(keyElement => keyElement.value),
		"pkCCR", ...pk_CCR.stream().map(keyElement => keyElement.value)
	);

	return base64.base64Encode(hash.recursiveHash(h).value());
}
