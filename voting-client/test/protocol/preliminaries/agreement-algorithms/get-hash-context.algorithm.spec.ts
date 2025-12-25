/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {GqGroup} from "crypto-primitives-ts/lib/esm/math/gq_group";
import {GqElement} from "crypto-primitives-ts/lib/esm/math/gq_element";
import {GroupVector} from "crypto-primitives-ts/lib/esm/group_vector";
import {Base16Service} from "crypto-primitives-ts/lib/esm/math/base16_service";
import {Base64Service} from "crypto-primitives-ts/lib/esm/math/base64_service";
import {RandomService} from "crypto-primitives-ts/lib/esm/math/random_service";
import {getHashContext} from "../../../../src/protocol/preliminaries/agreement-algorithms/get-hash-context.algorithm";
import {PrimeGqElement} from "crypto-primitives-ts/lib/esm/math/prime_gq_element";
import {GqGroupGenerator} from "../../../tools/generators/gq-group-generator";
import {NullPointerError} from "crypto-primitives-ts/lib/esm/error/null_pointer_error";
import {PrimitivesParams} from "../../../../src/domain/primitives-params.types";
import {PrimesMappingTable} from "../../../../src/domain/election/primes-mapping-table";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {IllegalArgumentError} from "crypto-primitives-ts/lib/esm/error/illegal_argument_error";
import {FailedValidationError} from "../../../../src/domain/validations/failed-validation-error";
import {parsePrimitivesParams} from "../../../../src/domain/primitives-params-parser";
import {PrimesMappingTableEntry} from "../../../../src/domain/election/primes-mapping-table-entry";
import {ElGamalMultiRecipientPublicKey} from "crypto-primitives-ts/lib/esm/elgamal/elgamal_multi_recipient_public_key";

import authenticateVoterResponseJson from "../../../tools/data/authenticate-voter-response.json";
import realValuesJson from './get-hash-context.json';
import {ImmutableArray} from "crypto-primitives-ts/lib/esm/immutable_array";

