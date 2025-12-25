/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {
	ActivatedRouteSnapshot,
	Router,
	RouterStateSnapshot,
} from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { MockStore } from '@ngrx/store/testing';
import { MockProvider } from 'ng-mocks';
import {
	MockStoreProvider,
	RandomElectionEventId,
	setState,
} from '@vp/shared-util-testing';
import { isConfigLoaded } from './is-config-loaded.guard';
import { ExtendedFactor } from '@vp/voter-portal-util-types';

describe('isConfigLoaded', () => {
	let router: Router;
	let store: MockStore;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				MockStoreProvider(),
				MockProvider(Router, {
					createUrlTree: jest
						.fn()
						.mockImplementation((commands) => commands.join('/')),
				} as unknown as Router),
			],
		});

		router = TestBed.inject(Router);
		store = TestBed.inject(MockStore);
	});

	it('should return true if getConfig is truthy', () => {
		setState(store, {
			config: {
				identification: ExtendedFactor.YearOfBirth,
				contestsCapabilities: {
					writeIns: true,
				},
				requestTimeout: {
					authenticateVoter: 0,
					sendVote: 0,
					confirmVote: 0,
				},
				header: {
					logo: '',
					logoHeight: { desktop: 0, mobile: 0 },
				},
				electionEventId: RandomElectionEventId(),
				favicon: '',
			},
		});

		TestBed.runInInjectionContext(() => {
			const result = isConfigLoaded(
				{} as ActivatedRouteSnapshot,
				{} as RouterStateSnapshot,
			);

			expect(result).toBe(true);
		});
	});

	it('should return a UrlTree if getConfig is falsy', () => {
		setState(store, {
			config: {
				identification: ExtendedFactor.YearOfBirth,
				contestsCapabilities: {
					writeIns: true,
				},
				requestTimeout: {
					authenticateVoter: 0,
					sendVote: 0,
					confirmVote: 0,
				},
				header: {
					logo: '',
					logoHeight: { desktop: 0, mobile: 0 },
				},
				electionEventId: '',
				favicon: '',
			},
		});

		TestBed.runInInjectionContext(() => {
			const result = isConfigLoaded(
				{} as ActivatedRouteSnapshot,
				{} as RouterStateSnapshot,
			);

			expect(router.createUrlTree).toHaveBeenCalledWith(['']);
			expect(result).toBe('');
		});
	});
});
