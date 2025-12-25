/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {Vote} from "../../../../src/protocol/voting-phase/send-vote/create-vote.types";
import {getPsi} from "../../../../src/protocol/preliminaries/electoral-model/primes-mapping-table/get-psi.algorithm";
import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {ZqGroup} from "crypto-primitives-ts/lib/esm/math/zq_group";
import {GqElement} from "crypto-primitives-ts/lib/esm/math/gq_element";
import {ZqElement} from "crypto-primitives-ts/lib/esm/math/zq_element";
import {createVote} from "../../../../src/protocol/voting-phase/send-vote/create-vote.algorithm";
import {getGqGroup} from "../../../tools/data/group-test-data";
import {LATIN_ALPHABET} from "../../../../src/domain/latin-alphabet";
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";
import {GqGroupGenerator} from "../../../tools/generators/gq-group-generator";
import {PrimitivesParams} from "../../../../src/domain/primitives-params.types";
import {ZqGroupGenerator} from "../../../tools/generators/zq-group-generator";
import {PrimesMappingTable} from "../../../../src/domain/election/primes-mapping-table";
import {parsePrimitivesParams} from "../../../../src/domain/primitives-params-parser";
import {ElGamalMultiRecipientPublicKey} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_public_key";

import authenticateVoterResponseJson from "../../../tools/data/authenticate-voter-response.json";

