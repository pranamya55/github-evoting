/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {NullPointerError} from "crypto-primitives-ts/lib/esm/error/null_pointer_error";
import {FailedValidationError} from "../../../../src/domain/validations/failed-validation-error";
import {deriveBaseAuthenticationChallenge} from "../../../../src/protocol/preliminaries/voter-authentication/derive-base-authentication-challenge.algorithm";
import {SVK_ALPHABET} from "../../../../src/domain/start-voting-key-alphabet";

describe("Derive base authentication challenge", function (): void {

	const electionEventId: string = "34caee78ed3d4cf981ca06b659f558eb";
	const startVotingKey: string = "4d65ej2adb4ia6ghhzb52kg6";
	const extendedAuthenticationFactor: string = "01061944";
	const challenge: string = "yIbfUp8gYa5lXkAHgv5R1fCr2w+Jhgq0c6jxcL5dgIs=";

	test("should return expected challenge", async function (): Promise<void> {
		const baseAuthenticationChallenge: string = await deriveBaseAuthenticationChallenge(
			{
				electionEventId: electionEventId,
				extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
			},
			startVotingKey,
			extendedAuthenticationFactor);
		expect(baseAuthenticationChallenge).toBe(challenge);
	});

	describe("should throw an Error when given null arguments", function (): void {

		test("election event id", async function (): Promise<void> {
			await expect(
				deriveBaseAuthenticationChallenge(
					{
						electionEventId: null,
						extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
					},
					startVotingKey,
					extendedAuthenticationFactor
				)
			).rejects.toThrow(NullPointerError);
		});

		test("start voting key", async function (): Promise<void> {
			await expect(
				deriveBaseAuthenticationChallenge(
					{
						electionEventId: electionEventId,
						extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
					},
					null,
					extendedAuthenticationFactor
				)
			).rejects.toThrow(NullPointerError);
		});

		test("extended authentication factor", async function (): Promise<void> {
			await expect(
				deriveBaseAuthenticationChallenge(
					{
						electionEventId: electionEventId,
						extendedAuthenticationFactorLength: 4
					},
					startVotingKey,
					null
				)
			).rejects.toThrow(NullPointerError);
		});

	});

	test("should throw an Error when given non UUID election event id", async function (): Promise<void> {
		const nonUuidElectionEventId: string = "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!";
		const expectedErrorMessage: string = `The given string does not comply with the required format. [string: ${nonUuidElectionEventId}, format: ^[a-fA-F0-9]{32}$].`;

		await deriveBaseAuthenticationChallenge(
			{
				electionEventId: nonUuidElectionEventId,
				extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
			},
			startVotingKey,
			extendedAuthenticationFactor
		).catch(error => {
			expect(error).toBeInstanceOf(FailedValidationError);
			expect(error.message).toEqual(expectedErrorMessage);
		});
	});

	test("should throw an Error when given non valid start voting key", async function (): Promise<void> {
		const nonValidStartVotingKey: string = "111111111111111111111111";
		const expectedErrorMessage: string = `The given string does not comply with the required format. [string: ${nonValidStartVotingKey}, format: ^[${SVK_ALPHABET.join('')}]{24}$].`;

		await deriveBaseAuthenticationChallenge(
			{
				electionEventId: electionEventId,
				extendedAuthenticationFactorLength: extendedAuthenticationFactor.length
			},
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
			const expectedErrorMessage: string = "The given string does not comply with the required format. [string: a106, format: ^(\\d{4})(\\d{4})?$].";

			await deriveBaseAuthenticationChallenge(
				{
					electionEventId: electionEventId,
					extendedAuthenticationFactorLength: 4
				},
				startVotingKey,
				nonCompliantExtendedAuthenticationFactor
			).catch(error => {
				expect(error).toBeInstanceOf(FailedValidationError);
				expect(error.message).toEqual(expectedErrorMessage);
			});
		});

		test("incorrect size", async function (): Promise<void> {
			const nonCompliantExtendedAuthenticationFactor: string = "010644";
			const expectedErrorMessage: string = "The extended authentication factor must be a digit of correct size.";

			await deriveBaseAuthenticationChallenge(
				{
					electionEventId: electionEventId,
					extendedAuthenticationFactorLength: 4
				},
				startVotingKey,
				nonCompliantExtendedAuthenticationFactor
			).catch(error => {
				expect(error).toBeInstanceOf(FailedValidationError);
				expect(error.message).toEqual(expectedErrorMessage);
			});
		});
	});

});
