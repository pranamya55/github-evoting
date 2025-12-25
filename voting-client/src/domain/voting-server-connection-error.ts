/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

export class VotingServerConnectionError extends Error {
	private readonly errorResponse: object;
	private readonly cause: string;

	constructor(cause: string) {
		super("Voting Server connection error.");

		this.errorResponse = {errorStatus: 'CONNECTION_ERROR'};
		this.cause = cause;

		// Set the prototype explicitly.
		Object.setPrototypeOf(this, VotingServerConnectionError.prototype);
	}
}