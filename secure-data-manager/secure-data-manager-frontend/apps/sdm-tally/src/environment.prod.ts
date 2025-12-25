/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import localeDeCH from '@angular/common/locales/de-CH';
import localeEnGB from '@angular/common/locales/en-GB';
import localeFrCH from '@angular/common/locales/fr-CH';
import localeItCH from '@angular/common/locales/it-CH';
import {Environment} from '@sdm/shared-util-types';

export const environment: Environment = {
  production: true,
  workflowEnabled: true,
  backendPath: 'http://localhost:8091',
  remoteServerAvailable: false,
  defaultLang: 'de',
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
