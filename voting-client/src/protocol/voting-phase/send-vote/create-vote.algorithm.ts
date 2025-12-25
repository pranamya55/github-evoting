/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */


import {getPsi} from "../../preliminaries/electoral-model/primes-mapping-table/get-psi.algorithm";
import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {ZqGroup} from "crypto-primitives-ts/lib/esm/math/zq_group";
import {GqElement} from "crypto-primitives-ts/lib/esm/math/gq_element";
import {ZqElement} from "crypto-primitives-ts/lib/esm/math/zq_element";
import {getDelta} from "../../preliminaries/electoral-model/primes-mapping-table/get-delta.algorithm";
import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {RandomService} from "crypto-primitives-ts/lib/esm/math/random_service";
import {encodeWriteIns} from "../../preliminaries/write-ins/creating-vote-with-write-ins/encode-write-ins.algorithm";
import {getHashContext} from "../../preliminaries/agreement-algorithms/get-hash-context.algorithm";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {LATIN_ALPHABET} from "../../../domain/latin-alphabet";
import {PrimeGqElement} from "crypto-primitives-ts/lib/esm/math/prime_gq_element";
import {integerToString} from "crypto-primitives-ts/lib/esm/conversions";
import {PrimesMappingTable} from "../../../domain/election/primes-mapping-table";
import {ExponentiationProof} from "crypto-primitives-ts/lib/esm/zeroknowledgeproofs/exponentiation_proof";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {PlaintextEqualityProof} from "crypto-primitives-ts/lib/esm/zeroknowledgeproofs/plaintext_equality_proof";
import {CreateVoteContext, Vote} from "./create-vote.types";
import {getEncodedVotingOptions} from "../../preliminaries/electoral-model/primes-mapping-table/get-encoded-voting-options.algorithm";
import {getCorrectnessInformation} from "../../preliminaries/electoral-model/primes-mapping-table/get-correctness-information.algorithm";
import {ZeroKnowledgeProofService} from "crypto-primitives-ts/lib/esm/zeroknowledgeproofs/zero_knowledge_proof_service";
import {checkArgument, checkNotNull} from "crypto-primitives-ts/lib/esm/validation/preconditions";
import {ElGamalMultiRecipientMessage} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_message";
import {getBlankCorrectnessInformation} from "../../preliminaries/electoral-model/primes-mapping-table/get-blank-correctness-information.algorithm";
import {ElGamalMultiRecipientPublicKey} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_public_key";
import {MAXIMUM_WRITE_IN_OPTION_LENGTH} from "../../../domain/voting-options-constants";
import {ElGamalMultiRecipientCiphertext} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_ciphertext";
import {validateActualVotingOptions, validateUUID} from "../../../domain/validations/validations";
import {getActualVotingOptions} from "../../preliminaries/electoral-model/primes-mapping-table/get-actual-voting-options.algorithm";

const l_w: number = MAXIMUM_WRITE_IN_OPTION_LENGTH;

/**
 * Implements the CreateVote algorithm described in the cryptographic protocol.
 *
 * @param {CreateVoteContext} context - the createVote context.
 * @param {ImmutableArray<string>} selectedActualVotingOptions - v_id_hat, the vector of selected actual voting options.
 * @param {ImmutableArray<string>} selectedWriteIns - s_id_hat, the vector of selected write-ins.
 * @param {ZqElement} verificationCardSecretKey - k_id, the verification card secret key.
 * @returns {Vote} - the vote object.
 */
