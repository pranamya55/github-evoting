/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {CommonModule} from '@angular/common';
import {Component, Inject, OnInit} from '@angular/core';
import {NgbDropdownModule, NgbPopover} from '@ng-bootstrap/ng-bootstrap';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {environment} from '@sdm/shared-ui-config';
import {APP_NAME, Locale, VotingServerHealth,} from '@sdm/shared-util-types';
import {SummaryService, VotingServerHealthService,} from '@sdm/shared-ui-services';
import packageInfo from 'package.json';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Summary} from "e-voting-libraries-ui-kit";

const LOCALE_ID = 'sdm.language';

@Component({
	selector: 'sdm-header',
	standalone: true,
	imports: [CommonModule, NgbDropdownModule, TranslateModule, NgbPopover],
	templateUrl: './header.component.html',
})
export class HeaderComponent implements OnInit {
	readonly appLocales = environment.locales;
	readonly remoteServerAvailable = environment.remoteServerAvailable;
	readonly description = packageInfo.description;
	readonly version = packageInfo.version;
	currentLocale!: Locale;
	votingServerConnected: boolean | null = null;
	votingServerName: string = '';
	configurationSummary: Summary | null = null;

	constructor(
		@Inject(APP_NAME) public readonly applicationName: string,
		private readonly summaryService: SummaryService,
		private readonly translateService: TranslateService,
		private readonly votingServerHealthService: VotingServerHealthService,
	) {
		this.votingServerHealthService
			.get()
			.pipe(takeUntilDestroyed())
			.subscribe((votingServerHealth: VotingServerHealth) => {
				this.votingServerConnected = votingServerHealth.status;
				this.votingServerName = votingServerHealth.serverName;
			});

		if (!environment.workflowEnabled) return;

		this.summaryService
			.getHeaderSummary()
			.subscribe((configurationSummary: Summary) => {
				this.configurationSummary = configurationSummary;
			});
	}

	getTranslatedDescription(configurationSummary: Summary): string {
		const descriptionInCurrentLang = configurationSummary.contestDescription[this.translateService.currentLang];
		if (descriptionInCurrentLang) return descriptionInCurrentLang;

		return configurationSummary.contestDescription[this.translateService.defaultLang];
	}


	private get initialLocale(): Locale {
		const storedLocaleId = localStorage.getItem(LOCALE_ID);
		const storedLocale =
			!!storedLocaleId &&
			this.appLocales.find((locale) => locale.id === storedLocaleId);

		if (storedLocale) return storedLocale;

		const navigatorLocale = this.appLocales.find((locale) =>
			navigator.language.startsWith(locale.id),
		);

		if (navigatorLocale) return navigatorLocale;

		const defaultLocale = this.appLocales.find(
			(locale) => locale.id === this.translateService.defaultLang,
		);

		return defaultLocale as Locale;
	}

	ngOnInit() {
		this.setLocale(this.initialLocale);
	}

	setLocale(locale: Locale) {
		this.currentLocale = locale;
		this.translateService.use(locale.id);
		document.documentElement.lang = locale.id;
		localStorage.setItem(LOCALE_ID, locale.id);
	}
}
