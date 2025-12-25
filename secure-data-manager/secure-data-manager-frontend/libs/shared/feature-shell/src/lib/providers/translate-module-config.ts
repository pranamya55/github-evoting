/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {HttpClient} from '@angular/common/http';
import {TranslateLoader} from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';
import {environment} from '@sdm/shared-ui-config';

const httpLoaderFactory = (http: HttpClient) => {
  return new TranslateHttpLoader(http, './assets/i18n/', '.json');
};

export const translateModuleConfig = {
  defaultLanguage: environment.locales[0].id,
  loader: {
    provide: TranslateLoader,
    useFactory: httpLoaderFactory,
    deps: [HttpClient],
  },
};
