/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, inject} from '@angular/core';
import {RouterModule} from '@angular/router';
import {ExternalHeaderComponent, ExternalHeaderConfiguration, Locale} from "e-voting-libraries-ui-kit";
import {TranslateService} from "@ngx-translate/core";
import {TenantConfigurationService} from "@vp/landing-page-data-access";
import {FaviconService} from "@vp/shared-ui-services";
import {FooterElementComponent} from "@vp/landing-page-ui-components";
import {FooterElement, SUPPORTED_LOCALES} from "@vp/landing-page-utils-types";

@Component({
	imports: [RouterModule, ExternalHeaderComponent, FooterElementComponent],
	selector: 'lp-root',
	templateUrl: './app.component.html',
	standalone: true
})
export class AppComponent {
	readonly translate: TranslateService = inject(TranslateService);
	private readonly configurationService: TenantConfigurationService = inject(TenantConfigurationService);
	private readonly faviconService = inject(FaviconService);

	headerConfiguration: ExternalHeaderConfiguration | undefined;
	footerConfiguration!: FooterElement;
	availableLocales!: Locale[];

	constructor() {
		// Load page element configurations
		this.configurationService.getHeaderConfiguration().subscribe(configuration => this.headerConfiguration = configuration);
		this.configurationService.getFooterConfiguration().subscribe(configuration => this.footerConfiguration = configuration);
		this.configurationService.getFavicon().subscribe(favicon => this.faviconService.setFavicon(favicon));

		// Register locale data for the supported languages
		this.configurationService.getLanguageConfiguration().subscribe(languageConfiguration => {
			this.availableLocales = (languageConfiguration?.availableLanguages && languageConfiguration?.availableLanguages.length > 0)
				? languageConfiguration?.availableLanguages
					.map(lang => SUPPORTED_LOCALES.find(locale => locale.id === lang))
					.filter((locale): locale is Locale => !!locale)
				: SUPPORTED_LOCALES;
			const availableLanguages = this.availableLocales.map(locale => locale.id);
			const defaultLanguage = languageConfiguration?.defaultLanguage ?? availableLanguages[0];
			this.translate.addLangs(availableLanguages);
			this.translate.setDefaultLang(defaultLanguage);
			this.translate.use(defaultLanguage);
		});
	}
}
