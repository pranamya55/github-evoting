/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';

export const isElectionEventIdValid = (
	route: ActivatedRouteSnapshot,
): boolean | UrlTree => {
	const router = inject(Router);

	const electionEventId = route.paramMap.get('electionEventId');
	const isElectionEventIdValid =
		electionEventId && /^[0-9A-F]{32}$/.test(electionEventId);

	if (isElectionEventIdValid) {
		return true;
	}

	return router.createUrlTree(['page-not-found']);
};
