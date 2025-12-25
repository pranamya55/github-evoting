/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ActionReducer, INIT, MetaReducer, UPDATE } from '@ngrx/store';
import { Paths, SHARED_FEATURE_KEY, SharedState } from '@vp/voter-portal-util-types';
import { get, merge, set } from 'lodash';

const locallyStored: Paths<SharedState>[] = ['currentLanguage'];

const hydrationMetaReducer = (
	reducer: ActionReducer<SharedState>,
): ActionReducer<SharedState> => {
	return (state, action) => {
		const nextState = reducer(state, action);

		if (action.type === INIT) {
			const storageValue = sessionStorage.getItem('state');
			if (storageValue) {
				try {
					merge(nextState, JSON.parse(storageValue));
				} catch {
					sessionStorage.removeItem('state');
				}
			}
		}

		sessionStorage.setItem('state', JSON.stringify(nextState));

		return nextState;
	};
};

const localHydrationMetaReducer = (
	reducer: ActionReducer<SharedState>,
): ActionReducer<SharedState> => {
	return (state, action) => {
		const nextState = reducer(state, action);

		if (action.type === UPDATE) {
			locallyStored.forEach((path) => {
				const storageSlice = localStorage.getItem(path);
				if (storageSlice) {
					try {
						set(
							nextState,
							`${SHARED_FEATURE_KEY}.${path}`,
							JSON.parse(storageSlice),
						);
					} catch {
						localStorage.removeItem(path);
					}
				}
			});
		}

		locallyStored.forEach((path) => {
			const stateSlice = get(nextState, `${SHARED_FEATURE_KEY}.${path}`);
			if (stateSlice) {
				localStorage.setItem(path, JSON.stringify(stateSlice));
			}
		});

		return nextState;
	};
};

export const metaReducers: MetaReducer[] = [
	hydrationMetaReducer,
	localHydrationMetaReducer,
];
