/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

export class VotingServerResponseError extends Error {
	private readonly errorResponse: object;
	private readonly cause: string;

	constructor(httpStatus: number, errorJson: any) {
		super("Voting Server response error.");

		// Add http status to the response
		errorJson.status = httpStatus;

		if (errorJson.errorStatus === "EXTENDED_FACTOR_INVALID") {
			const serverTimestamp = errorJson.timestamp;
			const clientTimestamp = Math.floor(Date.now() / 1000);

			const timestampDifference = clientTimestamp - serverTimestamp;
			if (Math.abs(timestampDifference) > 300) {
				errorJson.errorStatus = "TIMESTAMP_MISALIGNMENT";
			}
		}

		this.errorResponse = errorJson;
		this.cause = errorJson.errorStatus;

		// Set the prototype explicitly.
		Object.setPrototypeOf(this, VotingServerResponseError.prototype);
	}
}