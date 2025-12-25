/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import localeDeCH from '@angular/common/locales/de-CH';
import localeFrCH from '@angular/common/locales/fr-CH';
import localeItCH from '@angular/common/locales/it-CH';
import localeRm from '@angular/common/locales/rm';
import localeEn from '@angular/common/locales/en-CH';
import {Locale} from "e-voting-libraries-ui-kit";

// Supported locales in the landing page
export const SUPPORTED_LOCALES: Locale[] = [
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
		id: 'rm',
		name: 'Rumantsch',
		data: localeRm,
	},
	{
		id: 'en',
		name: 'English',
		data: localeEn,
	}
];

type SupportedLanguage = typeof SUPPORTED_LOCALES[number]['id'];

export interface LanguageConfiguration {
	availableLanguages: SupportedLanguage[];
	defaultLanguage: SupportedLanguage;
}