describe("Create vote algorithm", function (): void {
	"use strict";

	const primitivesParams: PrimitivesParams = parsePrimitivesParams(
		authenticateVoterResponseJson.votingClientPublicKeys,
		authenticateVoterResponseJson.primesMappingTable
	);

	const primesMappingTable: PrimesMappingTable = primitivesParams.primesMappingTable;
	const psi: number = getPsi(primesMappingTable);

	const gqGroupGenerator: GqGroupGenerator = new GqGroupGenerator(primitivesParams.encryptionGroup);

	const gqElementsEl: GqElement[] = gqGroupGenerator.genRandomGqElements(2);
	const electionPublicKey: ElGamalMultiRecipientPublicKey = new ElGamalMultiRecipientPublicKey(gqElementsEl);

	const gqElementsCcr: GqElement[] = gqGroupGenerator.genRandomGqElements(psi);
	const choiceReturnCodesEncryptionPublicKey: ElGamalMultiRecipientPublicKey = new ElGamalMultiRecipientPublicKey(gqElementsCcr);

	const zqGroup: ZqGroup = ZqGroup.sameOrderAs(primitivesParams.encryptionGroup);
	const zqGroupGenerator: ZqGroupGenerator = new ZqGroupGenerator(zqGroup);
	const verificationCardSecretKey: ZqElement = zqGroupGenerator.genRandomZqElementMember();

	const actualVotingOptions: string [] = [
		"806f52e6-9d49-4906-b2a8-7c89dfdf53e2|3aa38c9e-6e93-3159-91e1-c3da90681572", // 11
		"16f99be5-0ce6-4a81-ae8b-1817ceecaebc|e9da718b-469e-35d4-8d7d-d6329621e755", // 61
		"b173aebb-7b2c-476a-97fa-c81d640e498a|d0f886b7-03a8-3d8a-9e7d-4c9bfa02ddc6", // 71
		"1129f74b-1e49-4636-9731-84dba618bec6|e6467637-1921-3c4f-b669-88ba2f73a193", // 101
		"nrw_test|97e7687a-9ce7-4724-91bb-17396344b50c", // 163
		"nrw_test|cdd0f070-a3a2-4a89-b525-fa19825d1305|0", // 499
		"nrw_test|cdd0f070-a3a2-4a89-b525-fa19825d1305|1", // 509
		"nrw_test|1eb4e5e4-d2fc-4cdf-bbc4-2f84439b9870|0", // 523
		"nrw_test|0e08a1c6-62d3-3e54-8642-6792536ba039", // 743
		"nrw_test|899e6604-8374-370d-961c-e1537d2f7d53", // 757
		"nrw_test|1a6ea144-5fe6-376a-a659-aa8d72bb2ad8", // 761
		"majorz_test|f71ffce9-78c9-3621-895b-aebcc0849ad1" // 839
	];

	function getSelectedVotingOptions(actualVotingOptions: string[], length: number): ImmutableArray<string> {

		return ImmutableArray.from(actualVotingOptions.slice(0, length));

	}

	function runCreateVote(testParameters: TestParameters = {}): Vote {
		return createVote(
			{
				encryptionGroup: primitivesParams.encryptionGroup,
				electionEventId: testParameters.electionEventId || authenticateVoterResponseJson.voterAuthenticationData.electionEventId,
				verificationCardSetId: testParameters.verificationCardSetId || authenticateVoterResponseJson.voterAuthenticationData.verificationCardSetId,
				verificationCardId: testParameters.verificationCardId || authenticateVoterResponseJson.verificationCardKeystore.verificationCardId,
				primesMappingTable: primesMappingTable,
				electionPublicKey: testParameters.electionPublicKey || electionPublicKey,
				choiceReturnCodesEncryptionPublicKey: testParameters.choiceReturnCodesEncryptionPublicKey || choiceReturnCodesEncryptionPublicKey,

			},
			testParameters.selectedVotingOptions || getSelectedVotingOptions(actualVotingOptions, psi),
			testParameters.selectedWriteIns || ImmutableArray.of("Eins Hans"),
			testParameters.verificationCardSecretKey || verificationCardSecretKey
		);
	}

	test("must create a vote with correct information", function (): void {
		expect(() => runCreateVote()).not.toThrow();
	});


	describe("should fail with", function (): void {
		const otherGqGroup: GqGroup = getGqGroup();
		const otherGqGroupGenerator: GqGroupGenerator = new GqGroupGenerator(otherGqGroup);

		const context_group_EL_pk_group_error: string = "The encryption group of the context must equal the encryption group of the election public key.";
		const context_group_pk_CCR_group_error: string =
			"The encryption group of the context must equal the encryption group of the choice return codes encryption public key.";
		const EL_pk_k_id_group_error: string = "The encryption group of the context must equal the order of the verification card secret key.";

		test("a different group between context and election public key", function () {
			const differentGroupGqElements: GqElement[] = otherGqGroupGenerator.genRandomGqElements(2);
			const differentGroupKey: ElGamalMultiRecipientPublicKey = new ElGamalMultiRecipientPublicKey(differentGroupGqElements);

			expect(() => runCreateVote({electionPublicKey: differentGroupKey}))
				.toThrow(new Error(context_group_EL_pk_group_error));
		});

		test("a different group between context and choice return codes encryption public key", function () {
			const differentGroupGqElements: GqElement[] = otherGqGroupGenerator.genRandomGqElements(2);
			const differentGroupKey: ElGamalMultiRecipientPublicKey = new ElGamalMultiRecipientPublicKey(differentGroupGqElements);

			expect(() => runCreateVote({choiceReturnCodesEncryptionPublicKey: differentGroupKey}))
				.toThrow(new Error(context_group_pk_CCR_group_error));
		});

		test("a verification card secret key from a different group", function () {
			const otherZqGroup: ZqGroup = ZqGroup.sameOrderAs(otherGqGroup);
			const otherZqGroupGenerator: ZqGroupGenerator = new ZqGroupGenerator(otherZqGroup);
			const differentGroupZqElement: ZqElement = otherZqGroupGenerator.genRandomZqElementMember();

			expect(() => runCreateVote({verificationCardSecretKey: differentGroupZqElement}))
				.toThrow(new Error(EL_pk_k_id_group_error));
		});

		test("no selections", function () {
			expect(() => runCreateVote({selectedVotingOptions: getSelectedVotingOptions(actualVotingOptions, 0)}))
				.toThrow(new Error("The number of selected actual voting options must be equal to psi."));
		});

		test("duplicate selections", function () {
			const selectionWithDuplicates: string[] = [actualVotingOptions[0], ...actualVotingOptions];

			expect(() => runCreateVote({selectedVotingOptions: getSelectedVotingOptions(selectionWithDuplicates, psi)}))
				.toThrow(new Error("All actual voting options must be distinct."));
		});

		test("more write-ins than election public key elements", function () {
			expect(() => runCreateVote({selectedWriteIns: ImmutableArray.of("Eins Hans", "Zwei Jörg")})).toThrow();
		});

		test("All selected write-in options must not contain the rank 0 character.", function () {
			const s_id_hat_with_rank_zero_char = ImmutableArray.of(LATIN_ALPHABET.get(0), LATIN_ALPHABET.get(1), LATIN_ALPHABET.get(0), LATIN_ALPHABET.get(2), LATIN_ALPHABET.get(3), LATIN_ALPHABET.get(4), LATIN_ALPHABET.get(5));
			expect(() => runCreateVote({selectedWriteIns: s_id_hat_with_rank_zero_char}))
				.toThrow(new Error(`All selected write-in options must not contain the rank 0 character. [rank 0 character: ${LATIN_ALPHABET.get(0)}]`));
		});

		test("selected write-in options contain characters not in A_latin alphabet", function () {
			const hebrew_char: string = 'ה';
			const greek_char: string = 'Δ';
			const s_id_hat_with_hebrew_char = ImmutableArray.of(LATIN_ALPHABET.get(1), LATIN_ALPHABET.get(2), LATIN_ALPHABET.get(3), LATIN_ALPHABET.get(4), LATIN_ALPHABET.get(5), hebrew_char);
			const s_id_hat_with_greek_char = ImmutableArray.of(LATIN_ALPHABET.get(1), LATIN_ALPHABET.get(2), LATIN_ALPHABET.get(3), LATIN_ALPHABET.get(4), LATIN_ALPHABET.get(5), greek_char);

			// Test with ה (Hebrew letter), which is not a latin alphabet character.
			expect(() => runCreateVote({selectedWriteIns: s_id_hat_with_hebrew_char}))
				.toThrow(new Error("All characters in each selected write-in option must be in A_latin alphabet."));

			// Test with Δ (greek letter), which is not a latin alphabet character.
			expect(() => runCreateVote({selectedWriteIns: s_id_hat_with_greek_char}))
				.toThrow(new Error("All characters in each selected write-in option must be in A_latin alphabet."));
		});

	});
});

interface TestParameters {
	electionEventId?: string;
	verificationCardSetId?: string;
	verificationCardId?: string;
	selectedVotingOptions?: ImmutableArray<string>;
	selectedWriteIns?: ImmutableArray<string>;
	electionPublicKey?: ElGamalMultiRecipientPublicKey;
	choiceReturnCodesEncryptionPublicKey?: ElGamalMultiRecipientPublicKey;
	verificationCardSecretKey?: ZqElement;
}