export function createVote(
	context: CreateVoteContext,
	selectedActualVotingOptions: ImmutableArray<string>,
	selectedWriteIns: ImmutableArray<string>,
	verificationCardSecretKey: ZqElement
): Vote {
	// Context.
	checkNotNull(context);
	const p_q_g: GqGroup = checkNotNull(context.encryptionGroup);
	const ee: string = validateUUID(context.electionEventId);
	const vcs: string = validateUUID(context.verificationCardSetId);
	const vc_id: string = validateUUID(context.verificationCardId);
	const pTable: PrimesMappingTable = checkNotNull(context.primesMappingTable);
	const EL_pk: ElGamalMultiRecipientPublicKey = checkNotNull(context.electionPublicKey);
	const pk_CCR: ElGamalMultiRecipientPublicKey = checkNotNull(context.choiceReturnCodesEncryptionPublicKey);
	const psi: number = getPsi(pTable);
	const delta: number = getDelta(pTable);

	// Input.
	const v_id_hat: ImmutableArray<string> = validateActualVotingOptions(selectedActualVotingOptions);
	const s_id_hat: ImmutableArray<string> = checkNotNull(selectedWriteIns);
	checkArgument(s_id_hat.every(s_i_hat => !s_i_hat.includes(LATIN_ALPHABET.get(0))),
		`All selected write-in options must not contain the rank 0 character. [rank 0 character: ${LATIN_ALPHABET.get(0)}]`);
	checkArgument(s_id_hat.every(s_i_hat => s_i_hat.split('').every(char => LATIN_ALPHABET.includes(char))),
		"All characters in each selected write-in option must be in A_latin alphabet.");
	const k_id: ZqElement = checkNotNull(verificationCardSecretKey);

	const randomService: RandomService = new RandomService();
	const zeroKnowledgeProofService: ZeroKnowledgeProofService = new ZeroKnowledgeProofService();
	const q: ImmutableBigInteger = p_q_g.q;

	// Cross-checks.
	const pTable_v_vector: string[] = getActualVotingOptions(pTable, GroupVector.of()).elements();
	checkArgument(v_id_hat.every(elem => pTable_v_vector.includes(elem)),
		"The primes mapping table's actual voting options must contain all the selected actual voting options.");
	checkArgument(p_q_g.equals(EL_pk.group),
		"The encryption group of the context must equal the encryption group of the election public key.");
	checkArgument(p_q_g.equals(pk_CCR.group),
		"The encryption group of the context must equal the encryption group of the choice return codes encryption public key.");
	checkArgument(p_q_g.hasSameOrderAs(k_id.group),
		"The encryption group of the context must equal the order of the verification card secret key.");
	checkArgument(v_id_hat.length === psi, "The number of selected actual voting options must be equal to psi.")

	// Require.
	checkArgument(JSON.stringify(getBlankCorrectnessInformation(pTable)) === JSON.stringify(getCorrectnessInformation(pTable, v_id_hat)),
		"A decrypted vote contains an invalid combination of voting options.");
	const k: number = s_id_hat.length;
	checkArgument(k <= delta - 1, `There must be at most delta - 1 selected write-ins. [k: ${k}, delta: ${delta}]`);
	checkArgument(s_id_hat.every(s_i_hat => s_i_hat.length < l_w), "All selected write-in options must be strictly smaller than l_w.")

	// Operation.
	const p_hat: GroupVector<PrimeGqElement, GqGroup> = getEncodedVotingOptions(pTable, v_id_hat);

	const w_id: GroupVector<GqElement, GqGroup> = encodeWriteIns({encryptionGroup: p_q_g, numberOfAllowedWriteInsPlusOne: delta}, s_id_hat);

	const identity: GqElement = p_q_g.identity;
	const reducer = (product: GqElement, currentValue: PrimeGqElement | GqElement) => product.multiply(currentValue);
	const rho: GqElement = p_hat.elements.reduce(reducer, identity);

	const r_value: ImmutableBigInteger = randomService.genRandomInteger(q);
	const r: ZqElement = ZqElement.create(r_value, ZqGroup.sameOrderAs(p_q_g));

	const E1: ElGamalMultiRecipientCiphertext = ElGamalMultiRecipientCiphertext.getCiphertext(new ElGamalMultiRecipientMessage([rho, ...w_id.elements]), r, EL_pk);

	const pCC_id_vector: GqElement[] = [];
	for (let i = 0; i < psi; i++) {
		pCC_id_vector[i] = p_hat.elements[i].exponentiate(k_id);
	}

	const pCC_id: ElGamalMultiRecipientMessage = new ElGamalMultiRecipientMessage(pCC_id_vector);

	const r_prime_value: ImmutableBigInteger = randomService.genRandomInteger(q);
	const r_prime: ZqElement = ZqElement.create(r_prime_value, ZqGroup.sameOrderAs(p_q_g));

	const E2: ElGamalMultiRecipientCiphertext = ElGamalMultiRecipientCiphertext.getCiphertext(pCC_id, r_prime, pk_CCR);

	const E1_tilde: ElGamalMultiRecipientCiphertext = ElGamalMultiRecipientCiphertext.create(E1.gamma, [E1.get(0)]).getCiphertextExponentiation(k_id);

	const E2_tilde_phis_product: GqElement = E2.phis.elements.reduce(reducer, identity);
	const E2_tilde: ElGamalMultiRecipientCiphertext = ElGamalMultiRecipientCiphertext.create(E2.gamma, [E2_tilde_phis_product]);

	const K_id: GqElement = p_q_g.generator.exponentiate(k_id);

	const i_aux: ImmutableArray<string> = ImmutableArray.of("CreateVote", vc_id, getHashContext(p_q_g, ee, vcs, pTable, EL_pk, pk_CCR),
		integerToString(E1.gamma.value), ...E1.phis.elements.map(phi_1_i => integerToString(phi_1_i.value)),
		integerToString(E2.gamma.value), ...E2.phis.elements.map(phi_2_i => integerToString(phi_2_i.value)));

	const bases: GroupVector<GqElement, GqGroup> = GroupVector.of(p_q_g.generator, E1.gamma, E1.get(0));
	const gamma_1_k_id: GqElement = E1_tilde.gamma;
	const phi_1_0_k_id: GqElement = E1_tilde.get(0);
	const exponentiations: GroupVector<GqElement, GqGroup> = GroupVector.of(K_id, gamma_1_k_id, phi_1_0_k_id);
	const pi_Exp: ExponentiationProof = zeroKnowledgeProofService.genExponentiationProof(bases, k_id, exponentiations, i_aux);

	const pk_CCR_tilde: GqElement = pk_CCR.stream().slice(0, psi).reduce(reducer, identity);

	const randomness: GroupVector<ZqElement, ZqGroup> = GroupVector.of(r.multiply(k_id), r_prime);
	const pi_EqEnc: PlaintextEqualityProof = zeroKnowledgeProofService.genPlaintextEqualityProof(E1_tilde, E2_tilde, EL_pk.get(0), pk_CCR_tilde, randomness, i_aux);

	// Output.
	return {
		encryptedVote: E1,
		encryptedPartialChoiceReturnCodes: E2,
		exponentiatedEncryptedVote: E1_tilde,
		exponentiationProof: pi_Exp,
		plaintextEqualityProof: pi_EqEnc
	};
}