describe("getHashContext", () => {

	const random: RandomService = new RandomService();
	const base16: Base16Service = new Base16Service();
	const base64: Base64Service = new Base64Service();
	const primitivesParams: PrimitivesParams = parsePrimitivesParams(
		authenticateVoterResponseJson.votingClientPublicKeys,
		authenticateVoterResponseJson.primesMappingTable
	);
	const electionEventId: string = genUUID();
	const verificationCardSetId: string = genUUID();
	const encryptionGroup: GqGroup = primitivesParams.encryptionGroup;
	const primesMappingTable: PrimesMappingTable = primitivesParams.primesMappingTable;
	const electionPublicKey: ElGamalMultiRecipientPublicKey = primitivesParams.electionPublicKey;
	const choiceReturnCodesEncryptionPublicKey: ElGamalMultiRecipientPublicKey = primitivesParams.choiceReturnCodesEncryptionPublicKey;

	test("with null arguments should throw a NullPointerError", () => {
		expect(() => getHashContext(null, electionEventId, verificationCardSetId, primesMappingTable, electionPublicKey, choiceReturnCodesEncryptionPublicKey)).toThrow(NullPointerError);
		expect(() => getHashContext(encryptionGroup, null, verificationCardSetId, primesMappingTable, electionPublicKey, choiceReturnCodesEncryptionPublicKey)).toThrow(NullPointerError);
		expect(() => getHashContext(encryptionGroup, electionEventId, null, primesMappingTable, electionPublicKey, choiceReturnCodesEncryptionPublicKey)).toThrow(NullPointerError);
		expect(() => getHashContext(encryptionGroup, electionEventId, verificationCardSetId, null, electionPublicKey, choiceReturnCodesEncryptionPublicKey)).toThrow(NullPointerError);
		expect(() => getHashContext(encryptionGroup, electionEventId, verificationCardSetId, primesMappingTable, null, choiceReturnCodesEncryptionPublicKey)).toThrow(NullPointerError);
		expect(() => getHashContext(encryptionGroup, electionEventId, verificationCardSetId, primesMappingTable, electionPublicKey, null)).toThrow(NullPointerError);
	});

	test("with non UUIDs should throw a FailedValidationError", () => {
		expect(() => getHashContext(encryptionGroup, "nonUUID", verificationCardSetId, primesMappingTable, electionPublicKey, choiceReturnCodesEncryptionPublicKey)).toThrow(FailedValidationError);
		expect(() => getHashContext(encryptionGroup, electionEventId, "nonUUID", primesMappingTable, electionPublicKey, choiceReturnCodesEncryptionPublicKey)).toThrow(FailedValidationError);
	});

	test("with the primes mapping table group different from the encryption group should throw an IllegalArgumentError", () => {
		const differentGroup: GqGroup = new GqGroup(ImmutableBigInteger.fromNumber(59), ImmutableBigInteger.fromNumber(29), ImmutableBigInteger.fromNumber(3));
		const actualVotingOption: string = "question-id|answer-id";
		const encodedVotingOption: PrimeGqElement = PrimeGqElement.fromValue(7, differentGroup);
		const pTableEntry: PrimesMappingTableEntry = new PrimesMappingTableEntry(actualVotingOption, encodedVotingOption, "BLANK|semantic Information", "correctness-information");
		const differentPrimesMappingTable: PrimesMappingTable = new PrimesMappingTable(GroupVector.of(pTableEntry));

		expect(() => getHashContext(encryptionGroup, electionEventId, verificationCardSetId, differentPrimesMappingTable, electionPublicKey, choiceReturnCodesEncryptionPublicKey))
			.toThrow(new IllegalArgumentError("The pTable's group must be the same as the encryption group."));
	});

	test("with the election public key's group different from the encryption group should throw an IllegalArgumentError", () => {
		const differentGroup: GqGroup = new GqGroup(ImmutableBigInteger.fromNumber(59), ImmutableBigInteger.fromNumber(29), ImmutableBigInteger.fromNumber(3));
		const differentGroupGenerator: GqGroupGenerator = new GqGroupGenerator(differentGroup);
		const differentElectionPublicKey: ElGamalMultiRecipientPublicKey = new ElGamalMultiRecipientPublicKey(differentGroupGenerator.genRandomGqElements(5));

		expect(() => getHashContext(encryptionGroup, electionEventId, verificationCardSetId, primesMappingTable, differentElectionPublicKey, choiceReturnCodesEncryptionPublicKey))
			.toThrow(new IllegalArgumentError("The election public key's group must be the same as the encryption group."));
	});

	test("with the choice return codes public key's group different from the encryption group should throw an IllegalArgumentError", () => {
		const differentGroup: GqGroup = new GqGroup(ImmutableBigInteger.fromNumber(59), ImmutableBigInteger.fromNumber(29), ImmutableBigInteger.fromNumber(3));
		const differentGroupGenerator: GqGroupGenerator = new GqGroupGenerator(differentGroup);
		const differentChoiceReturnCodesPublicKey: ElGamalMultiRecipientPublicKey = new ElGamalMultiRecipientPublicKey(differentGroupGenerator.genRandomGqElements(5));

		expect(() => getHashContext(encryptionGroup, electionEventId, verificationCardSetId, primesMappingTable, electionPublicKey, differentChoiceReturnCodesPublicKey))
			.toThrow(new IllegalArgumentError("The choice return codes encryption public key's group must be the same as the encryption group."));
	});

	test("with valid arguments should return a Base64 encoded string", () => {
		expect(() => getHashContext(encryptionGroup, electionEventId, verificationCardSetId, primesMappingTable, electionPublicKey, choiceReturnCodesEncryptionPublicKey)).not.toThrow();

		let hashContext = getHashContext(encryptionGroup, electionEventId, verificationCardSetId, primesMappingTable, electionPublicKey, choiceReturnCodesEncryptionPublicKey);
		expect(hashContext.length).toBe(44);
		expect(() => base64.base64Decode(hashContext)).not.toThrow();
	});

	describe("with specific values should give expected output", () => {

		interface JsonFileArgument {
			encryptionGroup: GqGroup;
			electionEventId: string;
			verificationCardSetId: string;
			primesMappingTable: PrimesMappingTable;
			electionPublicKey: ElGamalMultiRecipientPublicKey;
			choiceReturnCodesEncryptionPublicKey: ElGamalMultiRecipientPublicKey;
			hash: string;
			description: string;
		}

		function jsonFileArgumentProvider(): JsonFileArgument[] {
			const parametersList: any[] = JSON.parse(JSON.stringify(realValuesJson));

			const args: JsonFileArgument[] = [];
			parametersList.forEach(testParameters => {
				const p: ImmutableBigInteger = readValue(testParameters.context.p);
				const q: ImmutableBigInteger = readValue(testParameters.context.q);
				const g: ImmutableBigInteger = readValue(testParameters.context.g);

				const gqGroup: GqGroup = new GqGroup(p, q, g);

				const electionEventId: string = testParameters.context.ee;

				const verificationCardSetId: string = testParameters.context.vcs;

				const primesMappingTable: PrimesMappingTable = readPrimesMappingTable(testParameters.context.pTable, gqGroup);

				const electionPublicKey: ElGamalMultiRecipientPublicKey = readPublicKey(testParameters.context.ELpk, gqGroup);

				const choiceReturnCodeEncryptionPublicKey: ElGamalMultiRecipientPublicKey = readPublicKey(testParameters.context.pkCCR, gqGroup);

				const hash: string = testParameters.output;

				args.push({
					encryptionGroup: gqGroup,
					electionEventId: electionEventId,
					verificationCardSetId: verificationCardSetId,
					primesMappingTable: primesMappingTable,
					electionPublicKey: electionPublicKey,
					choiceReturnCodesEncryptionPublicKey: choiceReturnCodeEncryptionPublicKey,
					hash: hash,
					description: testParameters.description
				});
			});

			return args;
		}

		function readPrimesMappingTable(serializedPrimesMappingTable: any, gqGroup: GqGroup): PrimesMappingTable {
			const primesMappingTableEntries: PrimesMappingTableEntry[] = [];
			serializedPrimesMappingTable.forEach((entry: {
				v: string;
				pTilde: number;
				sigma: string;
				tau: string;
			}) => {
				primesMappingTableEntries.push(new PrimesMappingTableEntry(entry.v, PrimeGqElement.fromValue(entry.pTilde, gqGroup), entry.sigma, entry.tau));
			});
			return new PrimesMappingTable(GroupVector.from(primesMappingTableEntries));
		}

		function readPublicKey(serializedPublicKeyElements: any, gqGroup: GqGroup): ElGamalMultiRecipientPublicKey {
			const publicKeyElements: GqElement[] = readValues(serializedPublicKeyElements)
				.map(pk => GqElement.fromValue(pk, gqGroup))
				.elements();
			return new ElGamalMultiRecipientPublicKey(publicKeyElements);
		}

		function readValues(input: string[]): ImmutableArray<ImmutableBigInteger> {
			const values: ImmutableBigInteger[] = [];
			input.forEach(value => values.push(readValue(value)));
			return ImmutableArray.from(values);
		}

		function readValue(value: string): ImmutableBigInteger {
			return ImmutableBigInteger.fromString(value.substring(2), 16);
		}

		// @ts-ignore description is used by test definition
		test.each(jsonFileArgumentProvider())("$description", ({
																   encryptionGroup,
																   electionEventId,
																   verificationCardSetId,
																   primesMappingTable,
																   electionPublicKey,
																   choiceReturnCodesEncryptionPublicKey,
																   hash,
																   description
															   }) => {

			expect(getHashContext(encryptionGroup, electionEventId, verificationCardSetId, primesMappingTable, electionPublicKey, choiceReturnCodesEncryptionPublicKey))
				.toBe(hash);
		})
	});

	function genUUID(): string {
		// One character can be represented by 4 bits in Base16 encoding, so to represent 32 characters, 16 bytes are sufficient.
		return base16.base16Encode(random.randomBytes(16).value());
	}
});
