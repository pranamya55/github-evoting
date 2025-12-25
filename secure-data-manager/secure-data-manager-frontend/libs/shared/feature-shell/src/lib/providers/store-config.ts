/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {INIT, MetaReducer} from '@ngrx/store';

const STORAGE_KEY = 'sdm.session';

const sessionStorageMetaReducer: MetaReducer = (reducer) => {
  return (state, action) => {
    const nextState = reducer(state, action);

    if (action.type === INIT) {
      const storageValue = sessionStorage.getItem(STORAGE_KEY);
      if (storageValue) {
        try {
          Object.assign(nextState, JSON.parse(storageValue));
        } catch {
          sessionStorage.removeItem(STORAGE_KEY);
        }
      }
    }

    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(nextState));

    return nextState;
  };
};

export const storeConfig = { metaReducers: [sessionStorageMetaReducer] };
