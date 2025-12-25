/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import {APP_ENVIRONMENT} from "@vp/landing-page-utils-types";
import {importProvidersFrom} from "@angular/core";
import {NgHcaptchaModule} from "ng-hcaptcha";
import {environment} from "@vp/landing-page-data-access";

fetch(environment.environmentJsonPath)
	.then((resp) => {
		if (!resp.ok) {
			throw new Error(`Failed to load environment json file: ${resp.statusText}`);
		}
		return resp.json();
	})
	.then((environment) => {
		return bootstrapApplication(AppComponent, {
			providers: [
				...appConfig.providers,
				{ provide: APP_ENVIRONMENT, useValue: environment },
				importProvidersFrom(
					NgHcaptchaModule.forRoot({
						siteKey: environment.captcha.siteKey,
					})
				),
			],
		});
	})
	.catch((err) => console.error(err));