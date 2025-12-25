/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

export class VotingClientTimeError extends Error {
	private readonly errorResponse: object;

	constructor() {
		super("Voting Client time error.");

		this.errorResponse = {errorStatus: 'VOTING_CLIENT_TIME_ERROR'};

		// Set the prototype explicitly.
		Object.setPrototypeOf(this, VotingClientTimeError.prototype);
	}
}