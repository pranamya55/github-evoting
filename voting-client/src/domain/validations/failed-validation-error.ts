/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

export class FailedValidationError extends Error {
	constructor(m?: string) {
		super(m);

		// Set the prototype explicitly.
		Object.setPrototypeOf(this, FailedValidationError.prototype);
	}
}