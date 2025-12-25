/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, inject } from '@angular/core';
import {
	ActivatedRouteSnapshot,
	Router,
	RouterStateSnapshot,
	UrlTree,
} from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ProcessCancellationService } from '@vp/voter-portal-ui-services';
import {NavigateAwayAction, RouteData, VotingRoutePath} from '@vp/voter-portal-util-types';

export const confirmDeactivationIfNeeded = (
	_component: Component,
	currentRoute: ActivatedRouteSnapshot,
	_currentState: RouterStateSnapshot,
	nextState?: RouterStateSnapshot | undefined,
): boolean | UrlTree => {
	const cancellationService = inject(ProcessCancellationService);
	const router = inject(Router);
	const modalService = inject(NgbModal);

	// allow all trusted navigation
	const navigationInfo = router.currentNavigation()?.extras.info;
	if (navigationInfo && typeof navigationInfo === 'object' && 'trusted' in navigationInfo) {
		return true;
	}

	if (!cancellationService.backButtonPressed) {
		return true;
	}

	cancellationService.backButtonPressed = false;

	if (modalService.hasOpenModals()) {
		history.pushState(null, '', location.href);
		return false;
	}

	const routeData = currentRoute.data as RouteData;
	if (routeData?.reachablePaths && nextState?.url) {
		// Check in the list of reachable steps if the next state URL starts with one of the reachable steps
		const isPathAllowed = routeData.reachablePaths.find((path) =>
			nextState.url.replace(/^\//, '').startsWith(`${path}`),
		);

		if (isPathAllowed) {
			return true;
		}
	}

	switch (routeData?.navigateAwayAction) {
		case NavigateAwayAction.ShowCancelVoteDialog:
			cancellationService.cancelVote();
			return false;
		case NavigateAwayAction.ShowLeaveProcessDialog:
			cancellationService.leaveProcess();
			return false;
		case NavigateAwayAction.ShowQuitProcessDialog:
			cancellationService.quitProcess();
			return false;
		case NavigateAwayAction.GoToStartVotingPage:
			return router.createUrlTree([VotingRoutePath.StartVoting]);
		case NavigateAwayAction.GoToLegalTermsPage:
			return router.createUrlTree([VotingRoutePath.LegalTerms]);
		default:
			history.pushState(null, '', location.href);
			return false;
	}
};
