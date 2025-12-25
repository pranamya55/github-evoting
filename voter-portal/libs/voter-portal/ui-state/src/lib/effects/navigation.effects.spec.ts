/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { Action } from '@ngrx/store';
import { Observable, of } from 'rxjs';
import { ReviewActions, StartVotingActions } from '@vp/voter-portal-ui-state';

import { NavigationEffects } from './navigation.effects';

describe('NavigationEffects', () => {
	let actions$: Observable<Action>;
	let effects: NavigationEffects;
	let router: Router;

	const trustedNavigationExtras = {'info': {'trusted': true}};

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [RouterTestingModule],
			providers: [NavigationEffects, provideMockActions(() => actions$)],
		});
		effects = TestBed.inject(NavigationEffects);
		router = TestBed.inject(Router);
		router.navigate = jest.fn();
	});

	it('should navigate to the start voting page after a logout', () => {
		actions$ = of(
			StartVotingActions.textsLoaded({
				votesTexts: [],
				electionsTexts: [],
				writeInAlphabet: '',
			}),
		);
		effects.navigateToChoose$.subscribe();
		expect(router.navigate).toHaveBeenNthCalledWith(1, ['/choose'], trustedNavigationExtras);
	});

	it('should navigate to the start voting page after the legal terms have been agreed', () => {
		actions$ = of(
			StartVotingActions.textsLoaded({
				votesTexts: [],
				electionsTexts: [],
				writeInAlphabet: '',
			}),
		);
		effects.navigateToChoose$.subscribe();
		expect(router.navigate).toHaveBeenNthCalledWith(1, ['/choose'], trustedNavigationExtras);
	});

	it('should navigate to the choose page after the ballot has been loaded', () => {
		actions$ = of(
			StartVotingActions.textsLoaded({
				votesTexts: [],
				electionsTexts: [],
				writeInAlphabet: '',
			}),
		);
		effects.navigateToChoose$.subscribe();
		expect(router.navigate).toHaveBeenNthCalledWith(1, ['/choose'], trustedNavigationExtras);
	});

	it('should navigate to the choose page after the vote seal has been canceled', () => {
		actions$ = of(ReviewActions.sealVoteCanceled());
		effects.navigateToChoose$.subscribe();
		expect(router.navigate).toHaveBeenNthCalledWith(1, ['/choose'], trustedNavigationExtras);
	});

	it('should navigate to the verify page after the short choice return codes have been loaded', () => {
		actions$ = of(
			StartVotingActions.shortChoiceReturnCodesLoaded({
				votesTexts: [],
				electionsTexts: [],
				writeInAlphabet: '',
				shortChoiceReturnCodes: [],
			}),
		);
		effects.navigateToVerify$.subscribe();
		expect(router.navigate).toHaveBeenNthCalledWith(1, ['/verify'], trustedNavigationExtras);
	});

	it('should navigate to the end page after the vote cast return code has been loaded', () => {
		actions$ = of(
			StartVotingActions.voteCastReturnCodeLoaded({ voteCastReturnCode: '' }),
		);
		effects.navigateToFinalization$.subscribe();
		expect(router.navigate).toHaveBeenNthCalledWith(1, ['/end'], trustedNavigationExtras);
	});
});
