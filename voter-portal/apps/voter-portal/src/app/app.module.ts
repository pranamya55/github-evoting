/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {registerLocaleData} from '@angular/common';
import {HttpClient, HttpClientModule} from '@angular/common/http';
import localeDeCH from '@angular/common/locales/de-CH';
import localeEnGB from '@angular/common/locales/en-GB';
import localeFrCH from '@angular/common/locales/fr-CH';
import localeItCH from '@angular/common/locales/it-CH';
import localeRm from '@angular/common/locales/rm';
import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {NgbCollapseModule, NgbDateAdapter, NgbDateParserFormatter, NgbDatepickerI18n,} from '@ng-bootstrap/ng-bootstrap';
import {EffectsModule} from '@ngrx/effects';
import {Store, StoreModule} from '@ngrx/store';
import {StoreDevtoolsModule} from '@ngrx/store-devtools';
import {TranslateCompiler, TranslateLoader, TranslateModule,} from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';
import {environment} from '@vp/voter-portal-data-access';
import {ChooseModule} from '@vp/voter-portal-step-choose';
import {ConfirmModule} from '@vp/voter-portal-step-confirm';
import {LegalTermsModule} from '@vp/voter-portal-step-legal-terms';
import {ReviewModule} from '@vp/voter-portal-step-review';
import {StartVotingModule} from '@vp/voter-portal-step-start-voting';
import {VerifyModule} from '@vp/voter-portal-step-verify';
import {UiComponentsModule} from '@vp/voter-portal-ui-components';
import {SwpDateAdapter, SwpDateParserFormatter, SwpDatepickerI18n, SwpTranslateCompiler,} from '@vp/voter-portal-ui-services';
import {metaReducers} from '@vp/voter-portal-ui-state';
import {AppRoutingModule} from './app-routing.module';

import {AppComponent} from './app.component';
import {HeaderComponent} from './header/header.component';
import {PageNotFoundComponent} from './page-not-found/page-not-found.component';
import {FinalizationPageComponent} from "./finalization-page/finalization-page.component";
import {StepperComponent} from './stepper/stepper.component';
import {CompatibilityCheckComponent} from "./compatibility-check/compatibility-check.component";
import {InitializationPageComponent} from "./intialization-page/initialization-page.component";
import {CastModule} from "@vp/voter-portal-step-cast";
import {TranslateTextPipe} from "e-voting-libraries-ui-kit";
import {IconComponent} from "@vp/shared-ui-components";

registerLocaleData(localeDeCH, 'DE');
registerLocaleData(localeItCH, 'IT');
registerLocaleData(localeFrCH, 'FR');
registerLocaleData(localeRm, 'RM');
registerLocaleData(localeEnGB, 'EN');

export function HttpLoaderFactory(http: HttpClient) {
	return new TranslateHttpLoader(http, './assets/i18n/', '.json');
}

@NgModule({
	declarations: [
		AppComponent,
		HeaderComponent,
		StepperComponent,
		CompatibilityCheckComponent,
		PageNotFoundComponent,
		InitializationPageComponent,
		FinalizationPageComponent,
	],
	imports: [
		BrowserModule,
		StoreModule.forRoot(
			{},
			{
				metaReducers,
				runtimeChecks: {
					strictActionImmutability: true,
					strictStateImmutability: true,
				},
			}
		),
		EffectsModule.forRoot([]),
		!environment.production ? StoreDevtoolsModule.instrument() : [],
		HttpClientModule,
		TranslateModule.forRoot({
			defaultLanguage:
				environment.availableLanguages
					.map((l) => l.id)
					.find((l) => navigator.language.includes(l.toLowerCase())) ?? environment.defaultLang,
			loader: {
				provide: TranslateLoader,
				useFactory: HttpLoaderFactory,
				deps: [HttpClient],
			},
			compiler: {
				provide: TranslateCompiler,
				useClass: SwpTranslateCompiler,
				deps: [Store],
			},
		}),
		LegalTermsModule,
		StartVotingModule,
		ChooseModule,
		ReviewModule,
		VerifyModule,
		CastModule,
		ConfirmModule,
		NgbCollapseModule,
		UiComponentsModule,
		AppRoutingModule,
		TranslateTextPipe,
		IconComponent,
		// keep after all page modules for correct rooting
	],
	providers: [
		{provide: NgbDateAdapter, useClass: SwpDateAdapter},
		{provide: NgbDateParserFormatter, useClass: SwpDateParserFormatter},
		{provide: NgbDatepickerI18n, useClass: SwpDatepickerI18n},
	],
	bootstrap: [AppComponent],
})
export class AppModule {
}
