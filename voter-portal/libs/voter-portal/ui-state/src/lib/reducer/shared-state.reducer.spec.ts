/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Action } from '@ngrx/store';
import { initialState, reducer, StartVotingActions } from '@vp/voter-portal-ui-state';
import { SharedState } from '@vp/voter-portal-util-types';

describe('SharedState Reducer', () => {
	describe('valid SharedState actions', () => {
		it('loadSvkSuccess should return authentication token', () => {
			const mockVoteCastCode = 'mockVoteCastCode';
			const action = StartVotingActions.voteCastReturnCodeLoaded({
				voteCastReturnCode: mockVoteCastCode,
			});

			const result: SharedState = reducer(initialState, action);

			expect(result.voteCastReturnCode?.length).toBeGreaterThan(0);
		});
	});

	describe('unknown action', () => {
		it('should return the previous state', () => {
			const action = {} as Action;

			const result = reducer(initialState, action);

			expect(result).toBe(initialState);
		});
	});
});
