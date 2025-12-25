/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {SdmRoute} from '@sdm/shared-util-types';
import {ManageSettingsComponent, SelectSettingsComponent,} from '@sdm/launcher';

export const appRoutes: SdmRoute[] = [
	{
		path: 'select-settings',
		component: SelectSettingsComponent,
		title: 'selectSettings.title',
	},
	{
		path: 'manage-settings',
		component: ManageSettingsComponent,
		title: 'manageSettings.title',
	},
	{path: '', redirectTo: 'select-settings', pathMatch: 'full'},
];
