/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {inject, Injectable} from '@angular/core';
import {
	AdditionalFAQs,
	AdditionalLegalTerms,
	ContestsCapabilities,
	ExtendedFactor,
	HeaderConfig,
	RequestTimeout,
	ResponsiveConfig,
	TranslatePlaceholders,
	VoterPortalConfig,
} from '@vp/voter-portal-util-types';
import {Store} from '@ngrx/store';
import {getConfig} from '@vp/voter-portal-ui-state';
import {TranslateService} from '@ngx-translate/core';

@Injectable({
	providedIn: 'root',
})
export class ConfigurationService implements VoterPortalConfig {
	private readonly store = inject(Store);
	private readonly translate = inject(TranslateService);

	electionEventId!: string;
	identification!: ExtendedFactor;
	contestsCapabilities!: ContestsCapabilities;
	requestTimeout!: RequestTimeout;
	header?: HeaderConfig;
	translatePlaceholders?: TranslatePlaceholders;
	additionalLegalTerms?: AdditionalLegalTerms;
	additionalFAQs?: AdditionalFAQs;
	favicon?: string;

	// Additional property to handle the header height, header height + bars height
	headerHeight: ResponsiveConfig<number> = {
		desktop: 0,
		mobile: 0,
	};

	constructor() {
		this.store.select(getConfig).subscribe((config) => {
			Object.assign(this, config);

			if (config.header) {
				this.headerHeight = {...config.header.logoHeight};

				const headerBars = config.header.bars ?? [];
				headerBars.forEach(bar => {
					this.headerHeight.desktop += bar.height.desktop;
					this.headerHeight.mobile += bar.height.mobile;
				});
			}

			// if the current translation is already loaded, recompile it with the new placeholders
			const { currentLang, translations, compiler } = this.translate;
			if (currentLang in translations) {
				const compiledTranslations = compiler.compileTranslations(
					translations,
					currentLang,
				);
				this.translate.setTranslation(
					currentLang,
					compiledTranslations[currentLang],
					false,
				);
			}
		});
	}
}
