/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {InjectionToken} from '@angular/core';
import {SdmRoute} from './sdm-route';

export const APP_NAME = new InjectionToken<string>('APP_NAME');
export const APP_ROUTES = new InjectionToken<SdmRoute[]>('APP_ROUTES');
