/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

// The file contents for the current environment will overwrite these during build.
// The build system defaults to the dev environment which uses `environment.ts`, but if you do
// `ng build --env=prod` then `environment.prod.ts` will be used instead.
// The list of which env maps to which file can be found in `.angular-cli.json`.

import localeDeCH from '@angular/common/locales/de-CH';
import localeEnGB from '@angular/common/locales/en-GB';
import localeFrCH from '@angular/common/locales/fr-CH';
import localeItCH from '@angular/common/locales/it-CH';
import {Environment} from '@sdm/shared-util-types';

export const environment: Environment = {
	production: false,
	workflowEnabled: false,
	remoteServerAvailable: false,
	defaultLang: 'de',
	backendPath: '',
	locales: [
		{
			id: 'de',
			name: 'Deutsch',
			data: localeDeCH,
		},
		{
			id: 'fr',
			name: 'Français',
			data: localeFrCH,
		},
		{
			id: 'it',
			name: 'Italiano',
			data: localeItCH,
		},
		{
			id: 'en',
			name: 'English',
			data: localeEnGB,
		},
	],
};
