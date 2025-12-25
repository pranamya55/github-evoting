/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {registerLocaleData} from '@angular/common';
import {
	HTTP_INTERCEPTORS,
	provideHttpClient,
	withInterceptorsFromDi,
} from '@angular/common/http';
import {
	APP_INITIALIZER,
	ApplicationConfig,
	ErrorHandler,
	importProvidersFrom,
	LOCALE_ID,
} from '@angular/core';
import {bootstrapApplication, Title} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {
	provideRouter,
	TitleStrategy,
	withEnabledBlockingInitialNavigation, withHashLocation,
} from '@angular/router';
import {
	NgbModalConfig,
	NgbProgressbarConfig,
} from '@ng-bootstrap/ng-bootstrap';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {environment} from '@sdm/shared-ui-config';
import {workflowGuard} from '@sdm/shared-ui-services';
import {APP_NAME, APP_ROUTES, SdmRoute} from '@sdm/shared-util-types';
import {provideEnvironmentNgxMask} from 'ngx-mask';
import {AppComponent} from './components/app/app.component';
import {configureNgbComponents} from './providers/configure-ngb-components';
import {GlobalErrorHandler} from './providers/global-error-handler';
import {HttpErrorInterceptor} from './providers/http-error-interceptor';
import {translateModuleConfig} from './providers/translate-module-config';
import {TranslatedTitleStrategy} from './providers/translated-title-strategy';
import {waitForTranslations} from './providers/wait-for-translations';
import {GoodbyeComponent} from './components/goodbye/goodbye.component';

export const bootstrapSdmApplication = (appRoutes: SdmRoute[]) => {
	const workflowRoutes: SdmRoute[] = [
		{
			path: '',
			canActivateChild: [workflowGuard],
			children: [
				...appRoutes,
				{ path: '', pathMatch: 'full', component: GoodbyeComponent, title: 'goodbye.completed' },
			],
		},
	];

	const appConfig: ApplicationConfig = {
		providers: [
			provideHttpClient(withInterceptorsFromDi()),
			provideRouter(
				environment.workflowEnabled ? workflowRoutes : appRoutes,
				withEnabledBlockingInitialNavigation(),
				withHashLocation()
			),
			importProvidersFrom(BrowserAnimationsModule),
			importProvidersFrom(TranslateModule.forRoot(translateModuleConfig)),

			provideEnvironmentNgxMask(),

			{
				provide: LOCALE_ID,
				useFactory: (translateService: TranslateService) =>
					`${translateService.defaultLang}-ch`,
				deps: [TranslateService],
			},
			{provide: ErrorHandler, useClass: GlobalErrorHandler},
			{
				provide: HTTP_INTERCEPTORS,
				useClass: HttpErrorInterceptor,
				multi: true,
			},
			{
				provide: TitleStrategy,
				useClass: TranslatedTitleStrategy,
			},
			{
				provide: APP_INITIALIZER,
				useFactory: waitForTranslations,
				deps: [TranslateService],
				multi: true,
			},
			{
				provide: APP_INITIALIZER,
				useFactory: configureNgbComponents,
				deps: [NgbProgressbarConfig, NgbModalConfig],
				multi: true,
			},
			{
				provide: APP_NAME,
				useFactory: (title: Title) => title.getTitle(),
				deps: [Title],
			},
			{
				provide: APP_ROUTES,
				useValue: appRoutes,
			},
		],
	};

	environment.locales.forEach((locale) => {
		registerLocaleData(locale.data, locale.id);
	});

	bootstrapApplication(AppComponent, appConfig).catch((err) =>
		console.error(err),
	);
};
