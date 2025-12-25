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
import { MockStoreProvider, setState } from '@vp/shared-util-testing';
import { areLegalTermsAccepted } from './are-legal-terms-accepted.guard';

describe('areLegalTermsAccepted', () => {
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

	it('should return true if hasAcceptedLegalTerms is truthy', () => {
		setState(store, { hasAcceptedLegalTerms: true });

		TestBed.runInInjectionContext(() => {
			const result = areLegalTermsAccepted(
				{} as ActivatedRouteSnapshot,
				{} as RouterStateSnapshot,
			);

			expect(result).toBe(true);
		});
	});

	it('should return a UrlTree if hasAcceptedLegalTerms is falsy', () => {
		setState(store, { hasAcceptedLegalTerms: false });

		TestBed.runInInjectionContext(() => {
			const result = areLegalTermsAccepted(
				{} as ActivatedRouteSnapshot,
				{} as RouterStateSnapshot,
			);

			expect(router.createUrlTree).toHaveBeenCalledWith(['']);
			expect(result).toBe('');
		});
	});
});
