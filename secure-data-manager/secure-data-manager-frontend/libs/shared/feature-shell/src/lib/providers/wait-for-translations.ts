/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {TranslateService} from '@ngx-translate/core';
import {firstValueFrom} from 'rxjs';

export const waitForTranslations = (translate: TranslateService) => {
  return () => firstValueFrom(translate.get('error.generic'));
};
