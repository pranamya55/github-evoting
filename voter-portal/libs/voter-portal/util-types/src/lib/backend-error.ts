/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ErrorStatus } from './error-status';

type ErrorWithHttpStatus = {
	status: number;
};

type ErrorWithErrorStatus = {
	errorStatus: string;
};

type ErrorWithNumberOfAttempts = {
	numberOfRemainingAttempts: number;
};

type ValidationError = {
	validationError: { validationErrorType: string; errorArgs?: string[] };
};

export class BackendError extends Error {
	httpStatus?: number;
	errorStatus: string = ErrorStatus.Default;
	numberOfRemainingAttempts?: number;

	constructor(error?: unknown) {
		super(`Backend Error: ${JSON.stringify(error)}`);

		if (typeof error === 'number') {
			this.parseNumberError(error);
		}

		if (error && typeof error === 'object') {
			if ('status' in error) {
				this.httpStatus = (error as ErrorWithHttpStatus).status;
			}

			if ('errorStatus' in error) {
				this.errorStatus = (error as ErrorWithErrorStatus).errorStatus;
			}

			if ('numberOfRemainingAttempts' in error) {
				this.numberOfRemainingAttempts = (
					error as ErrorWithNumberOfAttempts
				).numberOfRemainingAttempts;
			}

			if ('validationError' in error) {
				this.parseValidationError(error as ValidationError);
			}
		}
	}

	private parseNumberError(error: number): void {
		this.httpStatus = error;
		switch (error) {
			case 0:
				this.errorStatus = ErrorStatus.ConnectionError;
				break;
			case 401:
				this.errorStatus = ErrorStatus.ExtendedFactorInvalid;
				break;
			case 403:
				this.errorStatus = ErrorStatus.AuthenticationAttemptsExceeded;
				break;
			case 404:
				this.errorStatus = ErrorStatus.StartVotingKeyInvalid;
		}
	}

	private parseValidationError(error: ValidationError): void {
		const { validationErrorType, errorArgs } = error.validationError;

		this.errorStatus = validationErrorType;

		if (
			this.errorStatus === ErrorStatus.ConfirmationKeyInvalid &&
			errorArgs?.length
		) {
			this.numberOfRemainingAttempts = parseInt(errorArgs[0]);
		}
	}
}
