/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { SHARED_FEATURE_KEY, SharedState } from '@vp/voter-portal-util-types';

export const MockStoreProvider = (initialState: Partial<SharedState> = {}) => {
	Object.freeze(initialState);
	return provideMockStore({
		initialState: {
			[SHARED_FEATURE_KEY]: initialState,
		},
	});
};

export const setState = (store: MockStore, state: Partial<SharedState>) => {
	store.setState({ [SHARED_FEATURE_KEY]: state });
};
