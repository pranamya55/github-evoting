/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import * as AngularCore from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
	ActivatedRouteSnapshot,
	convertToParamMap,
	Router,
} from '@angular/router';
import { MockProvider } from 'ng-mocks';
import { RandomElectionEventId } from '@vp/shared-util-testing';
import { isElectionEventIdValid } from './is-election-event-id-valid.guard';

describe('isElectionEventIdValid', () => {
	let router: Router;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				MockProvider(Router, {
					createUrlTree: jest.fn().mockImplementation((commands) => {
						return commands.join('/');
					}),
				}),
			],
		});

		router = TestBed.inject(Router);
	});

	it('should return a UrlTree to "Page not Found" if the election event id is missing', () => {
		const route = {
			paramMap: convertToParamMap({}),
		} as ActivatedRouteSnapshot;

		const result = TestBed.runInInjectionContext(() =>
			isElectionEventIdValid(route),
		);

		expect(result).toBe('page-not-found');
	});

	it('should return a UrlTree to "Page not Found" if the election event id has an incorrect format', () => {
		const route = {
			paramMap: convertToParamMap({ electionEventId: '123' }),
		} as ActivatedRouteSnapshot;

		const result = TestBed.runInInjectionContext(() =>
			isElectionEventIdValid(route),
		);

		expect(result).toBe('page-not-found');
	});

	it('should return true if the election event id is has a correct format', () => {
		const route = {
			paramMap: convertToParamMap({ electionEventId: RandomElectionEventId() }),
		} as ActivatedRouteSnapshot;

		const result = TestBed.runInInjectionContext(() =>
			isElectionEventIdValid(route),
		);

		expect(result).toBe(true);
	});
});
