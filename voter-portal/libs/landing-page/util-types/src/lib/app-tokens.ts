/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {InjectionToken} from '@angular/core';
import {Environment} from './environment';

export const APP_ENVIRONMENT = new InjectionToken<Environment>('Application environment');
