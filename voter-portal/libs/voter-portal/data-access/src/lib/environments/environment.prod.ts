/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Environment } from '@vp/voter-portal-util-types';

export const environment: Environment = {
	production: true,
	defaultLang: 'DE',
	availableLanguages: [
		{ id: 'DE', label: 'Deutsch' },
		{ id: 'FR', label: 'Français' },
		{ id: 'IT', label: 'Italiano' },
		{ id: 'RM', label: 'Rumantsch' },
	],
	progressOverlayCloseDelay: 1000,
	progressOverlayNavigateDelay: 1100,
};
