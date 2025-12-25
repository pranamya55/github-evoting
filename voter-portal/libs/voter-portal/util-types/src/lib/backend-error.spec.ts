/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { BackendError } from './backend-error';
import { ErrorStatus } from './error-status';

describe('BackendError', () => {
	jest.spyOn(console, 'error').mockImplementation(() => {
		/* do nothing */
	});

	describe('number-errors', () => {
		it('should create correct message for 0', () => {
			const error = new BackendError(0);
			expect(error.errorStatus).toBe(ErrorStatus.ConnectionError);
		});
		it('should create correct message for 401', () => {
			const error = new BackendError(401);
			expect(error.errorStatus).toBe(ErrorStatus.ExtendedFactorInvalid);
		});
		it('should create correct message for 403', () => {
			const error = new BackendError(403);
			expect(error.errorStatus).toBe(
				ErrorStatus.AuthenticationAttemptsExceeded,
			);
		});
		it('should create correct message for 404', () => {
			const error = new BackendError(404);
			expect(error.errorStatus).toBe(ErrorStatus.StartVotingKeyInvalid);
		});
	});

	describe('OvApiValidationError', () => {
		it('should create correct message for OvApiValidationError, CONFIRMATION_ATTEMPTS_EXCEEDED', () => {
			const error = new BackendError({
				validationError: {
					validationErrorType: 'CONFIRMATION_ATTEMPTS_EXCEEDED',
				},
			});
			expect(error.errorStatus).toBe(ErrorStatus.ConfirmationAttemptsExceeded);
		});

		it('should create correct message for OvApiValidationError, CONFIRMATION_KEY_INVALID', () => {
			const error = new BackendError({
				validationError: {
					validationErrorType: 'CONFIRMATION_KEY_INVALID',
					errorArgs: [10],
				},
			});
			expect(error.errorStatus).toBe(ErrorStatus.ConfirmationKeyInvalid);
			expect(error.numberOfRemainingAttempts).toBe(10);
		});
	});

	describe('OvApiNumberOfAttemptsError, OvApiShortValidationError and httpStatus', () => {
		it('should create correct message for OvApiNumberOfAttemptsError', () => {
			const error = new BackendError({ numberOfRemainingAttempts: 3 });
			expect(error.numberOfRemainingAttempts).toBe(3);
		});

		it('should create correct message for OvApiShortValidationError', () => {
			const error = new BackendError({ errorStatus: 'TEST' });
			expect(error.errorStatus).toBe('TEST');
		});

		it('should create default message ERROR for unknown httpStatus', () => {
			const error = new BackendError({ httpStatus: 123 });
			expect(error.errorStatus).toBe(ErrorStatus.Default);
		});
	});
});
