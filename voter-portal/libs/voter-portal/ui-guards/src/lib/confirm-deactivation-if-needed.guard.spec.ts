/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, signal, Signal, WritableSignal} from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
	ActivatedRouteSnapshot,
	Router,
	RouterStateSnapshot,
	Navigation,
} from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ProcessCancellationService } from '@vp/voter-portal-ui-services';
import { MockProvider } from 'ng-mocks';
import { NavigateAwayAction, VotingStep } from '@vp/voter-portal-util-types';
import { confirmDeactivationIfNeeded } from './confirm-deactivation-if-needed.guard';

describe('confirmDeactivationIfNeeded', () => {
	let cancellationService: ProcessCancellationService;
	let modalService: NgbModal;
	let mockNavigation: WritableSignal<Navigation | null>;

	beforeEach(() => {
		mockNavigation = signal(null);

		TestBed.configureTestingModule({
			providers: [
				MockProvider(Router, {
					createUrlTree: jest.fn().mockImplementation((commands) => {
						return commands.join('/');
					}),
					currentNavigation: mockNavigation,
				}),
				MockProvider(ProcessCancellationService),
				MockProvider(NgbModal),
			],
		});

		cancellationService = TestBed.inject(ProcessCancellationService);
		modalService = TestBed.inject(NgbModal);

		cancellationService.backButtonPressed = true;
		jest.spyOn(modalService, 'hasOpenModals').mockReturnValue(false);
	});

	it('should allow trusted navigations', () => {
		mockNavigation.set({extras: {info: {trusted: true}}} as Navigation);
		expect(TestBed.runInInjectionContext(() =>
			confirmDeactivationIfNeeded(
				{} as Component,
				{} as unknown as ActivatedRouteSnapshot,
				{} as RouterStateSnapshot,
				{} as RouterStateSnapshot,
			),
		)).toBe(true);
	});

	describe('route data with reachableSteps', () => {
		function runDeactivationGuard(nextURL: string) {
			return TestBed.runInInjectionContext(() =>
				confirmDeactivationIfNeeded(
					{} as Component,
					{
						data: { reachablePaths: ['authorized-url'] },
					} as unknown as ActivatedRouteSnapshot,
					{} as RouterStateSnapshot,
					{ url: nextURL } as RouterStateSnapshot,
				),
			);
		}

		it('should allow deactivation to navigate to an authorized URL', () => {
			const deactivationAllowed = runDeactivationGuard('authorized-url');

			expect(deactivationAllowed).toBe(true);
		});

		it('should not allow deactivation to navigate to an unauthorized URL', () => {
			const deactivationAllowed = runDeactivationGuard('unauthorized-url');

			expect(deactivationAllowed).toBe(false);
		});

		it('should allow deactivation if the "Back" button has not been pressed', () => {
			cancellationService.backButtonPressed = false;

			const deactivationAllowed = runDeactivationGuard('unauthorized-url');

			expect(deactivationAllowed).toBe(true);
		});

		it('should not allow deactivation if a modal is currently open', () => {
			jest.spyOn(modalService, 'hasOpenModals').mockReturnValueOnce(true);

			const deactivationAllowed = runDeactivationGuard('authorized-url');

			expect(deactivationAllowed).toBe(false);
		});
	});

	describe('route data with navigateAwayAction', () => {
		function runDeactivationGuard(navigateAwayAction: NavigateAwayAction) {
			return TestBed.runInInjectionContext(() =>
				confirmDeactivationIfNeeded(
					{} as Component,
					{
						data: { reachablePaths: ['authorized-url'], navigateAwayAction: navigateAwayAction },
					} as unknown as ActivatedRouteSnapshot,
					{} as RouterStateSnapshot,
					{ url: 'unauthorized-url' } as RouterStateSnapshot,
				),
			);
		}

		it('should show the "Cancel Vote" dialog', () => {
			jest.spyOn(cancellationService, 'cancelVote');

			const deactivationAllowed = runDeactivationGuard(
				NavigateAwayAction.ShowCancelVoteDialog,
			);

			expect(deactivationAllowed).toBe(false);
			expect(cancellationService.cancelVote).toHaveBeenCalled();
		});

		it('should show the "Leave Process" dialog', () => {
			jest.spyOn(cancellationService, 'leaveProcess');

			const deactivationAllowed = runDeactivationGuard(
				NavigateAwayAction.ShowLeaveProcessDialog,
			);

			expect(deactivationAllowed).toBe(false);
			expect(cancellationService.leaveProcess).toHaveBeenCalled();
		});

		it('should show the "Quit Process" dialog', () => {
			jest.spyOn(cancellationService, 'quitProcess');

			const deactivationAllowed = runDeactivationGuard(
				NavigateAwayAction.ShowQuitProcessDialog,
			);

			expect(deactivationAllowed).toBe(false);
			expect(cancellationService.quitProcess).toHaveBeenCalled();
		});

		it('should go back to the start voting page', () => {
			const deactivationUrlTree = runDeactivationGuard(
				NavigateAwayAction.GoToStartVotingPage,
			);

			expect(deactivationUrlTree).toBe(VotingStep.StartVoting);
		});

		it('should go back to the legal terms page', () => {
			const deactivationUrlTree = runDeactivationGuard(
				NavigateAwayAction.GoToLegalTermsPage,
			);

			expect(deactivationUrlTree).toBe(VotingStep.LegalTerms);
		});
	});
});
