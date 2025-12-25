/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
// This file can be replaced during build by using the `fileReplacements` array.
// `ng build` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

import { Environment } from '@vp/voter-portal-util-types';

export const environment: Environment = {
	production: false,
	defaultLang: 'DE',
	availableLanguages: [
		{ id: 'DE', label: 'Deutsch' },
		{ id: 'FR', label: 'Français' },
		{ id: 'IT', label: 'Italiano' },
		{ id: 'RM', label: 'Rumantsch' },
		{ id: 'EN', label: 'English' },
	],
	progressOverlayCloseDelay: 1000,
	progressOverlayNavigateDelay: 1100, //a bit longer than progressOverlayCloseDelay
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 * import 'zone.js/plugins/zone-error';  // Included with Angular CLI.
 */
