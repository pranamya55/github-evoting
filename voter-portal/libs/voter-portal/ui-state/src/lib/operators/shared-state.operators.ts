/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { select } from '@ngrx/store';
import { pipe } from 'rxjs';
import { filter } from 'rxjs/operators';
import * as SharedSelectors from '../selectors/shared-state.selectors';
import { Nullable } from '@vp/voter-portal-util-types';

function isDefined<T>(item: Nullable<T>): item is T {
	return !!item;
}

export const getDefinedAnswers = pipe(
	select(SharedSelectors.getAnswers),
	filter(isDefined),
);

export const getDefinedWriteInAlphabet = pipe(
	select(SharedSelectors.getWriteInAlphabet),
	filter(isDefined),
);

export const getDefinedVoteCastReturnCode = pipe(
	select(SharedSelectors.getVoteCastReturnCode),
	filter(isDefined),
);

export const getDefinedShortChoiceReturnCodes = pipe(
	select(SharedSelectors.getShortChoiceReturnCodes),
	filter(isDefined),
);

export const getDefinedBackendError = pipe(
	select(SharedSelectors.getBackendError),
	filter(isDefined),
);
