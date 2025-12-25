/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { BackendError, ErrorStatus } from '@vp/voter-portal-util-types';
import {
	fromEvent,
	ObservableInput,
	ObservedValueOf,
	OperatorFunction,
	pipe,
	raceWith,
	startWith,
	switchMap,
	throwError,
	timeout as onTimeout,
} from 'rxjs';
import { filter, map } from 'rxjs/operators';

export function catchConnectionError<T, O extends ObservableInput<any>>(
	timeout: number,
): OperatorFunction<T, T | ObservedValueOf<O>> {
	// Creates an observable that will throw a connection BackendError
	const throwConnectionError = () =>
		throwError(
			() => new BackendError({ errorStatus: ErrorStatus.ConnectionError }),
		);

	// An observable that throws a connection BackendError whenever the browser is offline
	const noConnection$ = fromEvent(window, 'offline').pipe(
		map(() => true),
		startWith(!window.navigator.onLine),
		filter((isOffLine) => isOffLine),
		switchMap(() => throwConnectionError()),
	);

	return pipe(
		// if the browser becomes offline, then the current observable is cancelled and a connection BackendError is thrown
		raceWith(noConnection$),

		// when defined timeout has pass a connection BackendError is thrown
		onTimeout({ first: timeout, with: () => throwConnectionError() }),
	);
}
