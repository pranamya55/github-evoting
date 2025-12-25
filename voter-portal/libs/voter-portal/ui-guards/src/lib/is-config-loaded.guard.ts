/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { getConfig } from '@vp/voter-portal-ui-state';

export const isConfigLoaded: CanActivateFn = () => {
	const store = inject(Store);
	const router = inject(Router);

	const config = store.selectSignal(getConfig)();

	// if the config is set, then the config has been loaded => allow navigation
	if (config?.electionEventId) return true;

	// if the config is not set, then the config has not been loaded => redirect to base URL
	return router.createUrlTree(['']);
};
