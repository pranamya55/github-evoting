/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Location} from '@angular/common';
import {TestBed} from '@angular/core/testing';
import {Router, RouterModule, RouterOutlet} from '@angular/router';

import {routes} from './app-routing.module';
import {LegalTermsComponent} from "@vp/voter-portal-step-legal-terms";
import {MockComponent} from "ng-mocks";
import {areLegalTermsAccepted, confirmDeactivationIfNeeded, isConfigLoaded, isElectionEventIdValid} from "@vp/voter-portal-ui-guards";
import {VotingRoutePath} from "@vp/voter-portal-util-types";
import {RandomElectionEventId, RandomString} from "@vp/shared-util-testing";
import {Component} from "@angular/core";
import {By} from "@angular/platform-browser";
import {PageNotFoundComponent} from "./page-not-found/page-not-found.component";

jest.mock('@vp/voter-portal-ui-guards', () => ({
	isConfigLoaded: jest.fn(),
	isElectionEventIdValid: jest.fn(),
	areLegalTermsAccepted: jest.fn(),
	confirmDeactivationIfNeeded: jest.fn(),
}));

@Component({
	standalone: true,
	imports: [RouterOutlet],
	template: `
		<router-outlet/>`
})
class MockAppComponent {
}

describe('AppRoutingModule', () => {
	let router: Router;
	let location: Location;
	// all voting route paths except 'init' which required more complex path construction.
	const votingRoutePaths = Object.values(VotingRoutePath).filter(votingRoutePath => votingRoutePath !== VotingRoutePath.Init);

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				MockComponent(PageNotFoundComponent)
			],
			imports: [
				RouterModule.forRoot(routes),
				MockComponent(LegalTermsComponent)
			],
		}).compileComponents();

		router = TestBed.inject(Router);
		location = TestBed.inject(Location);
	});

	const canActivate = async (path: string) => {
		await router.navigate([path]);
		const locationPath = location.path();
		return locationPath === `/${path}`;
	};

	const canDeactivate = async (path: string) => {
		await router.navigate([path]);
		await router.navigate(['']);
		return location.path() === '';
	};

	describe('isElectionEventIdValid', () => {
		it('should prevent navigation to initialization page when the election event ID is invalid', async () => {
			(isElectionEventIdValid as jest.Mock).mockReturnValueOnce(false);
			expect(await canActivate(`init/${RandomElectionEventId()}`)).toBeFalsy();
		});

		it('should allow navigation to initialization page when the election event ID is valid', async () => {
			(isElectionEventIdValid as jest.Mock).mockReturnValueOnce(true);
			expect(await canActivate(`init/${RandomElectionEventId()}`)).toBeTruthy();
		});
	});

	describe('isConfigLoaded', () => {
		it('should prevent navigation to "legal-terms" when the config is not loaded', async () => {
			(isConfigLoaded as jest.Mock).mockReturnValueOnce(false);
			expect(await canActivate(VotingRoutePath.LegalTerms)).toBeFalsy();
		});

		it('should allow navigation to "legal-terms" when the config is loaded', async () => {
			(isConfigLoaded as jest.Mock).mockReturnValueOnce(true);
			expect(await canActivate(VotingRoutePath.LegalTerms)).toBeTruthy();
		});
	});

	describe('areLegalTermsAccepted', () => {
		const guardedSteps = votingRoutePaths.filter(votingRoutePath => votingRoutePath !== VotingRoutePath.LegalTerms);

		guardedSteps.forEach(step => {
			it(`should prevent navigation to "${step}" when legal terms are not accepted`, async () => {
				(areLegalTermsAccepted as jest.Mock).mockReturnValueOnce(false);
				expect(await canActivate(step)).toBeFalsy();
			});

			it(`should allow navigation to "${step}" when legal terms are accepted`, async () => {
				(areLegalTermsAccepted as jest.Mock).mockReturnValueOnce(true);
				expect(await canActivate(step)).toBeTruthy();
			});
		});
	});

	describe('confirmDeactivationIfNeeded', () => {
		votingRoutePaths.forEach(step => {
			const path = step;

			it(`should prevent navigation away from "${step}" when a confirmation is needed`, async () => {
				(confirmDeactivationIfNeeded as jest.Mock).mockReturnValueOnce(false);
				expect(await canDeactivate(path)).toBeFalsy();
			});

			it(`should allow navigation away from "${step}" when no confirmation is needed`, async () => {
				(confirmDeactivationIfNeeded as jest.Mock).mockReturnValueOnce(true);
				expect(await canDeactivate(path)).toBeTruthy();
			});
		});
	});

	it('should prevent navigation to any unknown route', async () => {
		(isElectionEventIdValid as jest.Mock).mockReturnValueOnce(false);
		expect(await canActivate(RandomString())).toBeFalsy();
	});

	it('should show the PageNotFoundComponent on the base URL', async () => {
		const fixture = TestBed.createComponent(MockAppComponent);

		await router.navigate(['']);
		expect(location.path()).toBe('');

		fixture.detectChanges();

		const myComponentDebugElement = fixture.debugElement.query(By.directive(PageNotFoundComponent));
		expect(myComponentDebugElement).toBeTruthy();
	});
});
