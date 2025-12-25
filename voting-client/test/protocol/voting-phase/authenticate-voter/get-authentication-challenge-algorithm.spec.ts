/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {SVK_ALPHABET} from "../../../../src/domain/start-voting-key-alphabet";
import {RandomService} from "crypto-primitives-ts/lib/esm/math/random_service";
import {NullPointerError} from "crypto-primitives-ts/lib/esm/error/null_pointer_error";
import {ImmutableBigInteger} from "crypto-primitives-ts/lib/esm/immutable_big_integer";
import {IllegalArgumentError} from "crypto-primitives-ts/lib/esm/error/illegal_argument_error";
import {FailedValidationError} from "../../../../src/domain/validations/failed-validation-error";
import {AuthenticationChallengeOutput} from "../../../../src/protocol/voting-phase/authenticate-voter/get-authentication-challenge.types";
import {getAuthenticationChallenge} from "../../../../src/protocol/voting-phase/authenticate-voter/get-authentication-challenge.algorithm";

import getAuthenticationChallengeJson from "./get-authentication-challenge.json";

describe("Get authentication challenge", function (): void {

	const electionEventId: string = "34caee78ed3d4cf981ca06b659f558eb";
	const authenticationStep: string = "authenticateVoter";
	const startVotingKey: string = "4d65ej2adb4ia6ghhzb52kg6";
	const extendedAuthenticationFactor: string = "01061944";
	const credentialId: string = "9660D63A4AB22ECCEF143D213BAF3EF2";

	describe("should return expected credential id when given valid input", function (): void {

		test("long extended authentication factor", async function (): Promise<void> {
			const authenticationChallengeOutput: AuthenticationChallengeOutput = await getAuthenticationChallenge(
				{
					electionEventId: electionEventId,
					extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
				},
				authenticationStep,
				startVotingKey,
				extendedAuthenticationFactor
			)

			expect(authenticationChallengeOutput.derivedVoterIdentifier).toBe(credentialId);
		});

		test("short extended authentication factor", async function (): Promise<void> {
			const authenticationChallengeOutput: AuthenticationChallengeOutput = await getAuthenticationChallenge(
				{
					electionEventId: electionEventId,
					extendedAuthenticationFactorLength: extendedAuthenticationFactor.substring(4).length
				},
				authenticationStep,
				startVotingKey,
				extendedAuthenticationFactor.substring(4)
			);

			expect(authenticationChallengeOutput.derivedVoterIdentifier).toBe(credentialId);
		});

	});

	describe("should throw an Error when given null arguments", function (): void {

		test("election event id", async function (): Promise<void> {
			await expect(getAuthenticationChallenge(
				{
					electionEventId: null,
					extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
				},
				authenticationStep,
				startVotingKey,
				extendedAuthenticationFactor)).rejects.toThrow(NullPointerError);
		});

		test("authentication step", async function (): Promise<void> {
			await expect(
				getAuthenticationChallenge(
					{
						electionEventId: electionEventId,
						extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
					},
					null,
					startVotingKey,
					extendedAuthenticationFactor
				)
			).rejects.toThrow(NullPointerError);
		});

		test("start voting key", async function (): Promise<void> {
			await expect(
				getAuthenticationChallenge(
					{
						electionEventId: electionEventId,
						extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
					},
					authenticationStep,
					null,
					extendedAuthenticationFactor)
			).rejects.toThrow(NullPointerError);
		});

		test("extended authentication factor", async function (): Promise<void> {
			await expect(
				getAuthenticationChallenge(
					{
						electionEventId: electionEventId,
						extendedAuthenticationFactorLength: 4
					},
					authenticationStep,
					startVotingKey,
					null)
			).rejects.toThrow(NullPointerError);
		});

	});

	test("should throw an Error when given invalid authentication step ", async function (): Promise<void> {
		const expectedErrorMessage: string = "The authentication step must be one of the valid values.";
		await getAuthenticationChallenge(
			{
				electionEventId: electionEventId,
				extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
			},
			"invalidAuthStep",
			startVotingKey,
			extendedAuthenticationFactor
		).catch(error => {
			expect(error).toBeInstanceOf(IllegalArgumentError);
			expect(error.message).toEqual(expectedErrorMessage);
		});

	});

	test("should throw an Error when given non UUID election event id", async function (): Promise<void> {
		const nonUuidElectionEventId: string = "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!";
		const expectedErrorMessage: string = `The given string does not comply with the required format. [string: ${nonUuidElectionEventId}, format: ^[a-fA-F0-9]{32}$].`;

		await getAuthenticationChallenge(
			{
				electionEventId: nonUuidElectionEventId,
				extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
			},
			authenticationStep,
			startVotingKey,
			extendedAuthenticationFactor)
			.catch(error => {
				expect(error).toBeInstanceOf(FailedValidationError);
				expect(error.message).toEqual(expectedErrorMessage);
			});
	});

	test("should throw an Error when given non valid start voting key", async function (): Promise<void> {
		const nonValidStartVotingKey: string = "111111111111111111111111";
		const expectedErrorMessage: string = `The given string does not comply with the required format. [string: ${nonValidStartVotingKey}, format: ^[${SVK_ALPHABET.join('')}]{24}$].`;

		await getAuthenticationChallenge(
			{
				electionEventId: electionEventId,
				extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
			},
			authenticationStep,
			nonValidStartVotingKey,
			extendedAuthenticationFactor
		).catch(error => {
			expect(error).toBeInstanceOf(FailedValidationError);
			expect(error.message).toEqual(expectedErrorMessage);
		});
	});

	describe("should throw an Error when given non compliant extended authentication factor", function (): void {

		test("non digit", async function (): Promise<void> {
			const nonCompliantExtendedAuthenticationFactor: string = "a106";
			const expectedErrorMessage: string = "The extended authentication factor must be a digit of correct size.";

			await getAuthenticationChallenge(
				{
					electionEventId: electionEventId,
					extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
				},
				authenticationStep,
				startVotingKey,
				nonCompliantExtendedAuthenticationFactor
			).catch(error => {
				expect(error).toBeInstanceOf(FailedValidationError);
				expect(error.message).toEqual(expectedErrorMessage);
			});
		});

		test("incorrect size", async function (): Promise<void> {
			const nonCompliantExtendedAuthenticationFactor: string = "010644";
			const expectedErrorMessage: string = `Unsupported extended authentication factor length provided. [length: ${nonCompliantExtendedAuthenticationFactor.length}]`;

			await getAuthenticationChallenge(
				{
					electionEventId: electionEventId,
					extendedAuthenticationFactorLength: nonCompliantExtendedAuthenticationFactor.length
				},
				authenticationStep,
				startVotingKey,
				nonCompliantExtendedAuthenticationFactor
			).catch(error => {
				expect(error).toBeInstanceOf(IllegalArgumentError);
				expect(error.message).toEqual(expectedErrorMessage);
			});
		});

	});


	describe("with specific values should give expected output", function (): void {
		const parameters = JSON.parse(JSON.stringify(getAuthenticationChallengeJson));

		let genRandomIntegerMock: jest.SpyInstance<ImmutableBigInteger>;
		let dateNowMock: jest.SpyInstance<number>;

		// Use beforeEach to set the mocked value before each test
		beforeEach(() => {
			// Mock the genRandomInteger method on RandomService prototype
			genRandomIntegerMock = jest.spyOn(RandomService.prototype, 'genRandomInteger');
			// Mock the Date.now() call in the getTimestamp function
			dateNowMock = jest.spyOn(global.Date, 'now');
		});

		// Clean up mocks after each test to avoid interference
		afterEach(() => {
			jest.clearAllMocks();
		});

		parameters.forEach((x) => {
			const electionEventId = x.context.ee;
			const authenticationStep = x.context.authStep;
			const startVotingKey = x.input.SVK_id;
			const extendedAuthenticationFactor = x.input.EA_id;
			const expectedDerivedVoterIdentifier = x.output.credentialID_id;
			const expectedDerivedAuthenticationChallenge = x.output.hhAuth_id;
			const expectedNonceValue = ImmutableBigInteger.fromString(x.output.nonce);

			const mockedNonceValue = ImmutableBigInteger.fromString(x.mocked.nonce);
			const mockedTimestampValue = x.mocked.t;

			test(x.description, async function (): Promise<void> {
				// Set the mock to return the correct nonce value for this test
				genRandomIntegerMock.mockReturnValueOnce(mockedNonceValue);
				// Set the mock to return the correct timestamp value for this test
				dateNowMock.mockReturnValueOnce(mockedTimestampValue);

				const authenticationChallengeOutput: AuthenticationChallengeOutput = await getAuthenticationChallenge(
					{
						electionEventId: electionEventId,
						extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
					},
					authenticationStep,
					startVotingKey,
					extendedAuthenticationFactor
				);

				expect(authenticationChallengeOutput.derivedVoterIdentifier).toEqual(expectedDerivedVoterIdentifier);
				expect(authenticationChallengeOutput.derivedAuthenticationChallenge).toEqual(expectedDerivedAuthenticationChallenge);
				expect(authenticationChallengeOutput.authenticationNonce).toEqual(expectedNonceValue);
			});
		});
	});
});
