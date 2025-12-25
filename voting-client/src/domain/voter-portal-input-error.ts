/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

export class VoterPortalInputError extends Error {
	private readonly errorResponse: object;
	private readonly cause: string;

	constructor(status: string, cause: string) {
		super("Voter Portal input error.");
		this.errorResponse = {errorStatus: status};
		this.cause = cause;
		// Set the prototype explicitly.
		Object.setPrototypeOf(this, VoterPortalInputError.prototype);
	}
}