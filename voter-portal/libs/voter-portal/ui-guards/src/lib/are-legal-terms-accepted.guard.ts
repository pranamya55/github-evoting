/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { getHasAcceptedLegalTerms } from '@vp/voter-portal-ui-state';

export const areLegalTermsAccepted: CanActivateFn = () => {
	const store = inject(Store);
	const router = inject(Router);

	const hasAcceptedLegalTerms = store.selectSignal(getHasAcceptedLegalTerms)();

	// if the config id is set, then the legal terms have been accepted => allow navigation
	if (hasAcceptedLegalTerms) return true;

	// if the config id is not set, then the legal terms have not been accepted => redirect to base URL
	return router.createUrlTree(['']);
};
