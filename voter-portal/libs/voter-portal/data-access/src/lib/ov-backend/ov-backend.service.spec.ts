/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { TestBed } from '@angular/core/testing';
import { BackendError, OvApi, Voter, VoterPortalConfig } from '@vp/voter-portal-util-types';
import { OvBackendService } from './ov-backend.service';
import { VoterAnswers } from 'e-voting-libraries-ui-kit';

const mockVoter: Voter = {
	startVotingKey: '',
	extendedFactor: '',
};

class MockOvApi implements OvApi {
	authenticateVoter = jest.fn().mockReturnValue(Promise.resolve({}));
	sendVote = jest.fn().mockReturnValue(Promise.resolve([]));
	confirmVote = jest.fn().mockReturnValue(Promise.resolve(''));
	isBrowserCompatible = jest.fn().mockReturnValue(Promise.resolve(true));
	configureVoterPortal = jest.fn().mockReturnValue(Promise.resolve({}));
}

describe('BackendService', () => {
	let backendService: OvBackendService;
	let mockOvApi: MockOvApi;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [OvBackendService],
		});

		backendService = TestBed.inject(OvBackendService);

		mockOvApi = new MockOvApi();
		Object.assign(global, { OvApi: () => mockOvApi });
		Object.assign(window, { OvApi: mockOvApi });
	});

	function throwError() {
		throw new Error();
	}

	describe('authenticateVoter', () => {
		it('should return the authentication response fetched from the OvApi', async () => {
			const mockOvApiResponse = {
				mockAuthenticateVoterResponse: 'mockAuthenticateVoterResponse',
			};

			mockOvApi.authenticateVoter.mockReturnValueOnce(mockOvApiResponse);
			const authenticateVoterResponse = await backendService.authenticateVoter(
				mockVoter,
				{} as VoterPortalConfig,
			);

			expect(authenticateVoterResponse).toBe(mockOvApiResponse);
		});

		it('should throw a backend error if the OvApi throws an error', async () => {
			mockOvApi.authenticateVoter.mockImplementationOnce(throwError);

			await expect(
				backendService.authenticateVoter(mockVoter, {} as VoterPortalConfig),
			).rejects.toBeInstanceOf(BackendError);
		});
	});

	describe('sendVote', () => {
		it('should return the short choice return codes fetched from the OvApi', async () => {
			const mockShortChoiceReturnCodes = [
				{
					questionIdentification: 'mockQuestionIdentification',
					shortChoiceReturnCode: 'mockChoiceReturnCode',
				},
			];

			mockOvApi.sendVote.mockReturnValueOnce({
				shortChoiceReturnCodes: mockShortChoiceReturnCodes,
			});
			const sendVoteResponse = await backendService.sendVote(
				{} as VoterAnswers,
			);

			expect(sendVoteResponse).toBe(mockShortChoiceReturnCodes);
		});

		it('should throw a backend error if the OvApi throws an error', async () => {
			mockOvApi.sendVote.mockImplementationOnce(throwError);

			await expect(
				backendService.sendVote({} as VoterAnswers),
			).rejects.toBeInstanceOf(BackendError);
		});
	});

	describe('confirmVote', () => {
		it('should return the vote cast code fetched from the OvApi', async () => {
			const mockVoteCastReturnCode = 'mockVoteCastReturnCode';

			mockOvApi.confirmVote.mockReturnValueOnce({
				voteCastReturnCode: mockVoteCastReturnCode,
			});
			const confirmVoteResponse = await backendService.confirmVote('');

			expect(confirmVoteResponse).toBe(mockVoteCastReturnCode);
		});

		it('should throw a backend error if the OvApi throws an error', async () => {
			mockOvApi.confirmVote.mockImplementationOnce(throwError);

			await expect(backendService.confirmVote('')).rejects.toBeInstanceOf(
				BackendError,
			);
		});
	});

	describe('isBrowserCompatible', () => {
		it('should return true if the OvApi indicates the browser is compatible', async () => {
			mockOvApi.isBrowserCompatible.mockReturnValueOnce(Promise.resolve(true));
			const isCompatible = await backendService.isBrowserCompatible();
			expect(isCompatible).toBe(true);
		});

		it('should throw a backend error if the OvApi throws an error', async () => {
			mockOvApi.isBrowserCompatible.mockImplementationOnce(throwError);
			await expect(backendService.isBrowserCompatible()).rejects.toBeInstanceOf(
				BackendError,
			);
		});
	});
});
