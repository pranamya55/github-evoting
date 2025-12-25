/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {ZqElement} from "crypto-primitives-ts/lib/esm/math/zq_element";
import {PrimitivesParams} from "./domain/primitives-params.types";
import {VoterAuthenticationData} from "./domain/authenticate-voter.types";

export class SessionService {
	private static instance: SessionService;
	private sessionState = {};

	private constructor() {
	}

	public static getInstance(): SessionService {
		if (!SessionService.instance) {
			SessionService.instance = new SessionService();
		}
		return SessionService.instance;
	}

	/**
	 * Get / Set the StartVotingKey
	 * @param {string} [value] - the start voting key.
	 *
	 * @returns {string} - the start voting key.
	 */
	public startVotingKey(value?: string): string {
		return this.session("startVotingKey", value);
	}

	/**
	 * Get / Set the extended authentication factor.
	 * @param {string} [value] - the extended authentication factor.
	 *
	 * @returns {string} - the extended authentication factor.
	 */
	public extendedAuthenticationFactor(value?: string): string {
		return this.session("extendedAuthenticationFactor", value);
	}

	/**
	 * Get / Set the primitives params.
	 * @param {PrimitivesParams} [value] - the primitives params object.
	 *
	 * @returns {PrimitivesParams} - the primitives params object.
	 */
	public primitivesParams(value?: PrimitivesParams): PrimitivesParams {
		return this.session("primitivesParams", value);
	}

	/**
	 * Get / Set the voter authentication data.
	 * @param {VoterAuthenticationData} [value] - the voter authentication data object.
	 * @returns {VoterAuthenticationData} the voter authentication data object.
	 */
	public voterAuthenticationData(value?: VoterAuthenticationData): VoterAuthenticationData {
		return this.session("voterAuthenticationData", value);
	}

	/**
	 * Get / Set the verification card secret key.
	 * @param {ZqElement} [value] - the verification card secret key.
	 *
	 * @returns {ZqElement} - the verification card secret key.
	 */
	public verificationCardSecretKey(value?: ZqElement): ZqElement {
		return this.session("verificationCardSecretKey", value);
	}

	private session(name: string, value?: any) {
		if (value) {
			// @ts-ignore
			this.sessionState[name] = value;
		}
		// @ts-ignore
		return this.sessionState[name];
	}

}
